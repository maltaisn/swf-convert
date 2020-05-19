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

package com.maltaisn.swfconvert.app.params

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.beust.jcommander.ParametersDelegate
import com.maltaisn.swfconvert.app.checkNoOptionsInArgs
import com.maltaisn.swfconvert.app.configError
import com.maltaisn.swfconvert.render.pdf.PdfConfiguration
import com.maltaisn.swfconvert.render.pdf.metadata.PdfMetadata
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File
import java.io.IOException
import java.text.DecimalFormat


@Parameters(commandDescription = "PDF output format")
class FormatPdfParams : FormatParams<PdfConfiguration> {

    @ParametersDelegate
    override var params = BaseParams(true, "pdf")

    // General

    @Parameter(names = ["--compress"], description = "Whether to enable rasterization of complex input files or not.", order = 1000)
    var compress: Boolean = false

    // Metadata

    @Parameter(names = ["--metadata"], variableArity = true, description = "JSON files containing metadata to set on PDF files.", order = 1100)
    var metadata: List<String> = emptyList()

    @Parameter(names = ["--optimize-page-labels"], description = "Whether to optimize page labels or not.", order = 1110)
    var optimizePageLabels: Boolean = true

    // Rasterization

    @Parameter(names = ["--rasterization-enabled"], description = "Whether to enable rasterization of complex input files or not.", order = 1200)
    var rasterizationEnabled: Boolean = false

    @Parameter(names = ["--rasterization-threshold"], description = "Minimum input file complexity required to perform rasterization, in arbitrary units.", order = 1210)
    var rasterizationThreshold = 100000

    @Parameter(names = ["--rasterization-dpi"], description = "Density in DPI to use to rasterize output if rasterization is enabled.", order = 1220)
    var rasterizationDpi = 200f

    @Parameter(names = ["--rasterizer"], description = "External program used to rasterize output files.", order = 1230)
    var rasterizer: String = "pdfbox"

    @Parameter(names = ["--rasterizer-args"], description = "Arguments to use with rasterizer to rasterize files of specified output format.", order = 1240)
    var rasterizerArgs: String = "--input=%1\$s --dpi=%2\$s --output=%3\$s"
    // Note: default arguments are just to show interpolation parameters in help, they aren't really used.


    private val pdfMetadata: List<PdfMetadata> by lazy {
        if (metadata.isEmpty()) {
            return@lazy emptyList()
        }

        checkNoOptionsInArgs(metadata)

        val json = Json(JsonConfiguration.Stable)
        return@lazy metadata.map { filename ->
            val file = File(filename)
            configError(file.exists()) { "PDF metadata file '$filename' doesn't exist." }
            configError(file.extension.toLowerCase() == "json") {
                "PDF metadata file '$filename' is not a JSON file."
            }

            try {
                json.parse(PdfMetadata.serializer(), file.readText())
            } catch (e: SerializationException) {
                configError("Error while parsing PDF metadata file at '$filename'", e)
            } catch (e: IOException) {
                configError("Could not read PDF metadata file at '$filename'", e)
            }
        }
    }

    override fun createConfigurations(count: Int): List<PdfConfiguration> {
        configError(rasterizationDpi in 10f..2000f) { "Rasterization density must be between 10 and 2000 DPI." }
        configError(rasterizationThreshold >= 0) { "Rasterization threshold complexity must be greater or equal to 0." }

        val config = PdfConfiguration(
                compress,
                null,
                optimizePageLabels,
                rasterizationEnabled,
                rasterizationThreshold,
                rasterizationDpi,
                rasterizer,
                rasterizerArgs)

        return if (pdfMetadata.isEmpty()) {
            List(count) { config }
        } else {
            List(count) { config.copy(metadata = pdfMetadata.getOrNull(it)) }
        }
    }

    override fun print() {
        println("Compress PDF: $compress")
        println("Add metadata: ${metadata.isNotEmpty()}")
        if (metadata.isNotEmpty()) {
            println("Optimize page labels: $optimizePageLabels")
        }
        if (rasterizationEnabled) {
            println("""
                |Rasterization threshold : ${NUMBER_FMT.format(rasterizationThreshold)}
                |Rasterization DPI : ${NUMBER_FMT.format(rasterizationDpi)}
                |Rasterizer: $rasterizer
            """.trimMargin())
        }
    }

    companion object {
        private val NUMBER_FMT = DecimalFormat()
    }

}
