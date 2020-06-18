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

package com.maltaisn.swfconvert.render.svg

import com.maltaisn.swfconvert.core.BlendMode
import com.maltaisn.swfconvert.core.FrameGroup
import com.maltaisn.swfconvert.core.FrameObject
import com.maltaisn.swfconvert.core.GroupObject
import com.maltaisn.swfconvert.core.image.Color
import com.maltaisn.swfconvert.core.shape.Path
import com.maltaisn.swfconvert.core.shape.PathElement.*
import com.maltaisn.swfconvert.core.shape.PathFillStyle
import com.maltaisn.swfconvert.core.shape.PathLineStyle
import com.maltaisn.swfconvert.core.shape.ShapeObject
import com.maltaisn.swfconvert.core.text.GlyphData
import com.maltaisn.swfconvert.core.text.TextObject
import com.maltaisn.swfconvert.render.svg.writer.SvgPathWriter
import com.maltaisn.swfconvert.render.svg.writer.SvgStreamWriter
import com.maltaisn.swfconvert.render.svg.writer.data.*
import java.awt.BasicStroke
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject


internal class SvgFrameRenderer @Inject constructor(
        private val config: SvgConfiguration
) {

    private lateinit var svg: SvgStreamWriter

    private var currentDefId = 0
    private val nextDefId: String
        get() {
            val id = currentDefId.toDefId()
            currentDefId++
            return id
        }

    private val definedFonts = mutableMapOf<File, String>()

    private lateinit var imagesDir: File
    private lateinit var fontsDir: File

    fun renderFrame(index: Int, frame: FrameGroup,
                    imagesDir: File, fontsDir: File) {
        val outputFile = config.output[index]
        val outputDir = outputFile.parentFile
        this.imagesDir = imagesDir.relativeToOrSelf(outputDir)
        this.fontsDir = fontsDir.relativeToOrSelf(outputDir)

        FileOutputStream(outputFile).use { outputStream ->
            svg = SvgStreamWriter(outputStream, config.precision, config.transformPrecision,
                    config.percentPrecision, config.prettyPrint)

            svg.start(SvgNumber(frame.actualWidth, SvgUnit.PT),
                    SvgNumber(frame.actualHeight, SvgUnit.PT),
                    Rectangle2D.Float(0f, 0f, frame.width, frame.height),
                    config.writeProlog,
                    SvgGraphicsState(
                            fillRule = SvgFillRule.EVEN_ODD,
                            clipPathRule = SvgFillRule.EVEN_ODD,
                            preserveAspectRatio = SvgPreserveAspectRatio.NONE))
            // FrameGroup transform is ignored because the transform is already created by the
            // viewBox having a different size than the one set by 'width' and 'height'.
            drawSimpleGroup(frame)
            svg.end()
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
        val transforms = group.transform.toSvgTransformList()
        if (transforms != null) {
            svg.group(SvgGraphicsState(transforms = transforms)) {
                drawSimpleGroup(group)
            }
        } else {
            // Identity transform.
            drawSimpleGroup(group)
        }
    }

    private fun drawClipGroup(group: GroupObject.Clip) {
        if (group.clips.isEmpty()) {
            drawSimpleGroup(group)
            return
        }

        svg.group(createClipSvgGraphicsState(group.clips)) {
            drawSimpleGroup(group)
        }
    }

    private fun drawBlendGroup(group: GroupObject.Blend) {
        svg.group(SvgGraphicsState(mixBlendMode = group.blendMode.toSvgMixBlendMode())) {
            drawSimpleGroup(group)
        }
    }

    private fun drawMaskedGroup(group: GroupObject.Masked) {
        if (group.objects.size < 2) {
            // There must be at least a mask and something to mask.
            // Otherwise there's nothing to draw.
            return
        }

        val id = nextDefId
        svg.writeDef(id) {
            mask {
                drawObject(group.objects.last())
            }
        }
        svg.group(SvgGraphicsState(maskId = id)) {
            for (i in 0 until group.objects.lastIndex) {
                drawObject(group.objects[i])
            }
        }
    }

    private fun drawShape(shape: ShapeObject) {
        for (path in shape.paths) {
            drawPath(path)
        }
    }


    private fun drawText(text: TextObject) {
        // Create font definition if not already created.
        val fontFile = File(getFontsFile(text.font.fontFile!!.name))
        val fontId = definedFonts.getOrPut(fontFile) {
            val id = nextDefId
            svg.writeDef {
                svg.font(id, fontFile.invariantSeparatorsPath)
            }
            id
        }

        val dx = FloatArray(text.glyphOffsets.size) {
            text.glyphOffsets[it] / GlyphData.EM_SQUARE_SIZE * text.fontSize
        }
        svg.text(SvgNumber(text.x), SvgNumber(text.y), dx, fontId, text.fontSize, text.text,
                SvgGraphicsState(fill = SvgFillColor(text.color), fillOpacity = text.color.floatAlpha))
    }

    private fun drawPath(path: Path) {
        val fill = path.fillStyle
        val line = path.lineStyle

        // Draw fill if needed
        when (fill) {
            is PathFillStyle.Solid -> Unit
            is PathFillStyle.Image -> drawImage(path, fill)
            is PathFillStyle.Gradient -> drawGradient(path, fill)
        }

        // Stroke or fill path if needed
        var grState = line?.toSvgGraphicsState()
        if (fill is PathFillStyle.Solid) {
            grState = (grState ?: SvgStreamWriter.NULL_GRAPHICS_STATE)
                    .copy(fill = SvgFillColor(fill.color), fillOpacity = fill.color.floatAlpha)
        }
        if (grState != null) {
            svg.path(grState) {
                writePath(path, this)
            }
        }
    }

    private fun drawImage(path: Path, imageFill: PathFillStyle.Image) {
        if (imageFill.clip) {
            svg.startGroup(createClipSvgGraphicsState(listOf(path)))
        }

        val imageData = imageFill.imageData
        val imageHref = getImagesFile(imageData.dataFile!!.name)
        val maskId = if (imageData.alphaDataFile != null) {
            val id = nextDefId
            val alphaImageHref = getImagesFile(imageData.alphaDataFile!!.name)
            svg.writeDef(id) {
                mask {
                    svg.image(alphaImageHref)
                }
            }
            id
        } else {
            null
        }

        val transform = AffineTransform(imageFill.transform)
        transform.scale(1.0 / imageData.width, 1.0 / imageData.height)

        svg.image(imageHref, grState = SvgGraphicsState(
                transforms = transform.toSvgTransformList(),
                maskId = maskId))

        if (imageFill.clip) {
            svg.endGroup()
        }
    }

    private fun drawGradient(path: Path, gradient: PathFillStyle.Gradient) {
        // Create gradient stops
        val stops = gradient.colors.map {
            SvgGradientStop(it.ratio, it.color, it.color.floatAlpha)
        }

        val id = nextDefId
        svg.writeDef(id) {
            linearGradient(stops, SvgGradientUnits.USER_SPACE_ON_USE,
                    gradient.transform.toSvgTransformList(), x1 = -16384f, x2 = 16384f)
        }
        svg.path(SvgGraphicsState(fill = SvgFillId(id))) {
            writePath(path, this)
        }
    }

    private fun createClipSvgGraphicsState(paths: List<Path>): SvgGraphicsState {
        val id = nextDefId
        svg.writeDef(id) {
            clipPathData {
                for (path in paths) {
                    writePath(path, this)
                }
            }
        }
        return SvgGraphicsState(clipPathId = id)
    }

    private fun writePath(path: Path, pathWriter: SvgPathWriter) = pathWriter.apply {
        for (e in path.elements) {
            when (e) {
                is MoveTo -> moveTo(e.x, e.y)
                is LineTo -> lineTo(e.x, e.y)
                is QuadTo -> quadTo(e.cx, e.cy, e.x, e.y)
                is CubicTo -> cubicTo(e.c1x, e.c1y, e.c2x, e.c2y, e.x, e.y)
                is ClosePath -> closePath()
                is Rectangle -> {
                    moveTo(e.x, e.y)
                    lineTo(e.x + e.width, e.y)
                    lineTo(e.x + e.width, e.y + e.height)
                    lineTo(e.x, e.y + e.height)
                    closePath()
                }
            }
        }
    }

    private fun Int.toDefId(): String {
        var v = this
        return buildString(4) {
            // Use base 52 for first char to use only letters.
            append(BASE_64_ALPHABET[v % 52])
            v /= 52
            while (v > 0) {
                // Use base 64 for following chars.
                append(BASE_64_ALPHABET[v % 64])
                v /= 64
            }
        }
    }

    private val Color.floatAlpha: Float
        get() = this.a / 255f

    private fun AffineTransform.toSvgTransformList() = when {
        this.isIdentity -> null
        scaleX == 1.0 && scaleY == 1.0 && shearX == 0.0 && shearY == 0.0 -> {
            // Translate only
            listOf(SvgTransform.Translate(translateX.toFloat(), translateY.toFloat()))
        }
        shearX == 0.0 && shearY == 0.0 && translateX == 0.0 && translateY == 0.0 -> {
            // Scale only
            listOf(SvgTransform.Scale(scaleX.toFloat(), scaleY.toFloat()))
        }
        else -> listOf(SvgTransform.Matrix(scaleX.toFloat(), shearY.toFloat(), shearX.toFloat(),
                scaleY.toFloat(), translateX.toFloat(), translateY.toFloat()))
    }

    private fun BlendMode.toSvgMixBlendMode() = when (this) {
        BlendMode.NORMAL -> SvgMixBlendMode.NORMAL
        BlendMode.MULTIPLY -> SvgMixBlendMode.MULTIPLY
        BlendMode.LIGHTEN -> SvgMixBlendMode.LIGHTEN
        BlendMode.DARKEN -> SvgMixBlendMode.DARKEN
        BlendMode.HARD_LIGHT -> SvgMixBlendMode.HARD_LIGHT
        BlendMode.SCREEN -> SvgMixBlendMode.SCREEN
        BlendMode.OVERLAY -> SvgMixBlendMode.OVERLAY
    }

    private fun PathLineStyle.toSvgGraphicsState() = SvgGraphicsState(
            stroke = SvgFillColor(this.color),
            strokeOpacity = this.color.floatAlpha,
            strokeWidth = this.width,
            strokeLineCap = when (this.cap) {
                BasicStroke.CAP_BUTT -> SvgStrokeLineCap.BUTT
                BasicStroke.CAP_ROUND -> SvgStrokeLineCap.ROUND
                BasicStroke.CAP_SQUARE -> SvgStrokeLineCap.SQUARE
                else -> error("Unknown stroke line cap")
            },
            strokeLineJoin = when (this.join) {
                BasicStroke.JOIN_BEVEL -> SvgStrokeLineJoin.Bevel
                BasicStroke.JOIN_MITER -> if (this.miterLimit == 0f) {
                    SvgStrokeLineJoin.Miter
                } else {
                    SvgStrokeLineJoin.MiterClip(this.miterLimit)
                }
                BasicStroke.JOIN_ROUND -> SvgStrokeLineJoin.Bevel
                else -> error("Unknown stroke line join")
            })

    private fun getImagesFile(name: String) = File(imagesDir, name).invariantSeparatorsPath
    private fun getFontsFile(name: String) = File(fontsDir, name).invariantSeparatorsPath


    companion object {
        private const val BASE_64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-"
    }

}
