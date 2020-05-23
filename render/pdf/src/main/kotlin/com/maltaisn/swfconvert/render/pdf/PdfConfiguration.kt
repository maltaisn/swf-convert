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

import com.maltaisn.swfconvert.core.config.Configuration
import com.maltaisn.swfconvert.core.config.FormatConfiguration
import com.maltaisn.swfconvert.core.frame.FramesRenderer
import com.maltaisn.swfconvert.render.pdf.metadata.PdfMetadata
import com.maltaisn.swfconvert.render.pdf.rasterize.PdfBoxFrameRasterizer
import kotlinx.coroutines.CoroutineScope


/**
 * Configuration for the PDF output format.
 */
data class PdfConfiguration(

        /** Whether to compress output files or not. */
        val compress: Boolean,

        // METADATA CONFIGURATION

        val metadata: PdfMetadata?,

        /** Whether to optimize page labels or not. */
        val optimizePageLabels: Boolean,

        // RASTERIZATION CONFIGURATION

        /**
         * Whether to enable rasterization of complex input files or not.
         * Rasterization is used to reduce file size when some input files have too complex shapes.
         */
        val rasterizationEnabled: Boolean,

        /**
         * Minimum input file complexity required to perform rasterization.
         * At some point though, rasterization might increase file size,
         * so minimum complexity should be kept high enough.
         */
        val rasterizationThreshold: Int,

        /** Density to use to rasterize output files if rasterization is enabled. */
        val rasterizationDpi: Float,

        /** Program used to rasterize output files, or `"pdfbox"` to use internal rasterizer. */
        val rasterizer: String,

        /**
         * Arguments to use with [rasterizer] to rasterize output files.
         * These are ignored if rasterizer is the internal one.
         *
         * Format arguments are:
         * 1. Input file path (surrounded with double quotes).
         * 2. Output desired DPI.
         * 3. Output PNG file path (surrounded with double quotes).
         *
         * For example, to use inkscape as an external rasterizer, this can be set to:
         * `"%1\$s -z --export-dpi=%2\$s --export-area-page --export-png=%3\$s"`.
         */
        val rasterizerArgs: String,

        /** Whether to rasterize pages in parallel. */
        val parallelRasterization: Boolean

) : FormatConfiguration<PdfConfiguration> {

    override fun createRenderer(coroutineScope: CoroutineScope,
                                config: Configuration): FramesRenderer =
            PdfFramesRenderer(coroutineScope, config)

    companion object {
        const val INTERNAL_RASTERIZER = PdfBoxFrameRasterizer.NAME
    }

}
