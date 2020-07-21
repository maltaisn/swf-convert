/*
 * Copyright (C) 2020 Nicolas Maltais
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 */

package com.maltaisn.swfconvert.render.svg

import com.maltaisn.swfconvert.core.BlendMode
import com.maltaisn.swfconvert.core.FrameGroup
import com.maltaisn.swfconvert.core.FrameObject
import com.maltaisn.swfconvert.core.GroupObject
import com.maltaisn.swfconvert.core.image.ImageFormat
import com.maltaisn.swfconvert.core.shape.Path
import com.maltaisn.swfconvert.core.shape.PathElement.ClosePath
import com.maltaisn.swfconvert.core.shape.PathElement.CubicTo
import com.maltaisn.swfconvert.core.shape.PathElement.LineTo
import com.maltaisn.swfconvert.core.shape.PathElement.MoveTo
import com.maltaisn.swfconvert.core.shape.PathElement.QuadTo
import com.maltaisn.swfconvert.core.shape.PathFillStyle
import com.maltaisn.swfconvert.core.shape.PathLineStyle
import com.maltaisn.swfconvert.core.shape.ShapeObject
import com.maltaisn.swfconvert.core.text.Font
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
    private val config: SvgConfiguration,
    private val frameDefsCreator: SvgFrameDefsCreator
) {

    private val logger = logger()

    private lateinit var svg: SvgStreamWriter

    private lateinit var imagesDir: File
    private lateinit var fontsDir: File

    private lateinit var defs: Map<FrameDef, String>

    fun renderFrame(frame: FrameGroup, outputFile: File, imagesDir: File, fontsDir: File) {
        val outputDir = outputFile.parentFile
        this.imagesDir = imagesDir.relativeToOrSelf(outputDir)
        this.fontsDir = fontsDir.relativeToOrSelf(outputDir)

        defs = frameDefsCreator.createFrameDefs(frame)

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
                    clipRule = SvgFillRule.EVEN_ODD,
                    preserveAspectRatio = SvgPreserveAspectRatio.NONE))
            // FrameGroup transform is ignored because the transform is already created by the
            // viewBox having a different size than the one set by 'width' and 'height'.

            drawSimpleGroup(frame)
            writeAllDefs()

            svg.end()
        }
    }

    private fun writeAllDefs() {
        if (defs.isNotEmpty()) {
            svg.writeDefs {
                for ((def, id) in defs) {
                    writeDef(def, id)
                }
            }
        }
    }

    private fun writeDef(def: FrameDef, id: String) {
        svg.writeDef(id.takeIf { def !is FrameDef.FontDef }) {
            when (def) {
                is FrameDef.FontDef -> writeFontDef(def, id)
                is FrameDef.ImageDef -> writeImageDef(def)
                is FrameDef.ImageMaskDef -> writeImageMaskDef(def)
                is FrameDef.MaskDef -> writeMaskDef(def)
                is FrameDef.ClipDef -> writeClipDef(def)
                is FrameDef.GlyphDef -> writeGlyphDef(def)
                is FrameDef.GradientDef -> writeGradientDef(def)
            }
        }
    }

    private fun writeFontDef(def: FrameDef.FontDef, id: String) {
        svg.font(id, when (config.fontsMode) {
            SvgFontsMode.EXTERNAL -> File(fontsDir, def.file.name).invariantSeparatorsPath
            SvgFontsMode.BASE64 -> def.file.readBytes().toBase64DataUrl("font/ttf")
            else -> error("")
        })
    }

    private fun writeImageDef(def: FrameDef.ImageDef) {
        val imageData = def.imageData
        svg.image(createImageHref(imageData.format, imageData.data, def.file))
    }

    private fun writeImageMaskDef(def: FrameDef.ImageMaskDef) {
        val imageData = def.imageData
        svg.mask {
            svg.image(createImageHref(imageData.format, imageData.alphaData, def.file))
        }
    }

    private fun writeMaskDef(def: FrameDef.MaskDef) {
        svg.mask {
            drawObject(def.mask)
        }
    }

    private fun writeClipDef(def: FrameDef.ClipDef) {
        svg.clipPathData {
            for (path in def.paths) {
                writePath(path, this)
            }
        }
    }

    private fun writeGlyphDef(def: FrameDef.GlyphDef) {
        svg.path {
            for (contour in def.contours) {
                writePath(contour, this)
            }
        }
    }

    private fun writeGradientDef(def: FrameDef.GradientDef) {
        val stops = def.gradient.colors.map {
            SvgGradientStop(it.ratio, it.color.opaque, it.color.floatA)
        }
        svg.linearGradient(stops, SvgGradientUnits.USER_SPACE_ON_USE,
            def.gradient.transform.toSvgTransformList(), x1 = -16384f, x2 = 16384f)
    }

    private fun requireDef(def: FrameDef) = checkNotNull(defs[def]) { "Missing def for $def" }

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
            svg.group(SvgGraphicsState(transform = transforms)) {
                drawSimpleGroup(group)
            }
        } else {
            // Identity transform.
            drawSimpleGroup(group)
        }
    }

    private fun drawClipGroup(group: GroupObject.Clip) {
        svg.group(SvgGraphicsState(clipPathId = requireDef(FrameDef.ClipDef(group.clips))), discardIfEmpty = true) {
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

        svg.group(SvgGraphicsState(maskId = requireDef(FrameDef.MaskDef(group.objects.last())))) {
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

        val fontFile = checkNotNull(text.font.fontFile) { "Missing font" }
        val fontId = requireDef(FrameDef.FontDef(fontFile))

        val dx = FloatArray(text.glyphOffsets.size + 1) {
            // SVG dx first value is the offset before the first char, whereas in IR the first
            // value is the offset between the 1st and 2nd char. So add leading 0 offset.
            val offset = text.glyphOffsets.getOrElse(it - 1) { 0f }
            // SVG dx values are in user space units, not glyph space units.
            // SVG font size is the size of the EM square so we multiply by that.
            offset / GlyphData.EM_SQUARE_SIZE * text.fontSize
        }

        svg.text(text.text, dx,
            SvgGraphicsState(
                x = SvgNumber(text.x),
                y = SvgNumber(text.y),
                fontFamily = fontId,
                fontSize = text.fontSize,
                fill = SvgFillColor(text.color.opaque),
                fillOpacity = text.color.floatA))
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
            transform = listOf(transform))

        // Use defined glyphs
        if (text.text.length == 1) {
            drawGlyphOrUseGlyphDef(font, text.glyphIndices.first(), grState)
        } else {
            svg.group(grState) {
                var advance = 0f
                for ((i, glyphIndex) in text.glyphIndices.withIndex()) {
                    drawGlyphOrUseGlyphDef(font, glyphIndex, SvgGraphicsState(x = SvgNumber(advance)))
                    advance += font.glyphs[glyphIndex].data.advanceWidth + text.glyphOffsets.getOrElse(i) { 0f }
                }
            }
        }
    }

    private fun drawGlyphOrUseGlyphDef(font: Font, glyphIndex: Int, grState: SvgGraphicsState) {
        val glyph = font.glyphs[glyphIndex]
        if (glyph.isWhitespace) {
            return
        }
        svg.use(requireDef(FrameDef.GlyphDef(glyph.data.contours)), grState)
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
            grState = SvgGraphicsState(
                base = grState ?: SvgGraphicsState.NULL,
                fill = SvgFillColor(fill.color.opaque),
                fillOpacity = fill.color.floatA)
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
        val alphaDataFile = imageData.alphaDataFile

        if (imageFill.clip) {
            svg.startGroup(SvgGraphicsState(clipPathId = requireDef(FrameDef.ClipDef(listOf(path)))))
        }

        // Transform in IR scales from image space (1x1 square) to user space.
        // In SVG, we can avoid specifying the image dimensions at user space if they are the same as the file's.
        // So scale the image transform down.
        val transform = AffineTransform(imageFill.transform)
        transform.scale(1.0 / imageData.width, 1.0 / imageData.height)

        // Create the image mask def if needed.
        val maskId = if (alphaDataFile != null) {
            requireDef(FrameDef.ImageMaskDef(alphaDataFile, imageData))
        } else {
            null
        }

        // Draw image
        val grState = SvgGraphicsState(
            transform = transform.toSvgTransformList(),
            maskId = maskId)
        when (config.imagesMode) {
            SvgImagesMode.EXTERNAL -> {
                // Define image directly using image path URL.
                val href = createImageHref(imageData.format, imageData.data, dataFile)
                svg.image(href, grState = grState)
            }
            SvgImagesMode.BASE64 -> {
                // Create image def to avoid duplicating image data if image is used more than once.
                val imageId = requireDef(FrameDef.ImageDef(dataFile, imageData))
                svg.use(imageId, grState = grState)
            }
        }

        if (imageFill.clip) {
            svg.endGroup()
        }
    }

    private fun createImageHref(format: ImageFormat, data: ByteArray, file: File) = when (config.imagesMode) {
        SvgImagesMode.EXTERNAL -> File(imagesDir, file.name).invariantSeparatorsPath
        SvgImagesMode.BASE64 -> data.toBase64DataUrl("image/${format.extension}")
    }

    private fun drawGradient(path: Path, gradient: PathFillStyle.Gradient) {
        val gradientId = requireDef(FrameDef.GradientDef(gradient))
        svg.path(SvgGraphicsState(fill = SvgFillId(gradientId))) {
            writePath(path, this)
        }
    }

    private fun writePath(path: Path, pathWriter: SvgPathWriter) = pathWriter.apply {
        for (e in path.elements) {
            when (e) {
                is MoveTo -> moveTo(e.x, e.y)
                is LineTo -> lineTo(e.x, e.y)
                is QuadTo -> quadTo(e.cx, e.cy, e.x, e.y)
                is CubicTo -> cubicTo(e.c1x, e.c1y, e.c2x, e.c2y, e.x, e.y)
                is ClosePath -> closePath()
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
}
