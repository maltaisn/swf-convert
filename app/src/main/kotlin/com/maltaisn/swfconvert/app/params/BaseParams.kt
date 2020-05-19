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
import com.maltaisn.swfconvert.app.checkNoOptionsInArgs
import com.maltaisn.swfconvert.app.configError
import com.maltaisn.swfconvert.core.config.MainConfiguration
import com.maltaisn.swfconvert.core.image.ImageFormat
import com.mortennobel.imagescaling.ResampleFilter
import com.mortennobel.imagescaling.ResampleFilters
import java.io.File
import java.text.DecimalFormat


class BaseParams(private val singleFileOutput: Boolean,
                 private val outputExtension: String) {

    // Files configuration

    @Parameter(description = "Input files or directories")
    var input: List<String> = mutableListOf()

    @Parameter(names = ["-o", "--output"], variableArity = true, description = "Output files or directories.", order = 0)
    var output: List<String> = mutableListOf()

    @Parameter(names = ["-t", "--tempdir"], description = "Temp directory used for debugging and intermediate files.", order = 10)
    var tempDir: String? = null

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

    // Other

    @Parameter(names = ["-h", "--help"], description = "Show help message for this command.", help = true, order = 10000)
    var help = false


    private val inputFileCollections: List<List<File>> by lazy {
        configError(input.isNotEmpty()) { "No input files." }
        checkNoOptionsInArgs(input)

        val collections = mutableListOf<List<File>>()
        for (filename in input) {
            val file = File(filename)
            configError(file.exists()) { "Input file '$filename' doesn't exist." }

            collections += if (file.isDirectory) {
                // File is directory, create collection for its content
                val collection = file.listFiles()!!.filter { it.isSwfFile() }
                collection.sortedBy { it.nameWithoutExtension }
                configError(collection.isNotEmpty()) { "Input folder '$filename' has no SWF files." }
                collection

            } else {
                // Create collection from file.
                configError(file.isSwfFile()) { "Input file '$filename' is not a SWF file." }
                listOf(file)
            }
        }

        collections
    }

    private val outputFiles: List<File> by lazy {
        checkNoOptionsInArgs(output)

        val input = inputFileCollections
        if (output.isEmpty()) {
            // Use same directory as input collections
            input.map { it.first().parentFile }

        } else {
            configError(output.size == input.size) {
                "Expected as many output files or directories as input."
            }

            val outputFiles = mutableListOf<File>()
            for ((i, filename) in output.withIndex()) {
                val file = File(filename)
                outputFiles += file
                if (file.name.matches("""^.+\..+$""".toRegex())
                        && (!file.exists() || file.isFile)) {
                    // If file doesn't exist, guess whether it's a file or a directory from the filename.
                    // If file already exist, this can be checked.
                    configError(singleFileOutput || input[i].size == 1) {
                        "Cannot output to single file '$filename' because input has multiple files."
                    }

                    // Treat as a file, check extension.
                    configError(file.extension.equals(outputExtension, ignoreCase = true)) {
                        "Output file '$filename' should have .${outputExtension.toLowerCase()} extension."
                    }

                } else {
                    // Treat as directory, create it.
                    file.mkdirs()
                }
            }
            outputFiles
        }
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
            else -> configError("Unknown downsampling filter '$downsampleFilterName'.")
        }

    private val jpegQualityFloat: Float by lazy {
        configError(jpegQuality in 0..100) { "JPEG quality must be between 0 and 100." }
        jpegQuality / 100f
    }

    private val imageFormat: ImageFormat? by lazy {
        when (imageFormatName) {
            "default" -> null
            "jpg", "jpeg" -> ImageFormat.JPG
            "png" -> ImageFormat.PNG
            else -> configError("Invalid image format '$imageFormatName'.")
        }
    }

    fun createConfigurations(): List<MainConfiguration> {
        configError(downsampleMinSize >= 3) { "Minimum downsampling size must be at least 3 px." }
        configError(maxDpi in 10f..2000f) { "Maximum image density must be between 10 and 2000 DPI." }

        return inputFileCollections.mapIndexed { i, input ->
            val tempDir = File(tempDir ?: input.first().parent)
            MainConfiguration(
                    input,
                    outputFiles[i],
                    tempDir,
                    ocrDetectGlyphs,
                    groupFonts,
                    removeDuplicateImages,
                    downsampleImages,
                    downsampleFilter,
                    downsampleMinSize,
                    maxDpi,
                    jpegQualityFloat,
                    imageFormat)
        }
    }

    fun print() {
        println("""
            |OCR detect glyphs: $ocrDetectGlyphs
            |Group fonts: $groupFonts
            |Remove duplicate images: $removeDuplicateImages
            |Downsample images: $downsampleImages
            """.trimMargin())
        if (downsampleImages) {
            println("""
                |Downsample filter: ${downsampleFilterName.toLowerCase()}
                |Downsample min size: ${NUMBER_FMT.format(downsampleMinSize)} px
            """.trimMargin())
        }
        println("""
            |Max DPI: ${NUMBER_FMT.format(maxDpi)}
            |JPEG quality: $jpegQuality %
            |Image format: ${imageFormatName.toLowerCase()}
        """.trimMargin())
    }

    private fun File.isSwfFile() = this.extension.toLowerCase() == "swf"

    companion object {
        private val NUMBER_FMT = DecimalFormat()
    }

}
