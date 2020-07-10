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
import com.maltaisn.swfconvert.core.image.ImageData
import com.maltaisn.swfconvert.core.image.ImageFormat
import com.maltaisn.swfconvert.core.shape.Path
import com.maltaisn.swfconvert.core.shape.PathElement.ClosePath
import com.maltaisn.swfconvert.core.shape.PathElement.CubicTo
import com.maltaisn.swfconvert.core.shape.PathElement.LineTo
import com.maltaisn.swfconvert.core.shape.PathElement.MoveTo
import com.maltaisn.swfconvert.core.shape.PathElement.QuadTo
import com.maltaisn.swfconvert.core.shape.PathElement.Rectangle
import com.maltaisn.swfconvert.core.shape.PathFillStyle
import com.maltaisn.swfconvert.core.shape.PathLineStyle
import com.maltaisn.swfconvert.core.shape.ShapeObject
import com.maltaisn.swfconvert.core.text.Font
import com.maltaisn.swfconvert.core.text.FontGlyph
import com.maltaisn.swfconvert.core.text.GlyphData
import com.maltaisn.swfconvert.core.text.TextObject
import com.maltaisn.swfconvert.render.svg.writer.SvgPathWriter
import com.maltaisn.swfconvert.render.svg.writer.SvgStreamWriter
import com.maltaisn.swfconvert.render.svg.writer.data.SvgFillColor
import com.maltaisn.swfconvert.render.svg.writer.data.SvgFillId
import com.maltaisn.swfconvert.render.svg.writer.data.SvgFillNone
import com.maltaisn.swfconvert.render.svg.writer.data.SvgFillRule
import com.maltaisn.swfconvert.render.svg.writer.data.SvgGradientStop
import com.maltaisn.swfconvert.render.svg.writer.data.SvgGradientUnits
import com.maltaisn.swfconvert.render.svg.writer.data.SvgGraphicsState
import com.maltaisn.swfconvert.render.svg.writer.data.SvgMixBlendMode
import com.maltaisn.swfconvert.render.svg.writer.data.SvgNumber
import com.maltaisn.swfconvert.render.svg.writer.data.SvgPreserveAspectRatio
import com.maltaisn.swfconvert.render.svg.writer.data.SvgStrokeLineCap
import com.maltaisn.swfconvert.render.svg.writer.data.SvgStrokeLineJoin
import com.maltaisn.swfconvert.render.svg.writer.data.SvgTransform
import com.maltaisn.swfconvert.render.svg.writer.data.SvgUnit
import org.apache.logging.log4j.kotlin.logger
import java.awt.BasicStroke
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Base64
import java.util.zip.GZIPOutputStream
import javax.inject.Inject

