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

package com.maltaisn.swfconvert.convert

import com.maltaisn.swfconvert.core.YAxisDirection
import com.maltaisn.swfconvert.core.image.Color
import com.maltaisn.swfconvert.core.image.ImageFormat
import com.maltaisn.swfconvert.core.shape.PathLineStyle
import com.maltaisn.swfconvert.core.text.FontScale
import com.mortennobel.imagescaling.ResampleFilter
import java.awt.BasicStroke
import java.awt.geom.AffineTransform
import java.io.File

/**
 * Configuration for the conversion to the intermediate representation.
 */
data class ConvertConfiguration(
    // FILES CONFIGURATION

    /** Input files. */
    val input: List<File>,

    /** Directory to which temp and debug files are written. */
    val tempDir: File,

    /**
     * Direction of the Y axis, either up or down. In SWF, Y axis goes down, but in
     * some output formats, Y axis goes up, so IR is generated in the right direction immediately.
     * Reversing Y axis involves flipping images and fonts vertically and swapping transformation
     * matrices rotation.
     */
    val yAxisDirection: YAxisDirection,

    /**
     * Whether to ignore empty frames, not creating intermediate frames for them.
     */
    val ignoreEmptyFrames: Boolean,

    // TEXT & FONT CONFIGURATION

    /** Whether to group fonts that can be merged into a single one. */
    val groupFonts: Boolean,

    /** Whether to keep original font names for font files or reassign generic names. */
    val keepFontNames: Boolean,

    // IMAGES CONFIGURATION

    /** Whether to keep duplicate images with the same binary data. */
    val keepDuplicateImages: Boolean,

    /** Whether to downsample big images to reduce output size. */
    val downsampleImages: Boolean,

    /** Filter used to downsample images, or `null` to use default Java API. */
    val downsampleFilter: ResampleFilter?,

    /**
     * Size in pixels under which images are never downsampled. For example if minimum is 10 px,
     * a 8x8 image is never downsampled, and a 20x20 image will be downsampled to 10x10, not 5x5.
     * Minimum value is 3 pixels.
     */
    val downsampleMinSize: Int,

    /** If downsampling images, the maximum allowed image density. */
    val maxDpi: Float,

    /** JPEG image compression quality. 0 is the worst quality and 1 is the best. */
    val jpegQuality: Float,

    /**
     * If `null`, the original image format will be used, that is PNG for DefineBitsLossless
     * tags and JPG for DefineBitsJPEG tags. Otherwise, a specific format can be forced to be
     * used for all images. JPEG images with an alpha channel will use a soft mask.
     */
    val imageFormat: ImageFormat? = ImageFormat.JPG,

    // PARALLELIZATION OPTIONS

    /** Whether to decode SWF files in parallel. */
    val parallelSwfDecoding: Boolean,

    /** Whether to convert input files to intermediate format in parallel. */
    val parallelSwfConversion: Boolean,

    /** Whether to create and optimize output images in parallel. */
    val parallelImageCreation: Boolean,

    // DEBUG OPTIONS

    /** Whether to keep the `fonts/` sub-directory after conversion. */
    val keepFonts: Boolean,

    /** Whether to keep the `images/` sub-directory after conversion. */
    val keepImages: Boolean,

    /** Whether to draw shape bounds to output. */
    val drawShapeBounds: Boolean,

    /** Whether to draw text bounds to output. */
    val drawTextBounds: Boolean,

    /** Whether to draw clip bounds to output. */
    val drawClipBounds: Boolean,

    /** Whether to disable clipping in output. */
    val disableClipping: Boolean,

    /** Whether to disable blending (except alpha). */
    val disableBlending: Boolean,

    /** Whether to disable masking (alpha blend mode). */
    val disableMasking: Boolean,

    /** Padding added around output, in inches. */
    val framePadding: Float,

    /** Font scale options for DefineFont2 tag. */
    val fontScale2: FontScale,
    /** Font scale options for DefineFont3 tag. */
    val fontScale3: FontScale,

    /** If not null, overrides frame size defined in swf, for all frames. Dimensions in are in inches. */
    val frameSize: List<Float>?,

    /** Preconcatenated transform on bitmap fill matrix, see issue #2. */
    val bitmapMatrixTransform: AffineTransform,

    /**
     * If all glyph offsets are under this threshold, they are ignored.
     * This can allow for reduced size since no extra information has to be specified.
     * Value is in glyph space units.
     */
    val ignoreGlyphOffsetsThreshold: Float,

    /**
     * Whether to create frames recursively from sprites. If false, then the place list is only parse
     * for ShowFrame tags at the top level, not at the sprite level. If true, then frames contained
     * in sprites will be used and brought to the top level, recursively.
     */
    val recursiveFrames: Boolean,

    // Debug line style options
    val debugLineWidth: Float,
    val debugLineColor: Color
) {

    val debugLineStyle = PathLineStyle(debugLineColor, debugLineWidth,
        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0f)

    // Temporary directories for fonts and images based on [tempDir]
    val fontsDir = File(tempDir, "fonts")
    val imagesDir = File(tempDir, "images")

}
