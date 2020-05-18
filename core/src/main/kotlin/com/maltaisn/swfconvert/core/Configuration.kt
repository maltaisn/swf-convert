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

package com.maltaisn.swfconvert.core

import com.maltaisn.swfconvert.core.frame.FrameRenderer
import com.maltaisn.swfconvert.core.image.ImageFormat
import com.mortennobel.imagescaling.ResampleFilter
import java.io.File


/**
 * Configuration for the conversion.
 */
data class Configuration(
        // FILES CONFIGURATION

        /** Input files. */
        val input: List<File>,

        /** Output file or directory. */
        val output: File,

        /** Directory to which temp and debug files are written. */
        val tempDir: File,

        // Output format factories.
        val rendererFactory: () -> FrameRenderer,


        // TEXT & FONT CONFIGURATION

        /** Whether to use OCR to detect glyphs with unknown char. */
        var ocrDetectGlyphs: Boolean,

        /** Whether to group fonts that can be merged into a single one. */
        var groupFonts: Boolean,

        // IMAGES CONFIGURATION

        /** Whether to use the same image for all images with the same binary data. */
        var removeDuplicateImages: Boolean,

        /** Whether to downsample big images to reduce output size. */
        var downsampleImages: Boolean,

        /** Filter used to downsample images, or `null` to use default Java API. */
        var downsampleFilter: ResampleFilter?,

        /**
         * Size in pixels under which images are never downsampled. For example if minimum is 10 px,
         * a 8x8 image is never downsampled, and a 20x20 image will be downsampled to 10x10, not 5x5.
         * Minimum value is 3 pixels.
         */
        var downsampleMinSize: Int,

        /** If downsampling images, the maximum allowed image density. */
        var maxDpi: Float,

        /** JPEG image compression quality. 0 is the worst quality and 1 is the best. */
        var jpegQuality: Float,

        /**
         * If `null`, the original image format will be used, that is PNG for DefineBitsLossless
         * tags and JPG for DefineBitsJPEG tags. Otherwise, a specific format can be forced to be
         * used for all images. JPEG images with an alpha channel will use a soft mask.
         */
        var imageFormat: ImageFormat? = ImageFormat.JPG,

        // RASTERIZATION CONFIGURATION

        /**
         * Whether to enable rasterization of complex input files or not.
         * Rasterization is used to reduce file size when some input files have too complex shapes.
         */
        var rasterizationEnabled: Boolean,

        /**
         * Minimum input file complexity required to perform rasterization.
         * At some point though, rasterization might increase file size,
         * so minimum complexity should be kept high enough.
         */
        var rasterizationThreshold: Int,

        /** Density to use to rasterize output files if rasterization is enabled. */
        var rasterizationDpi: Float,

        /** Program used to rasterize output files, or `"pdfbox"` to use internal rasterizer. */
        var rasterizer: String,

        /**
         * Arguments to use with [rasterizer] to rasterize output files.
         * These are ignored if rasterizer is the internal one.
         *
         * Format arguments are:
         * 1. Input file path.
         * 2. Output desired DPI.
         * 3. Output PNG file path.
         *
         * For example, to use inkscape as an external rasterizer, this can be set to:
         * `"\"%1\$s\" -z --export-dpi=%2\$s --export-area-page --export-png=\"%3\$s\""`.
         */
        var rasterizerArgs: String
)