internal class SvgFrameRenderer @Inject constructor(
    private val config: SvgConfiguration
) {

    private val logger = logger()

    private lateinit var svg: SvgStreamWriter

    private var currentDefId = 0
    private val nextDefId: String
        get() {
            val id = currentDefId.toDefId()
            currentDefId++
            return id
        }

    private val definedFonts = mutableMapOf<File, String>()
    private val definedGlyphs = mutableMapOf<Font, Array<String>>()
    private val definedImages = mutableMapOf<File, String>()
    private val definedImageMasks = mutableMapOf<File, String>()

    private lateinit var imagesDir: File
    private lateinit var fontsDir: File

    fun renderFrame(frame: FrameGroup, outputFile: File, imagesDir: File, fontsDir: File) {
        val outputDir = outputFile.parentFile
        this.imagesDir = imagesDir.relativeToOrSelf(outputDir)
        this.fontsDir = fontsDir.relativeToOrSelf(outputDir)

        definedFonts.clear()
        definedImages.clear()
        definedImageMasks.clear()
        definedGlyphs.clear()

        var outputStream: OutputStream = FileOutputStream(outputFile)
        if (config.compress) {
            // SVGZ, use GZIP compression stream.
            outputStream = GZIPOutputStream(outputStream)
        }

        outputStream.use {
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

            if (config.fontsMode == SvgFontsMode.NONE) {
                // If using paths for glyphs instead of fonts, start by creating glyph defs.
                // They will be the most used defs so we might as well give them the shortest IDs.
                createGlyphDefs(frame)
            }

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
        svg.group(createClipSvgGraphicsState(group.clips), discardIfEmpty = true) {
            drawSimpleGroup(group)
        }
    }

    private fun drawBlendGroup(group: GroupObject.Blend) {
        svg.group(SvgGraphicsState(mixBlendMode = group.blendMode.toSvgMixBlendModeOrNull()), discardIfEmpty = true) {
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
        if (config.fontsMode == SvgFontsMode.NONE) {
            drawTextWithPaths(text)
            return
        }

        val fontId = createFontDef(text.font)

        val dx = FloatArray(text.glyphOffsets.size + 1) {
            // SVG dx first value is the offset before the first char, whereas in IR the first
            // value is the offset between the 1st and 2nd char. So add leading 0 offset.
            val offset = text.glyphOffsets.getOrElse(it - 1) { 0f }
            // SVG dx values are in user space units, not glyph space units.
            // SVG font size is the size of the EM square so we multiply by that.
            offset / GlyphData.EM_SQUARE_SIZE * text.fontSize
        }

        svg.text(SvgNumber(text.x), SvgNumber(text.y), dx, fontId, text.fontSize, text.text,
            SvgGraphicsState(fill = SvgFillColor(text.color.opaque), fillOpacity = text.color.floatA))
    }

    private fun createFontDef(font: Font): String {
        val fontFile = checkNotNull(font.fontFile) { "Missing font" }
        val svgFontFile = File(fontsDir, fontFile.name)
        return definedFonts.getOrPut(fontFile) {
            val id = nextDefId
            svg.writeDef(null) {
                svg.font(id, when (config.fontsMode) {
                    SvgFontsMode.EXTERNAL -> svgFontFile.invariantSeparatorsPath
                    SvgFontsMode.BASE64 -> fontFile.readBytes().toBase64DataUrl("font/ttf")
                    else -> error("")
                })
            }
            id
        }
    }

    private fun drawTextWithPaths(text: TextObject) {
        val font = text.font

        // Create graphics state for text object.
        // Glyph are defined in glyph space units (EM square), so scale to get desired font size.
        // Also invert Y axis because glyph data is defined in Y positive up coordinate system, SVG is Y positive down.
        val scale = text.fontSize / GlyphData.EM_SQUARE_SIZE
        val transform = SvgTransform.Matrix(scale, 0f, 0f, -scale, text.x, text.y)
        val grState = SvgGraphicsState(
            fill = SvgFillColor(text.color),
            fillOpacity = text.color.floatA,
            transforms = listOf(transform))

        // Use defined glyphs
        if (text.text.length == 1) {
            drawGlyphOrUseGlyphDef(font, text.glyphIndices.first(), grState)
        } else {
            svg.group(grState) {
                var advance = 0f
                for ((i, glyphIndex) in text.glyphIndices.withIndex()) {
                    drawGlyphOrUseGlyphDef(font, glyphIndex,
                        // TODO this should be using x="" not transform
                        SvgGraphicsState(transforms = listOf(SvgTransform.Translate(advance))))
                    advance += font.glyphs[glyphIndex].data.advanceWidth + text.glyphOffsets.getOrElse(i) { 0f }
                }
            }
        }
    }

    private fun drawGlyphOrUseGlyphDef(font: Font, glyphIndex: Int, grState: SvgGraphicsState) {
        when (val glyphId = definedGlyphs[font]?.get(glyphIndex) ?: error("Missing glyph ID")) {
            GLYPH_ID_UNUSED -> Unit // Whitespace
            GLYPH_ID_SINGLE_USE -> drawGlyph(font.glyphs[glyphIndex], grState)
            else -> svg.use(glyphId, grState)
        }
    }

    private fun drawGlyph(glyph: FontGlyph, grState: SvgGraphicsState) {
        svg.path(grState) {
            for (contour in glyph.data.contours) {
                writePath(contour, this)
            }
        }
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
                .copy(fill = SvgFillColor(fill.color.opaque), fillOpacity = fill.color.floatA)
        }
        if (grState != null) {
            svg.path(grState) {
                writePath(path, this)
            }
        }
    }

    private fun drawImage(path: Path, imageFill: PathFillStyle.Image) {
        val imageData = imageFill.imageData
        val dataFile = checkNotNull(imageData.dataFile) { "Missing image" }

        if (imageFill.clip) {
            // Start clipping the image path.
            svg.startGroup(createClipSvgGraphicsState(listOf(path)))
        }

        // Transform in IR scales from image space (1x1 square) to user space.
        // In SVG, we can avoid specifying the image dimensions at user space if they are the same as the file's.
        // So scale the image transform down.
        val transform = AffineTransform(imageFill.transform)
        transform.scale(1.0 / imageData.width, 1.0 / imageData.height)

        // Create the image mask def if needed.
        val maskId = createImageMaskDef(imageData)

        // Draw image
        val grState = SvgGraphicsState(
            transforms = transform.toSvgTransformList(),
            maskId = maskId)
        when (config.imagesMode) {
            SvgImagesMode.EXTERNAL -> {
                // Define image directly using image path URL.
                val href = createImageHref(imageData.format, imageData.data, dataFile)
                svg.image(href, grState = grState)
            }
            SvgImagesMode.BASE64 -> {
                // Create image def to avoid duplicating image data if image is used more than once.
                val imageId = definedImages.getOrPut(dataFile) {
                    svg.writeDef(nextDefId) {
                        val href = createImageHref(imageData.format, imageData.data, dataFile)
                        svg.image(href)
                    }
                }
                svg.use(imageId, grState = grState)
            }
        }

        if (imageFill.clip) {
            // End image clip path.
            svg.endGroup()
        }
    }

    private fun createImageMaskDef(imageData: ImageData): String? {
        val alphaDataFile = imageData.alphaDataFile
        return if (alphaDataFile != null) {
            definedImageMasks.getOrPut(alphaDataFile) {
                val alphaImageHref = createImageHref(imageData.format, imageData.alphaData, alphaDataFile)
                svg.writeDef(nextDefId) {
                    mask {
                        svg.image(alphaImageHref)
                    }
                }
            }
        } else {
            null
        }
    }

    private fun createImageHref(format: ImageFormat, data: ByteArray, file: File) = when (config.imagesMode) {
        SvgImagesMode.EXTERNAL -> File(imagesDir, file.name).invariantSeparatorsPath
        SvgImagesMode.BASE64 -> data.toBase64DataUrl("image/${format.extension}")
    }

    private fun drawGradient(path: Path, gradient: PathFillStyle.Gradient) {
        // Create gradient stops
        val stops = gradient.colors.map {
            SvgGradientStop(it.ratio, it.color.opaque, it.color.floatA)
        }

        val id = svg.writeDef(nextDefId) {
            linearGradient(stops, SvgGradientUnits.USER_SPACE_ON_USE,
                gradient.transform.toSvgTransformList(), x1 = -16384f, x2 = 16384f)
        }
        svg.path(SvgGraphicsState(fill = SvgFillId(id))) {
            writePath(path, this)
        }
    }

    private fun createClipSvgGraphicsState(paths: List<Path>): SvgGraphicsState {
        if (paths.isEmpty()) {
            return SvgStreamWriter.NULL_GRAPHICS_STATE
        }
        val id = svg.writeDef(nextDefId) {
            clipPathData {
                for (path in paths) {
                    writePath(path, this)
                }
            }
        }
        return SvgGraphicsState(clipPathId = id)
    }

    /**
     * Populate the [definedGlyphs] map for each glyph used in [frame]. SVG ID will be created for glyphs
     * used at least twice, otherwise the special ID [GLYPH_ID_SINGLE_USE] is used for glyph used once,
     * and [GLYPH_ID_UNUSED] is used for glyph that aren't used.
     */
    private fun createGlyphDefs(frame: FrameGroup) {
        val allTexts = frame.findAllTextObjectsTo(mutableListOf())
        for (text in allTexts) {
            val font = text.font
            val definedFontGlyphs = definedGlyphs.getOrPut(font) {
                Array(font.glyphs.size) { GLYPH_ID_UNUSED }
            }
            for (glyphIndex in text.glyphIndices) {
                val glyph = font.glyphs[glyphIndex]
                if (glyph.isWhitespace) {
                    // No need to make empty path defs for whitespaces.
                    continue
                }
                if (definedFontGlyphs[glyphIndex] == GLYPH_ID_UNUSED) {
                    // Glyph is used for the first time.
                    definedFontGlyphs[glyphIndex] = GLYPH_ID_SINGLE_USE
                } else if (definedFontGlyphs[glyphIndex] == GLYPH_ID_SINGLE_USE) {
                    // Glyph is used for the second time, create def.
                    definedFontGlyphs[glyphIndex] = svg.writeDef(nextDefId) {
                        drawGlyph(glyph, SvgStreamWriter.NULL_GRAPHICS_STATE)
                    }
                }
            }
        }
    }

    private fun <C : MutableCollection<TextObject>> GroupObject.findAllTextObjectsTo(destination: C): C {
        for (obj in this.objects) {
            if (obj is TextObject) {
                destination += obj
            } else if (obj is GroupObject) {
                obj.findAllTextObjectsTo(destination)
            }
        }
        return destination
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
        return buildString {
            // See https://www.w3.org/TR/2008/REC-xml-20081126/#NT-Name. XML IDs have different chars
            // valid for the first char and the rest of the ID.
            append(XML_NAME_START_CHARS[v % XML_NAME_START_CHARS.length])
            v /= XML_NAME_START_CHARS.length
            while (v > 0) {
                // Use base 64 for following chars.
                append(XML_NAME_CHARS[v % XML_NAME_CHARS.length])
                v /= XML_NAME_CHARS.length
            }
        }
    }

    private fun AffineTransform.toSvgTransformList() = when {
        isIdentity -> null
        type == AffineTransform.TYPE_TRANSLATION -> {
            // Translate only
            listOf(SvgTransform.Translate(translateX.toFloat(), translateY.toFloat()))
        }
        type == AffineTransform.TYPE_UNIFORM_SCALE ||
                type == AffineTransform.TYPE_GENERAL_SCALE -> {
            // Scale only
            listOf(SvgTransform.Scale(scaleX.toFloat(), scaleY.toFloat()))
        }
        else -> listOf(SvgTransform.Matrix(scaleX.toFloat(), shearY.toFloat(), shearX.toFloat(),
            scaleY.toFloat(), translateX.toFloat(), translateY.toFloat()))
    }

    private fun BlendMode.toSvgMixBlendModeOrNull() = when (this) {
        // See: https://drafts.fxtf.org/compositing-1/#blendingseparable
        BlendMode.NULL, BlendMode.NORMAL, BlendMode.LAYER -> SvgMixBlendMode.NORMAL
        BlendMode.MULTIPLY -> SvgMixBlendMode.MULTIPLY
        BlendMode.SCREEN -> SvgMixBlendMode.SCREEN
        BlendMode.LIGHTEN -> SvgMixBlendMode.LIGHTEN
        BlendMode.DARKEN -> SvgMixBlendMode.DARKEN
        BlendMode.DIFFERENCE -> SvgMixBlendMode.DIFFERENCE
        BlendMode.OVERLAY -> SvgMixBlendMode.OVERLAY
        BlendMode.HARDLIGHT -> SvgMixBlendMode.HARD_LIGHT
        else -> {
            // Unsupported blend mode.
            logger.error { "Unsupported blend mode in SVG: $this" }
            null
        }
    }

    private fun PathLineStyle.toSvgGraphicsState() = SvgGraphicsState(
        fill = SvgFillNone,
        stroke = SvgFillColor(this.color.opaque),
        strokeOpacity = this.color.floatA,
        strokeWidth = this.width,
        strokeLineCap = when (this.cap) {
            BasicStroke.CAP_BUTT -> SvgStrokeLineCap.BUTT
            BasicStroke.CAP_ROUND -> SvgStrokeLineCap.ROUND
            BasicStroke.CAP_SQUARE -> SvgStrokeLineCap.SQUARE
            else -> error("Unknown stroke line cap")
        },
        strokeLineJoin = when (this.join) {
            BasicStroke.JOIN_BEVEL -> SvgStrokeLineJoin.BEVEL
            BasicStroke.JOIN_MITER -> if (this.miterLimit == 0f) {
                SvgStrokeLineJoin.MITER
            } else {
                SvgStrokeLineJoin.MITER_CLIP
            }
            BasicStroke.JOIN_ROUND -> SvgStrokeLineJoin.ROUND
            else -> error("Unknown stroke line join")
        },
        strokeMiterLimit = this.miterLimit.takeIf {
            this.join == BasicStroke.JOIN_MITER && it != 0f
        }
    )

    private fun ByteArray.toBase64DataUrl(mimeType: String) = buildString {
        append("data:")
        append(mimeType)
        append(";base64,")
        append(String(Base64.getEncoder().encode(this@toBase64DataUrl)))
    }

    companion object {
        private const val XML_NAME_START_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_:"
        private const val XML_NAME_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_:-."

        private const val GLYPH_ID_UNUSED = "{0}"
        private const val GLYPH_ID_SINGLE_USE = "{1}"
    }

}
