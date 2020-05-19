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

package com.maltaisn.swfconvert.core.config

import com.maltaisn.swfconvert.core.image.ImageFormat
import com.mortennobel.imagescaling.ResampleFilter
import java.io.File


/**
 * Configuration for the conversion to the intermediate format.
 * Not all these options are relevant for all formats however.
 */
data class MainConfiguration(
        // FILES CONFIGURATION

        /** Input files. */
        val input: List<File>,

        /** Output file or directory. */
        val output: File,

        /** Directory to which temp and debug files are written. */
        val tempDir: File,

        // TEXT & FONT CONFIGURATION

        /** Whether to use OCR to detect glyphs with unknown char. */
        val ocrDetectGlyphs: Boolean,

        /** Whether to group fonts that can be merged into a single one. */
        val groupFonts: Boolean,

        // IMAGES CONFIGURATION

        /** Whether to use the same image for all images with the same binary data. */
        val removeDuplicateImages: Boolean,

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
        val imageFormat: ImageFormat? = ImageFormat.JPG
)

