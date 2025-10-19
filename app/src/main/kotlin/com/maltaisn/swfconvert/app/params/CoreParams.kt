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

@file:Suppress("MagicNumber")

package com.maltaisn.swfconvert.app.params

import com.beust.jcommander.DynamicParameter
import com.beust.jcommander.Parameter
import com.maltaisn.swfconvert.app.configError
import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.core.YAxisDirection
import com.maltaisn.swfconvert.core.image.Color
import com.maltaisn.swfconvert.core.image.ImageFormat
import com.maltaisn.swfconvert.core.text.FontScale
import com.maltaisn.swfconvert.core.text.GlyphData
import com.mortennobel.imagescaling.ResampleFilter
import com.mortennobel.imagescaling.ResampleFilters
import java.awt.geom.AffineTransform
import java.io.File
import java.text.DecimalFormat
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Params for the 'convert' module and common params for the render modules.
 */
internal class CoreParams(
    private val singleFileOutput: Boolean,
    private val outputExtensionProvider: () -> String
) {

    // Files configuration

    @Parameter(
        description = "Input files or directories")
    private var input: List<String> = mutableListOf()

    @Parameter(
        names = ["-o", "--output"],
        variableArity = true,
        description = "Output files or directories.",
        splitter = NoSplitter::class,
        order = 0)
    private var output: List<String> = mutableListOf()

    @Parameter(
        names = ["-t", "--tempdir"],
        description = "Temp directory used for debugging and intermediate files.",
        order = 10)
    private var tempDir: String? = null

    /** Whether to group fonts that can be merged into a single one. */
    @Parameter(
        names = ["-e", "--ignore-empty"],
        description = "Ignore empty frames, not generating output for them.",
        order = 30)
    private var ignoreEmptyFrames: Boolean = false

    // Text & font configuration

    @Parameter(
        names = ["-g", "--dont-group-fonts"],
        description = "Disable font grouping (merging similar fonts together)",
        order = 40)
    private var dontGroupFonts: Boolean = false

    @Parameter(
        names = ["--keep-font-names"],
        description = "Whether to use original font names instead of renaming them.",
        order = 45)
    private var keepFontNames: Boolean = false

    // Images configuration

    @Parameter(
        names = ["--keep-duplicate-images"],
        description = "Keep duplicate images with same binary data.",
        order = 50)
    private var keepDuplicateImages: Boolean = false

    @Parameter(
        names = ["--downsample-images"],
        description = "Downsample big images to reduce output size.",
        order = 60)
    private var downsampleImages: Boolean = false

    @Parameter(
        names = ["--downsample-filter"],
        description = "Filter used to downsample images: fast | bell | bicubic | bicubichf | box | bspline | " +
                "hermite | lanczos3 | mitchell | triangle.",
        order = 70)
    private var downsampleFilterName: String = "lanczos3"

    @Parameter(
        names = ["--downsample-min-size"],
        description = "Minimum size in pixels that images are downsampled to or from. Must be at least 3 px.",
        order = 80)
    private var downsampleMinSize: Int = 10

    @Parameter(
        names = ["--max-dpi"],
        description = "Maximum image density in DPI.",
        order = 90)
    private var maxDpi: Float = 200f

    @Parameter(
        names = ["--jpeg-quality"],
        description = "JPEG image quality between 0 and 100.",
        order = 100)
    private var jpegQuality: Int = 75

    @Parameter(
        names = ["--image-format"],
        description = "Format to use for images: default | jpg | png",
        order = 110)
    private var imageFormatName: String = "default"

    // Other

    @DynamicParameter(
        names = ["-D"],
        description = "Additional parameters")
    var params = mutableMapOf<String, String>()

    @Parameter(
        names = ["-h", "--help"],
        description = "Show help message for this command.",
        help = true,
        order = 10000)
    var help = false

    // Convert module safe params

    private val inputFileCollections: List<List<File>> by lazy {
        configError(input.isNotEmpty()) { "No input files." }
        checkNoOptionsInArgs(input)

        val collections = mutableListOf<List<File>>()
        var onlyFiles = true
        for (filename in input) {
            val file = File(filename)
            configError(file.exists()) { "Input file '$filename' doesn't exist." }

            collections += if (file.isDirectory) {
                // File is directory, create collection for its content
                onlyFiles = false
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

        if (onlyFiles && output.size == 1 && singleFileOutput) {
            // Output file format creates a single file from multiple input files, and user has specify
            // multiple SWF files but a single output. Return a single file collection.
            listOf(collections.flatten())
        } else {
            collections
        }
    }

    private val downsampleFilter: ResampleFilter?
        get() = when (downsampleFilterName.lowercase()) {
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
            input.mapNotNull { it.first().absoluteFile.parent }
        } else {
            configError(output.size == input.size) {
                "Expected as many output files or directories as input."
            }
            output
        }
        configError(outputFilenames.isNotEmpty()) {
            "Could not find output file path"
        }

        val outputExtension = outputExtensionProvider()
        val outputFiles = mutableListOf<List<File>>()
        for ((i, filename) in outputFilenames.withIndex()) {
            val file = File(filename)
            outputFiles += if (file.name.matches("""^.+\..+$""".toRegex()) && (!file.exists() || file.isFile)) {
                // If file doesn't exist, guess whether it's a file or a directory from the filename.
                // If file already exist, this can be checked.
                configError(singleFileOutput || input[i].size == 1) {
                    "Cannot output to single file '$filename' because input has multiple files."
                }

                // Treat as a file, check extension.
                configError(file.extension.equals(outputExtension, ignoreCase = true)) {
                    "Output file '$filename' should have .$outputExtension extension."
                }

                listOf(file)

            } else {
                // Treat as directory, create it.
                file.mkdirs()
                if (singleFileOutput) {
                    // Output to single file in directory.
                    listOf(File(file, "output.$outputExtension"))
                } else {
                    // Output to one file per input file in directory.
                    input[i].map { inputFile ->
                        val name = inputFile.nameWithoutExtension
                        File(file, "$name.$outputExtension")
                    }
                }
            }
        }
        outputFiles
    }

    fun getTempDirForInput(input: List<File>) =
        File(tempDir ?: input.first().absoluteFile.parent ?: ".")

    private val parallelSwfDecoding by dynamicParam("parallelSwfDecoding", false, String::toBooleanOrNull)
    private val parallelSwfConversion by dynamicParam("parallelSwfConversion", false, String::toBooleanOrNull)
    private val parallelImageCreation by dynamicParam("parallelImageCreation", false, String::toBooleanOrNull)
    val parallelFrameRendering by dynamicParam("parallelFrameRendering", true, String::toBooleanOrNull)
    private val keepFonts by dynamicParam("keepFonts", false, String::toBooleanOrNull)
    private val keepImages by dynamicParam("keepImages", false, String::toBooleanOrNull)
    private val drawShapeBounds by dynamicParam("drawShapeBounds", false, String::toBooleanOrNull)
    private val drawTextBounds by dynamicParam("drawTextBounds", false, String::toBooleanOrNull)
    private val drawClipBounds by dynamicParam("drawClipBounds", false, String::toBooleanOrNull)
    private val disableClipping by dynamicParam("disableClipping", false, String::toBooleanOrNull)
    private val disableBlending by dynamicParam("disableBlending", false, String::toBooleanOrNull)
    private val disableMasking by dynamicParam("disableMasking", false, String::toBooleanOrNull)
    private val framePadding by dynamicParam("framePadding", 0f, String::toFloatOrNull)
    private val debugLineWidth by dynamicParam("debugLineWidth", 20f, String::toFloatOrNull)
    private val debugLineColor by dynamicParam("debugLineColor", Color.GREEN, String::toColorOrNull)
    private val fontScale2 by dynamicParam("fontScale2", DEFAULT_FONTSCALE_2, String::toFontScaleOrNull)
    private val fontScale3 by dynamicParam("fontScale3", DEFAULT_FONTSCALE_3, String::toFontScaleOrNull)
    private val frameSize by dynamicParam("frameSize", null) {
        toListOrNull(String::toFloatOrNull)?.takeIf { it.size == 2 }
    }
    private val bitmapMatrixOffset by dynamicParam(
        "bitmapMatrixOffset",
        AffineTransform(),
        String::toOffsetTransformOrNull
    )
    private val ignoreGlyphOffsetsThreshold by dynamicParam("ignoreGlyphOffsetsThreshold",
        GlyphData.EM_SQUARE_SIZE / 32f, String::toFloatOrNull)
    private val recursiveFrames by dynamicParam("recursiveFrames", false, String::toBooleanOrNull)

    private fun <T> dynamicParam(
        name: String,
        defaultValue: T,
        transform: String.() -> T?
    ): ReadOnlyProperty<CoreParams, T> =
        object : ReadOnlyProperty<CoreParams, T> {
            override fun getValue(thisRef: CoreParams, property: KProperty<*>): T {
                return thisRef.params[name]?.transform() ?: defaultValue
            }
        }

    fun createConfigurations(yAxisDirection: YAxisDirection): List<ConvertConfiguration> {
        configError(downsampleMinSize >= 3) { "Minimum downsampling size must be at least 3 px." }
        configError(maxDpi in 10f..2000f) { "Maximum image density must be between 10 and 2000 DPI." }
        configError(ignoreGlyphOffsetsThreshold >= 0) { "Ignore glyph offsets threshold must be positive." }

        return inputFileCollections.map { input ->
            val tempDir = getTempDirForInput(input)
            ConvertConfiguration(
                input,
                tempDir,
                yAxisDirection,
                ignoreEmptyFrames,
                !dontGroupFonts,
                keepFontNames,
                keepDuplicateImages,
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
                frameSize,
                bitmapMatrixOffset,
                ignoreGlyphOffsetsThreshold,
                recursiveFrames,
                debugLineWidth,
                debugLineColor)
        }
    }

    fun print() {
        println("""
            |Ignore empty frames: $ignoreEmptyFrames
            |Group fonts: ${!dontGroupFonts}
            |Keep font names: $keepFontNames
            |Keep duplicate images: $keepDuplicateImages
            |Downsample images: $downsampleImages
            """.trimMargin())
        if (downsampleImages) {
            println("""
                |  Downsample filter: ${downsampleFilterName.lowercase()}
                |  Downsample min size: ${NUMBER_FMT.format(downsampleMinSize)} px
            """.trimMargin())
        }
        println("""
            |Max DPI: ${NUMBER_FMT.format(maxDpi)}
            |JPEG quality: $jpegQuality %
            |Image format: ${imageFormatName.lowercase()}
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

    companion object {
        private val NUMBER_FMT = DecimalFormat()

        private val DEFAULT_FONTSCALE_2 = FontScale(1f, -1f, 1f, 1f)
        private val DEFAULT_FONTSCALE_3 = FontScale(0.05f, -0.05f, 1f, 1f)
    }
}

private fun String.toFontScaleOrNull(): FontScale? {
    val vals = this.toListOrNull(String::toFloatOrNull)
        ?.takeIf { it.size == 4 } ?: return null
    return FontScale(vals[0], vals[1], vals[2], vals[3])
}

private fun String.toOffsetTransformOrNull(): AffineTransform? {
    val vals = this.toListOrNull(String::toDoubleOrNull)
        ?.takeIf { it.size == 2 } ?: return null
    return AffineTransform.getTranslateInstance(vals[0], vals[1])
}
