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

package com.maltaisn.swfconvert.app

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.maltaisn.swfconvert.core.Configuration
import com.maltaisn.swfconvert.core.image.ImageFormat
import com.mortennobel.imagescaling.ResampleFilter
import com.mortennobel.imagescaling.ResampleFilters
import java.io.File


@Parameters
class Args {

    // Files configuration

    @Parameter(description = "Input files or directories")
    var input: List<String> = mutableListOf()

    @Parameter(names = ["-o", "--output"], variableArity = true, description = "Output files or directories.", order = 0)
    var output: List<String> = mutableListOf()

    @Parameter(names = ["-t", "--tempdir"], description = "Temp directory used for debugging and intermediate files.", order = 10)
    var tempDir: String? = null

    @Parameter(names = ["-f", "--format"], description = "Output format: ir | pdf", order = 20)
    var outputFormatName: String = "pdf"

    // Text & font configuration

    @Parameter(names = ["--enable-glyph-ocr"], description = "Whether to enable OCR to detect glyphs with unknown code.", order = 30)
    var ocrDetectGlyphs: Boolean = false

    /** Whether to group fonts that can be merged into a single one. */
    @Parameter(names = ["--group-fonts"], description = "Whether to group fonts that can be merged into a single one.", order = 40)
    var groupFonts: Boolean = true

    // Images configuration

    @Parameter(names = ["--remove-duplicate-images"], description = "Whether to use the same image for all images with the same binary data.", order = 50)
    var removeDuplicateImages: Boolean = true

    @Parameter(names = ["--downsample-images"], description = "Whether to downsample big images to reduce output size.", order = 60)
    var downsampleImages: Boolean = false

    @Parameter(names = ["--downsample-filter"], description = "Filter used to downsample images: fast | bell | bicubic | bicubichf | box | bspline | hermite | lanczos3 | mitchell | triangle.", order = 70)
    var downsampleFilterName: String = "lanczos3"

    @Parameter(names = ["--downsample-min-size"], description = "Minimum size in pixels that images are downsampled to or from. Must be at least 3 px.", order = 80)
    var downsampleMinSize: Int = 10

    /** If downsampling images, the maximum allowed image density. */
    @Parameter(names = ["--max-dpi"], description = "Maximum image density in DPI.", order = 90)
    var maxDpi: Float = 200f

    @Parameter(names = ["--jpeg-quality"], description = "JPEG image quality between 0 and 100.", order = 100)
    var jpegQuality: Int = 75

    @Parameter(names = ["--image-format"], description = "Format to use for images: default | jpg | png", order = 110)
    var imageFormatName: String = "default"

    // Rasterization configuration

    @Parameter(names = ["--rasterization-enabled"], description = "Whether to enable rasterization of complex input files or not.", order = 120)
    var rasterizationEnabled: Boolean = false

    @Parameter(names = ["--rasterization-threshold"], description = "Minimum input file complexity required to perform rasterization, in arbitrary units.", order = 130)
    var rasterizationThreshold = 100000

    @Parameter(names = ["--rasterization-dpi"], description = "Density in DPI to use to rasterize output if rasterization is enabled.", order = 140)
    var rasterizationDpi = 200f

    @Parameter(names = ["--rasterizer"], description = "External program used to rasterize output files.", order = 150)
    var rasterizer: String = "pdfbox"

    @Parameter(names = ["--rasterizer-args"], description = "Arguments to use with rasterizer to rasterize files of specified output format.", order = 160)
    var rasterizerArgs: String = "--input=%1\$s --dpi=%2\$s --output=%3\$s"

    // Other

    @Parameter(names = ["-h", "--help"], description = "Show help message.", help = true, order = 1000)
    var help = false

    @Parameter(names = ["-v", "--version"], description = "Show version.", order = 1010)
    var version = false

    private var _inputFileCollections: List<List<File>>? = null
    private val inputFileCollections: List<List<File>>
        get() {
            if (_inputFileCollections != null) {
                return _inputFileCollections!!
            }
            argError(input.isNotEmpty()) { "No input files." }

            val collections = mutableListOf<List<File>>()
            for (filename in input) {
                val file = File(filename)
                argError(file.exists()) {
                    if (filename.startsWith("--")) {
                        // User probably made a typo in an option.
                        "Unknown option '$filename'"
                    } else {
                        "Input file '$filename' doesn't exist."
                    }
                }

                collections += if (file.isDirectory) {
                    // File is directory, create collection for its content
                    val collection = file.listFiles()!!.filter { it.isSwfFile() }
                    collection.sortedBy { it.nameWithoutExtension }
                    argError(collection.isNotEmpty()) { "Input folder '$filename' has no SWF files." }
                    collection

                } else {
                    // Create collection from file.
                    argError(file.isSwfFile()) { "Input file '$filename' is not a SWF file." }
                    listOf(file)
                }
            }

            _inputFileCollections = collections
            return collections
        }

