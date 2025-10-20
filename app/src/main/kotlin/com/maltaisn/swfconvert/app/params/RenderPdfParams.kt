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

package com.maltaisn.swfconvert.app.params

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.beust.jcommander.ParametersDelegate
import com.maltaisn.swfconvert.app.configError
import com.maltaisn.swfconvert.core.YAxisDirection
import com.maltaisn.swfconvert.core.image.ImageFormat
import com.maltaisn.swfconvert.render.pdf.PdfConfiguration
import com.maltaisn.swfconvert.render.pdf.metadata.PdfMetadata
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.text.DecimalFormat

@Suppress("MagicNumber")
@Parameters(commandDescription = "PDF output format")
internal class RenderPdfParams : RenderParams<PdfConfiguration> {

    @ParametersDelegate
    override var params = CoreParams(true) { "pdf" }

    // General

    @Parameter(
        names = ["--no-compress"],
        description = "Disable output PDF compression.",
        order = 1000)
    private var noCompress: Boolean = false

    // Metadata

    @Parameter(
        names = ["--metadata"],
        variableArity = true,
        description = "JSON files containing metadata to set on PDF files.",
        splitter = NoSplitter::class,
        order = 1100)
    private var metadata: List<String> = emptyList()

    @Parameter(
        names = ["--dont-optimize-page-labels"],
        description = "Disable page labels optimization",
        order = 1110)
    private var dontOptimizePageLabels: Boolean = true

    // Rasterization

    @Parameter(
        names = ["--rasterization-enabled"],
        description = "Enable rasterization of frames with complex shapes.",
        order = 1200)
    private var rasterizationEnabled: Boolean = false

    @Parameter(
        names = ["--rasterization-threshold"],
        description = "Minimum input file complexity required to perform rasterization, in arbitrary units.",
        order = 1210)
    private var rasterizationThreshold = 100000

    @Parameter(
        names = ["--rasterization-dpi"],
        description = "Density in DPI to use to rasterize frames if rasterization is enabled.",
        order = 1220)
    private var rasterizationDpi = 200f

    @Parameter(
        names = ["--rasterization-format"],
        description = "Image format to use for rasterized frames.",
        order = 1230)
    private var rasterizationFormatName = ImageFormat.JPG.extension

    @Parameter(
        names = ["--rasterization-jpeg-quality"],
        description = "JPEG image quality for rasterization, between 0 and 100.",
        order = 1240)
    private var rasterizationJpegQuality = 75

    override val yAxisDirection: YAxisDirection
        get() = YAxisDirection.UP

    private val pdfMetadata: List<PdfMetadata?> by lazy {
        if (metadata.isEmpty()) {
            return@lazy emptyList()
        }

        checkNoOptionsInArgs(metadata)

        return@lazy metadata.map { filename ->
            if (filename == "_") {
                null
            } else {
                val file = File(filename)
                configError(file.exists()) { "PDF metadata file '$filename' doesn't exist." }
                configError(file.extension.lowercase() == "json") {
                    "PDF metadata file '$filename' is not a JSON file."
                }

                try {
                    Json.decodeFromString(PdfMetadata.serializer(), file.readText())
                } catch (e: SerializationException) {
                    configError("Error while parsing PDF metadata file at '$filename'", e)
                } catch (e: IOException) {
                    configError("Could not read PDF metadata file at '$filename'", e)
                }
            }
        }
    }

    private val rasterizationJpegQualityFloat: Float by lazy {
        configError(rasterizationJpegQuality in 0..100) { "Rasterization JPEG quality must be between 0 and 100." }
        rasterizationJpegQuality / 100f
    }

    private val rasterizationFormat: ImageFormat by lazy {
        when (rasterizationFormatName) {
            "jpg", "jpeg" -> ImageFormat.JPG
            "png" -> ImageFormat.PNG
            else -> configError("Invalid rasterization image format '$rasterizationFormatName'.")
        }
    }

    override fun createConfigurations(inputs: List<List<File>>): List<PdfConfiguration> {
        configError(rasterizationDpi in 10f..2000f) { "Rasterization density must be between 10 and 2000 DPI." }
        configError(rasterizationThreshold >= 0) { "Rasterization threshold complexity must be greater or equal to 0." }

        val parallelRasterization = params.params[OPT_PARALLEL_RASTERIZATION]?.toBooleanOrNull() ?: true

        return inputs.mapIndexed { i, input ->
            val tempDir = params.getTempDirForInput(input)
            PdfConfiguration(
                params.outputFiles[i],
                tempDir,
                !noCompress,
                pdfMetadata.getOrNull(i),
                !dontOptimizePageLabels,
                rasterizationEnabled,
                rasterizationThreshold,
                rasterizationDpi,
                rasterizationFormat,
                rasterizationJpegQualityFloat,
                params.parallelFrameRendering,
                parallelRasterization)
        }
    }

    override fun print() {
        println("Compress PDF: ${!noCompress}")
        println("Add metadata: ${metadata.isNotEmpty()}")
        if (metadata.isNotEmpty()) {
            println("  Optimize page labels: ${!dontOptimizePageLabels}")
        }
        println("Rasterization enabled: $rasterizationEnabled")
        if (rasterizationEnabled) {
            println("""
                |  Rasterization threshold : ${NUMBER_FMT.format(rasterizationThreshold)}
                |  Rasterization DPI : ${NUMBER_FMT.format(rasterizationDpi)}
                |  Rasterization JPEG quality: $rasterizationJpegQuality %
                |  Rasterization format : ${rasterizationFormat.name.lowercase()}
            """.trimMargin())
        }
    }

    companion object {
        private val NUMBER_FMT = DecimalFormat()

        const val OPT_PARALLEL_RASTERIZATION = "parallelRasterization"
    }

}
