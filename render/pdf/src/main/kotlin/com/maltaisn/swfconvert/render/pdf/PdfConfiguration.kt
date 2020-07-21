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
