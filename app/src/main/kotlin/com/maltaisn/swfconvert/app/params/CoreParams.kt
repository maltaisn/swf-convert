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

import com.beust.jcommander.DynamicParameter
import com.beust.jcommander.Parameter
import com.maltaisn.swfconvert.app.*
import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.core.YAxisDirection
import com.maltaisn.swfconvert.core.image.Color
import com.maltaisn.swfconvert.core.image.ImageFormat
import com.maltaisn.swfconvert.core.text.FontScale
import com.mortennobel.imagescaling.ResampleFilter
import com.mortennobel.imagescaling.ResampleFilters
import java.io.File
import java.text.DecimalFormat


/**
 * Params for the 'convert' module and common params for the render modules.
 */
class CoreParams(private val singleFileOutput: Boolean,
                 private val outputExtensionProvider: () -> String) {

    // Files configuration

    @Parameter(description = "Input files or directories")
    private var input: List<String> = mutableListOf()

    @Parameter(names = ["-o", "--output"], variableArity = true, description = "Output files or directories.", order = 0)
    private var output: List<String> = mutableListOf()

    @Parameter(names = ["-t", "--tempdir"], description = "Temp directory used for debugging and intermediate files.", order = 10)
    private var tempDir: String? = null

    // Text & font configuration

    /** Whether to group fonts that can be merged into a single one. */
    @Parameter(names = ["--group-fonts"], arity = 1, description = "Whether to group fonts that can be merged into a single one.", order = 40)
    private var groupFonts: Boolean = true

    // Images configuration

    @Parameter(names = ["--remove-duplicate-images"], arity = 1, description = "Whether to use the same image for all images with the same binary data.", order = 50)
    private var removeDuplicateImages: Boolean = true

    @Parameter(names = ["--downsample-images"], description = "Whether to downsample big images to reduce output size.", order = 60)
    private var downsampleImages: Boolean = false

    @Parameter(names = ["--downsample-filter"], description = "Filter used to downsample images: fast | bell | bicubic | bicubichf | box | bspline | hermite | lanczos3 | mitchell | triangle.", order = 70)
    private var downsampleFilterName: String = "lanczos3"

    @Parameter(names = ["--downsample-min-size"], description = "Minimum size in pixels that images are downsampled to or from. Must be at least 3 px.", order = 80)
    private var downsampleMinSize: Int = 10

    /** If downsampling images, the maximum allowed image density. */
    @Parameter(names = ["--max-dpi"], description = "Maximum image density in DPI.", order = 90)
    private var maxDpi: Float = 200f

    @Parameter(names = ["--jpeg-quality"], description = "JPEG image quality between 0 and 100.", order = 100)
    private var jpegQuality: Int = 75

    @Parameter(names = ["--image-format"], description = "Format to use for images: default | jpg | png", order = 110)
    private var imageFormatName: String = "default"

    // Other

    @DynamicParameter(names = ["-D"], description = "Additional parameters")
    var params = mutableMapOf<String, String>()

    @Parameter(names = ["-h", "--help"], description = "Show help message for this command.", help = true, order = 10000)
    var help = false


    // Convert module safe params

    private val inputFileCollections: List<List<File>> by lazy {
        configError(input.isNotEmpty()) { "No input files." }
        checkNoOptionsInArgs(input)

        val collections = mutableListOf<List<File>>()
        for (filename in input) {
            val file = File(filename)
            configError(file.exists()) { "Input file '$filename' doesn't exist." }

            collections += if (file.isDirectory) {
                // File is directory, create collection for its content
                val collection = file.listFiles()!!.filterTo(mutableListOf()) { it.isSwfFile() }
                collection.sortByFileName()
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

    // Render core safe params

    val outputFiles: List<List<File>> by lazy {
        checkNoOptionsInArgs(output)

        val input = inputFileCollections
        val outputFilenames = if (output.isEmpty()) {
            // Use same directory as input collections
            input.map { it.first().parent }
        } else {
            configError(output.size == input.size) {
                "Expected as many output files or directories as input."
            }
            output
        }

        val outputExtension = outputExtensionProvider()
        val outputFiles = mutableListOf<List<File>>()
        for ((i, filename) in outputFilenames.withIndex()) {
            val file = File(filename)
            if (file.name.matches("""^.+\..+$""".toRegex()) && (!file.exists() || file.isFile)) {
                // If file doesn't exist, guess whether it's a file or a directory from the filename.
                // If file already exist, this can be checked.
                configError(singleFileOutput || input[i].size == 1) {
                    "Cannot output to single file '$filename' because input has multiple files."
                }

                // Treat as a file, check extension.
                configError(file.extension.equals(outputExtension, ignoreCase = true)) {
                    "Output file '$filename' should have .$outputExtension extension."
                }

                outputFiles += listOf(file)

            } else {
                // Treat as directory, create it.
                file.mkdirs()
                if (singleFileOutput) {
                    // Output to single file in directory.
                    outputFiles += listOf(File(file, "output.$outputExtension"))
                } else {
                    // Output to one file per input file in directory.
                    outputFiles += input[i].map { inputFile ->
                        val name = inputFile.nameWithoutExtension
                        File(file, "$name.$outputExtension")
                    }
                }
            }
        }
        outputFiles
    }

    val parallelFrameRendering by lazy {
        params[OPT_PARALLEL_FRAME_RENDERING]?.toBooleanOrNull() ?: true
    }

    fun getTempDirForInput(input: List<File>) =
            File(tempDir ?: input.first().parent)


    fun createConfigurations(yAxisDirection: YAxisDirection): List<ConvertConfiguration> {
        configError(downsampleMinSize >= 3) { "Minimum downsampling size must be at least 3 px." }
        configError(maxDpi in 10f..2000f) { "Maximum image density must be between 10 and 2000 DPI." }

        // Additional options
        val parallelSwfDecoding = params[OPT_PARALLEL_SWF_DECODING]?.toBooleanOrNull() ?: true
        val parallelSwfConversion = params[OPT_PARALLEL_SWF_CONVERSION]?.toBooleanOrNull() ?: true
        val parallelImageCreation = params[OPT_PARALLEL_IMAGE_CREATION]?.toBooleanOrNull() ?: true
        val keepFonts = params[OPT_KEEP_FONTS]?.toBooleanOrNull() ?: false
        val keepImages = params[OPT_KEEP_IMAGES]?.toBooleanOrNull() ?: false
        val drawShapeBounds = params[OPT_DRAW_SHAPE_BOUNDS]?.toBooleanOrNull() ?: false
        val drawTextBounds = params[OPT_DRAW_TEXT_BOUNDS]?.toBooleanOrNull() ?: false
        val drawClipBounds = params[OPT_DRAW_CLIP_BOUNDS]?.toBooleanOrNull() ?: false
        val disableClipping = params[OPT_DISABLE_CLIPPING]?.toBooleanOrNull() ?: false
        val disableBlending = params[OPT_DISABLE_BLENDING]?.toBooleanOrNull() ?: false
        val disableMasking = params[OPT_DISABLE_MASKING]?.toBooleanOrNull() ?: false
        val framePadding = params[OPT_FRAME_PADDING]?.toFloatOrNull() ?: 0f
        val debugLineWidth = params[OPT_DEBUG_LINE_WIDTH]?.toFloatOrNull() ?: 20f
        val debugLineColor = params[OPT_DEBUG_LINE_COLOR]?.toColorOrNull() ?: Color(0, 255, 0)
        val fontScale2 = params[OPT_FONT_SCALE_2]?.toFontScaleOrNull()
                ?: FontScale(1f, -1f, 1f, 1f)
        val fontScale3 = params[OPT_FONT_SCALE_3]?.toFontScaleOrNull()
                ?: FontScale(0.05f, -0.05f, 1f, 1f)
        val ignoreGlyphOffsetsThreshold = params[OPT_IGNORE_GLYPH_OFFSETS_THRESHOLD]?.toFloatOrNull() ?: 5f

        return inputFileCollections.map { input ->
            val tempDir = getTempDirForInput(input)
            ConvertConfiguration(
                    input,
                    tempDir,
                    yAxisDirection,
                    groupFonts,
                    removeDuplicateImages,
                    downsampleImages,
                    downsampleFilter,
                    downsampleMinSize,
                    maxDpi,
                    jpegQualityFloat,
                    imageFormat,
                    parallelSwfDecoding,
                    parallelSwfConversion,
                    parallelImageCreation,
                    keepFonts,
                    keepImages,
                    drawShapeBounds,
                    drawTextBounds,
                    drawClipBounds,
                    disableClipping,
                    disableBlending,
                    disableMasking,
                    framePadding,
                    fontScale2,
                    fontScale3,
                    ignoreGlyphOffsetsThreshold,
                    debugLineWidth,
                    debugLineColor)
        }
    }

    fun print() {
        println("""
            |Group fonts: $groupFonts
            |Remove duplicate images: $removeDuplicateImages
            |Downsample images: $downsampleImages
            """.trimMargin())
        if (downsampleImages) {
            println("""
                |  Downsample filter: ${downsampleFilterName.toLowerCase()}
                |  Downsample min size: ${NUMBER_FMT.format(downsampleMinSize)} px
            """.trimMargin())
        }
        println("""
            |Max DPI: ${NUMBER_FMT.format(maxDpi)}
            |JPEG quality: $jpegQuality %
            |Image format: ${imageFormatName.toLowerCase()}
        """.trimMargin())
    }

    /**
     * Sort alphabetically if name is alphanumeric, and sort by
     * number if all files have numeric names.
     */
    private fun MutableList<File>.sortByFileName() {
        if (this.all { it.nameWithoutExtension.toIntOrNull() != null }) {
            // All file names are numeric, sort by number
            this.sortBy { it.nameWithoutExtension.toInt() }
        } else {
            this.sortBy { it.name }
        }
    }

    private fun String.toFontScaleOrNull(): FontScale? {
        val vals = this.toListOrNull(String::toFloatOrNull)
                ?.takeIf { it.size == 4 } ?: return null
        return FontScale(vals[0], vals[1], vals[2], vals[3])
    }

    companion object {
        private val NUMBER_FMT = DecimalFormat()

        const val OPT_PARALLEL_SWF_DECODING = "parallelSwfDecoding"
        const val OPT_PARALLEL_SWF_CONVERSION = "parallelSwfConversion"
        const val OPT_PARALLEL_FRAME_RENDERING = "parallelFrameRendering"
        const val OPT_PARALLEL_IMAGE_CREATION = "parallelImageCreation"
        const val OPT_KEEP_FONTS = "keepFonts"
        const val OPT_KEEP_IMAGES = "keepImages"
        const val OPT_DRAW_SHAPE_BOUNDS = "drawShapeBounds"
        const val OPT_DRAW_TEXT_BOUNDS = "drawTextBounds"
        const val OPT_DRAW_CLIP_BOUNDS = "drawClipBounds"
        const val OPT_DISABLE_CLIPPING = "disableClipping"
        const val OPT_DISABLE_BLENDING = "disableBlending"
        const val OPT_DISABLE_MASKING = "disableMasking"
        const val OPT_FRAME_PADDING = "framePadding"
        const val OPT_FONT_SCALE_2 = "fontScale2"
        const val OPT_FONT_SCALE_3 = "fontScale3"
        const val OPT_IGNORE_GLYPH_OFFSETS_THRESHOLD = "ignoreGlyphOffsetsThreshold"
        const val OPT_DEBUG_LINE_WIDTH = "debugLineWidth"
        const val OPT_DEBUG_LINE_COLOR = "debugLineColor"
    }

}
