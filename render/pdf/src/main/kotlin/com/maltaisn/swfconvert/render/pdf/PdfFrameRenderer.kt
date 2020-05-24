/*
 * Copyright 2020 Nicolas Maltais
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.maltaisn.swfconvert.render.pdf

import com.maltaisn.swfconvert.core.CoreConfiguration
import com.maltaisn.swfconvert.core.font.data.GlyphData
import com.maltaisn.swfconvert.core.frame.data.*
import com.maltaisn.swfconvert.core.image.data.ImageData
import com.maltaisn.swfconvert.core.shape.data.path.Path
import com.maltaisn.swfconvert.core.shape.data.path.PathElement
import com.maltaisn.swfconvert.core.shape.data.path.PathFillStyle
import com.maltaisn.swfconvert.core.shape.data.path.PathLineStyle
import org.apache.pdfbox.cos.COSArray
import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.cos.COSFloat
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDResources
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.graphics.form.PDTransparencyGroup
import org.apache.pdfbox.pdmodel.graphics.form.PDTransparencyGroupAttributes
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.util.Matrix
import java.awt.BasicStroke
import java.awt.geom.Rectangle2D
import java.io.File
import java.util.*
import javax.inject.Inject


class PdfFrameRenderer @Inject internal constructor(
        private val config: CoreConfiguration,
        private val pdfConfig: PdfConfiguration
) {

    private lateinit var pdfDoc: PDDocument
    private lateinit var pdfImages: Map<ImageData, PDImageXObject>
    private lateinit var pdfFonts: Map<File, PDFont>

    private val streamWrapperStack = LinkedList<PdfStreamWrapper>()
    private val streamWrapper: PdfStreamWrapper
        get() = streamWrapperStack.peek()

    private val pageBoundsStack = LinkedList<PDRectangle>()
    private val currentPageBounds: PDRectangle
        get() = pageBoundsStack.peek()


    fun renderFrame(pdfDoc: PDDocument, frame: FrameGroup,
                    pdfImages: Map<ImageData, PDImageXObject>,
                    pdfFonts: Map<File, PDFont>) {
        this.pdfDoc = pdfDoc
        this.pdfImages = pdfImages
        this.pdfFonts = pdfFonts

        // Create page
        val pdfPage = PDPage()
        pdfDoc.addPage(pdfPage)

        // Set page bounds
        val bounds = PDRectangle(frame.actualWidth, frame.actualHeight)
        pdfPage.mediaBox = bounds
        pageBoundsStack.push(bounds)

        // Draw page
        withPdfStream(PdfPageContentStream(pdfDoc, pdfPage, pdfConfig.compress)) {
            drawObject(frame)
        }
    }

    private fun drawObject(obj: FrameObject) {
        when (obj) {
            is GroupObject -> drawGroup(obj)
            is ShapeObject -> drawShape(obj)
            is TextObject -> drawText(obj)
            else -> error("Unknown frame object")
        }
    }

    private fun drawGroup(group: GroupObject) {
        if (group.objects.isEmpty()) {
            // Nothing to draw
            return
        }

        when (group) {
            is GroupObject.Simple -> drawSimpleGroup(group)
            is GroupObject.Transform -> drawTransformGroup(group)
            is GroupObject.Clip -> drawClipGroup(group)
            is GroupObject.Blend -> drawBlendGroup(group)
            is GroupObject.Masked -> drawMaskedGroup(group)
        }
    }

    private fun drawSimpleGroup(group: GroupObject) {
        for (obj in group.objects) {
            drawObject(obj)
        }
    }

    private fun drawTransformGroup(group: GroupObject.Transform) {
        streamWrapper.withState {
            // Apply transform
            streamWrapper.transform(group.transform)

            // Transform page bounds (with inverse transform)
            val bounds = currentPageBounds
            val boundsRect = Rectangle2D.Float(bounds.lowerLeftX,
                    bounds.lowerLeftY, bounds.width, bounds.height)
            val newBounds = group.transform.createInverse()
                    .createTransformedShape(boundsRect).bounds2D
            pageBoundsStack.push(PDRectangle(newBounds.x.toFloat(), newBounds.y.toFloat(),
                    newBounds.width.toFloat(), newBounds.height.toFloat()))

            // Draw group objects
            drawSimpleGroup(group)

            pageBoundsStack.pop()
        }
    }

    private fun drawClipGroup(group: GroupObject.Clip) {
        if (config.disableClipping) {
            drawSimpleGroup(group)
            return
        }
        streamWrapper.withState {
            // Apply clip
            for (clip in group.clips) {
                clipPath(clip)
            }

            // Draw group objects
            drawSimpleGroup(group)
        }
    }

    private fun drawMaskedGroup(group: GroupObject.Masked) {
        if (config.disableMasking) {
            drawSimpleGroup(group)
            return
        }

        if (group.objects.size < 2) {
            // There must be at least a mask and something to mask.
            // Otherwise there's nothing to draw.
            return
        }

        // Instead of using page bounds for drawing transparency groups,
        // use the mask bounds, which should give better performance since it's smaller.
        val bounds = group.bounds
        val pdbounds = PDRectangle(bounds.x.toFloat(), bounds.y.toFloat(),
                bounds.width.toFloat(), bounds.height.toFloat())

        // Create mask transparency group
        val maskGroup = createTransparencyGroup(pdbounds) {
            drawObject(group.objects.last())
        }

        streamWrapper.withState {
            // Set mask on graphics state
            streamWrapper.setExtendedState {
                val maskDict = COSDictionary()
                maskDict.setItem(COSName.S, COSName.ALPHA)
                maskDict.setItem(COSName.G, maskGroup)
                cosObject.setItem(COSName.SMASK, maskDict)
            }

            if (group.objects.size == 2) {
                // Draw the single object to mask.
                drawObject(group.objects.first())

            } else {
                // Multiple objects to mask, create transparency group so
                // that they will be composited as a single object.
                val contentGroup = createTransparencyGroup(pdbounds) {
                    for (i in 0 until group.objects.lastIndex) {
                        drawObject(group.objects[i])
                    }
                }
                streamWrapper.stream.drawForm(contentGroup)
            }
        }
    }

    private fun drawBlendGroup(group: GroupObject.Blend) {
        if (config.disableBlending) {
            drawSimpleGroup(group)
            return
        }

        if (countGroupBlendableChildren(group) == 1) {
            // Single object, blend can be applied directly.
            streamWrapper.withState {
                streamWrapper.blendMode = group.blendMode
                drawObject(group.objects.first())
            }

        } else {
            // Multiple objects to blend, create a group so they get blended as a single object.
            val contentGroup = createTransparencyGroup {
                drawSimpleGroup(group)
            }
            streamWrapper.withState {
                streamWrapper.blendMode = group.blendMode
                streamWrapper.stream.drawForm(contentGroup)
            }
        }
    }

    private fun drawShape(shape: ShapeObject) {
        // Draw shape paths
        for (path in shape.paths) {
            drawPath(path)
        }
    }

    private fun drawPath(path: Path) {
        streamWrapper.withState {
            val fill = path.fillStyle
            val line = path.lineStyle

            // Draw fill if needed
            when (fill) {
                is PathFillStyle.Solid -> Unit
                is PathFillStyle.Image -> drawImage(path, fill)
                is PathFillStyle.Gradient -> drawGradient(path, fill)
            }

            // Stroke or fill path if needed
            if (fill is PathFillStyle.Solid || line != null) {
                drawPathToPdf(path)
                if (fill is PathFillStyle.Solid) {
                    streamWrapper.nonStrokingColor = fill.color
                    streamWrapper.stream.fillEvenOdd()
                }
                if (line != null) {
                    // Set line style
                    applyLineStyle(line)
                    streamWrapper.stream.stroke()
                }
            }
        }
    }

    private fun applyLineStyle(style: PathLineStyle) {
        streamWrapper.let {
            it.strokingColor = style.color
            it.lineWidth = style.width
            it.lineCapStyle = style.cap
            it.lineJoinStyle = style.join
            if (style.join == BasicStroke.JOIN_MITER) {
                it.miterLimit = style.miterLimit
            }
        }
    }

    private fun drawImage(path: Path, imageFill: PathFillStyle.Image) {
        streamWrapper.withState {
            clipPath(path)

            val pdfImage = pdfImages[imageFill.imageData] ?: error("Missing PDF image")
            streamWrapper.stream.drawImage(pdfImage, Matrix(imageFill.transform))
        }
    }

    private fun drawGradient(path: Path, gradient: PathFillStyle.Gradient) {
        // Create PDF shading object
        val shading = PDGradient(gradient.colors.map {
            PDGradient.GradientPart(it.color, it.ratio)
        })

        val coords = floatArrayOf(0f, 0.5f, 1f, 0.5f)
        gradient.transform.transform(coords, 0, coords, 0, 2)
        val arr = COSArray()
        for (coord in coords) {
            arr.add(COSFloat(coord))
        }
        shading.coords = arr

        streamWrapper.withState {
            clipPath(path)
            streamWrapper.stream.shadingFill(shading)
        }
    }

    private fun drawText(text: TextObject) {
        streamWrapper.nonStrokingColor = text.color

        val stream = streamWrapper.stream
        stream.beginText()

        val font = pdfFonts[text.font.fontFile!!] ?: error("Missing PDF font")
        stream.setFont(font, text.fontSize)

        if (text.x != 0f || text.y != 0f) {
            stream.newLineAtOffset(text.x, text.y)
        }

        // Draw text
        val str = text.text
        if (text.glyphOffsets.isEmpty()) {
            stream.showText(str)
        } else {
            // Create array of chars and glyph offsets.
            val textArr = arrayOfNulls<Any>(str.length * 2 - 1)
            for ((i, c) in str.withIndex()) {
                textArr[i * 2] = c.toString()
                if (i != str.length - 1) {
                    val offset = -text.glyphOffsets[i] / GlyphData.EM_SQUARE_SIZE * 1000
                    textArr[i * 2 + 1] = offset
                }
            }
            stream.showTextWithPositioning(textArr.requireNoNulls())
        }

        stream.endText()
    }

    private fun clipPath(path: Path) {
        if (config.disableClipping) {
            return
        }

        // Draw clip bounds
        if (config.drawClipBounds) {
            streamWrapper.withState {
                applyLineStyle(config.debugLineStyle)
                drawPathToPdf(path)
                streamWrapper.stream.stroke()
            }
        }

        // Clip path
        drawPathToPdf(path)
        streamWrapper.stream.clipEvenOdd()
    }

    private fun drawPathToPdf(path: Path) {
        var currX = 0f
        var currY = 0f
        val stream = streamWrapper.stream
        for (e in path.elements) {
            when (e) {
                is PathElement.MoveTo -> stream.moveTo(e.x, e.y)
                is PathElement.LineTo -> stream.lineTo(e.x, e.y)
                is PathElement.CubicTo -> stream.curveTo(e.c1x, e.c1y, e.c2x, e.c2y, e.x, e.y)
                is PathElement.QuadTo -> e.toCubic(currX, currY).let {
                    stream.curveTo(it.c1x, it.c1y, it.c2x, it.c2y, it.x, it.y)
                }
                is PathElement.ClosePath -> stream.closePath()
                is PathElement.Rectangle -> stream.addRect(e.x, e.y, e.width, e.height)
            }
            currX = e.x
            currY = e.y
        }
    }

    private inline fun withPdfStream(pdfStream: PdfContentStream, block: () -> Unit) {
        val wrapper = PdfStreamWrapper(pdfStream)
        streamWrapperStack.push(wrapper)
        block()
        streamWrapperStack.pop()
        wrapper.stream.close()
    }

    private inline fun createTransparencyGroup(bounds: PDRectangle = currentPageBounds,
                                               block: () -> Unit): PDTransparencyGroup {
        val group = PDTransparencyGroup(pdfDoc)
        group.bBox = bounds
        group.resources = PDResources()
        group.cosObject.setItem(COSName.GROUP, PDTransparencyGroupAttributes())
        withPdfStream(PdfFormContentStream(group), block)
        return group
    }

    /**
     * Count blendable children in group. Group can have a single immediate child, for example
     * a transform or a clip, but this group can contain many objects that must be blended
     * as a single object. Masked and other blend groups count as 1 since they use a
     * transparency group already.
     */
    private fun countGroupBlendableChildren(group: GroupObject): Int = group.objects.sumBy {
        when (it) {
            is GroupObject.Masked, is GroupObject.Blend -> 1
            is GroupObject -> countGroupBlendableChildren(it)
            else -> 1
        }
    }

}
