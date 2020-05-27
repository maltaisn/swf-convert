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

import com.maltaisn.swfconvert.core.image.ImageFormat
import com.maltaisn.swfconvert.render.core.RenderConfiguration
import com.maltaisn.swfconvert.render.pdf.metadata.PdfMetadata
import java.io.File


/**
 * Configuration for the PDF output format.
 */
data class PdfConfiguration(

        override val output: List<File>,
        override val tempDir: File,

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

        /** Image format to use for rasterized frames. */
        val rasterizationFormat: ImageFormat,

        /** JPEG quality to use for rasterized frames, if format is JPG. */
        val rasterizationJpegQuality: Float,

        // DEBUG OPTIONS

        override val parallelFrameRendering: Boolean,

        /** Whether to rasterize pages in parallel. */
        val parallelRasterization: Boolean

) : RenderConfiguration