    private fun File.isSwfFile() = this.extension.toLowerCase() == "swf"


    private var _outputFiles: List<File>? = null
    private val outputFiles: List<File>
        get() {
            if (_outputFiles != null) {
                return _outputFiles!!
            }

            val input = inputFileCollections
            if (output.isEmpty()) {
                // Use same directory as input collections
                _outputFiles = input.map { it.first().parentFile }

            } else {
                argError(output.size == input.size) {
                    "Expected as many output files or directories as input."
                }

                val outputFiles = mutableListOf<File>()
                for ((i, filename) in output.withIndex()) {
                    val file = File(filename)
                    outputFiles += file
                    if (file.name.matches("""^.\..*$""".toRegex())) {
                        val format = outputFormat
                        argError(format.singleFileOutput || input[i].size == 1) {
                            "Cannot use single output file for multiple input files and that output format."
                        }

                        // Treat as a file, check extension.
                        when (format) {
                            OutputFormat.PDF -> argError(file.extension.toLowerCase() == "pdf") {
                                "Output file for PDF output should have PDF extension."
                            }
                        }

                    } else {
                        // Treat as directory, create it.
                        file.mkdirs()
                    }
                }
                _outputFiles = outputFiles
            }
            return _outputFiles!!
        }

    private val outputFormat: OutputFormat
        get() = when (outputFormatName.toLowerCase()) {
            "pdf" -> OutputFormat.PDF
            else -> argError("Unknown output format '$outputFormatName'.")
        }

    private val downsampleFilter: ResampleFilter?
        get() = when (downsampleFilterName.toLowerCase()) {
            "fast" -> null
            "bell" -> ResampleFilters.getBellFilter()
            "bicubic" -> ResampleFilters.getBiCubicFilter()
            "bicubichf" -> ResampleFilters.getBiCubicHighFreqResponse()
            "box" -> ResampleFilters.getBoxFilter()
            "bspline" -> ResampleFilters.getBSplineFilter()
            "hermite" -> ResampleFilters.getHermiteFilter()
            "lanczos3" -> ResampleFilters.getLanczos3Filter()
            "mitchell" -> ResampleFilters.getMitchellFilter()
            "triangle" -> ResampleFilters.getTriangleFilter()
            else -> argError("Unknown downsampling filter '$downsampleFilterName'.")
        }

    private val jpegQualityFloat: Float
        get() {
            argError(jpegQuality in 0..100) { "JPEG quality must be between 0 and 100." }
            return jpegQuality / 100f
        }

    private val imageFormat: ImageFormat?
        get() = when (imageFormatName) {
            "default" -> null
            "jpg", "jpeg" -> ImageFormat.JPG
            "png" -> ImageFormat.PNG
            else -> argError("Invalid image format '$imageFormatName'.")
        }

    /**
     * Create a configuration per input file collection.
     * Throws [ArgumentException] if an argument value is invalid.
     */
    fun createConfigurations(): List<Configuration> {
        argError(downsampleMinSize >= 3) { "Minimum downsampling size must be at least 3 px." }
        argError(maxDpi in 10f..2000f) { "Maximum image density must be between 10 and 2000 DPI." }
        argError(rasterizationDpi in 10f..2000f) { "Rasterization density must be between 10 and 2000 DPI." }
        argError(rasterizationThreshold >= 0) { "Rasterization threshold complexity must be greater or equal to 0." }

        val outputFormat = outputFormat
        val downsampleFilter = downsampleFilter
        val jpegQuality = jpegQualityFloat
        val imageFormat = imageFormat
        return inputFileCollections.mapIndexed { i, input ->
            val tempDir = File(tempDir ?: input.first().parent)
            Configuration(
                    input,
                    outputFiles[i],
                    tempDir,
                    outputFormat.rendererFactory,
                    ocrDetectGlyphs,
                    groupFonts,
                    removeDuplicateImages,
                    downsampleImages,
                    downsampleFilter,
                    downsampleMinSize,
                    maxDpi,
                    jpegQuality,
                    imageFormat,
                    rasterizationEnabled,
                    rasterizationThreshold,
                    rasterizationDpi,
                    rasterizer,
                    rasterizerArgs)
        }
    }

}
