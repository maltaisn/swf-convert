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

import com.maltaisn.swfconvert.core.FrameGroup
import com.maltaisn.swfconvert.core.FrameObject
import com.maltaisn.swfconvert.core.GroupObject
import com.maltaisn.swfconvert.core.ProgressCallback
import com.maltaisn.swfconvert.core.image.Color
import com.maltaisn.swfconvert.core.image.ImageData
import com.maltaisn.swfconvert.core.image.ImageDataCreator
import com.maltaisn.swfconvert.core.image.flippedVertically
import com.maltaisn.swfconvert.core.mapInParallel
import com.maltaisn.swfconvert.core.shape.Path
import com.maltaisn.swfconvert.core.shape.PathElement.ClosePath
import com.maltaisn.swfconvert.core.shape.PathElement.CubicTo
import com.maltaisn.swfconvert.core.shape.PathElement.LineTo
import com.maltaisn.swfconvert.core.shape.PathElement.MoveTo
import com.maltaisn.swfconvert.core.shape.PathElement.QuadTo
import com.maltaisn.swfconvert.core.shape.PathFillStyle
import com.maltaisn.swfconvert.core.shape.ShapeObject
import com.maltaisn.swfconvert.core.showProgress
import com.maltaisn.swfconvert.core.showStep
import com.maltaisn.swfconvert.core.text.TextObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.kotlin.logger
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider

/**
 * Rasterizes frame groups to optimize their size, if needed and enabled.
 */
internal class FramesRasterizer @Inject constructor(
    private val config: PdfConfiguration,
    private val progressCb: ProgressCallback,
    private val pdfFrameRendererProvider: Provider<PdfFrameRenderer>,
    private val imageDataCreatorProvider: Provider<ImageDataCreator>
) {

    private val logger = logger()

    suspend fun rasterizeFramesIfNeeded(
        pdfDoc: PDDocument,
        frameGroups: List<FrameGroup>,
        imagesDir: File,
        pdfImages: MutableMap<ImageData, PDImageXObject>,
        pdfFonts: Map<File, PDFont>
    ): List<FrameGroup> {
        if (!config.rasterizationEnabled) {
            return frameGroups
        }

        progressCb.beginStep("Rasterizing frames")

        // Find which frames to optimize
        val framesToRasterize = mutableListOf<Int>()
        progressCb.showStep("evaluating which frames need rasterization") {
            for ((i, frameGroup) in frameGroups.withIndex()) {
                val complexity = evaluateShapeComplexity(frameGroup)
                if (complexity >= config.rasterizationThreshold) {
                    // Frame is too complex, rasterize.
                    framesToRasterize += i
                }
            }
        }

        val newFrameGroups = frameGroups.toTypedArray()

        progressCb.showProgress(framesToRasterize.size) {
            framesToRasterize.mapInParallel(config.parallelRasterization) { i ->
                val frameGroup = frameGroups[i]
                val frameImagesDir = File(imagesDir, i.toString())
                val rasterizedFrame = rasterizeFrame(pdfDoc, frameGroup,
                    frameImagesDir, pdfImages, pdfFonts)
                if (rasterizedFrame != null) {
                    newFrameGroups[i] = rasterizedFrame
                }
                progressCb.incrementProgress()
            }
        }

        progressCb.endStep()
        return newFrameGroups.toList()
    }

    /**
     * Method to evaluate arbitrary shape complexity of a frame [obj].
     */
    private fun evaluateShapeComplexity(obj: FrameObject): Int = when (obj) {
        is GroupObject -> obj.objects.sumBy { evaluateShapeComplexity(it) }
        is TextObject -> {
            // Text will still be drawn no matter what so it has no influence.
            0
        }
        is ShapeObject -> obj.paths.sumBy {
            when (it.fillStyle) {
                null -> 0
                is PathFillStyle.Image -> IMAGE_COMPLEXITY
                else -> it.elements.sumBy { element ->
                    when (element) {
                        is MoveTo -> MOVE_TO_COMPLEXITY
                        is LineTo -> LINE_TO_COMPLEXITY
                        is QuadTo -> QUAD_TO_COMPLEXITY
                        is CubicTo -> CUBIC_TO_COMPLEXITY
                        is ClosePath -> CLOSE_PATH_COMPLEXITY
                    }
                }
            }
        }
        else -> error("Unknown object")
    }

    /**
     * Rasterizes a [frameGroup]. This removes all shapes in the frame and replaces
     * them with an image. Texts are made transparent to hide them but allow selection.
     * Images created during rasterization are saved to [imagesDir].
     */
    private suspend fun rasterizeFrame(
        pdfDoc: PDDocument,
        frameGroup: FrameGroup,
        imagesDir: File,
        pdfImages: MutableMap<ImageData, PDImageXObject>,
        pdfFonts: Map<File, PDFont>
    ): FrameGroup? {
        imagesDir.mkdirs()

        // Extract all text from frame.
        val textFrame = extractTextFromGroup(frameGroup) as FrameGroup

        // Remove padding on frame for rasterization.
        var croppedFrame = frameGroup
        if (frameGroup.padding != 0f) {
            croppedFrame = frameGroup.copy(padding = 0f)
            croppedFrame.objects += frameGroup.objects
        }

        // Render the PDF
        val frameDoc = PDDocument()
        val pdfFrameRenderer = pdfFrameRendererProvider.get()
        pdfFrameRenderer.renderFrame(frameDoc, croppedFrame, pdfImages, pdfFonts)

        // Render the image
        val imageData = withContext(Dispatchers.IO) {
            val pdfRenderer = PDFRenderer(frameDoc)
            var pdfImage: BufferedImage? = null
            for (i in 0 until RASTERIZATION_MAX_TRIES) {
                try {
                    pdfImage = pdfRenderer.renderImageWithDPI(
                        0, config.rasterizationDpi, ImageType.RGB)
                    break
                } catch (e: IOException) {
                    // Try again, this happens for seemingly no reason.
                    if (i == RASTERIZATION_MAX_TRIES - 1) {
                        logger.error(e) { "Failed to rasterize PDF frame after $RASTERIZATION_MAX_TRIES tries." }
                    }
                }
            }
            frameDoc.close()

            if (pdfImage == null) {
                // Rasterization failed.
                return@withContext null
            }

            // Create image data
            val imageData = imageDataCreatorProvider.get().createImageData(
                pdfImage.flippedVertically(),
                config.rasterizationFormat,
                config.rasterizationJpegQuality)

            // Create PDF image
            val imageFile = File(imagesDir, "frame.${imageData.format.extension}")
            imageFile.writeBytes(imageData.data)
            imageData.dataFile = imageFile
            pdfImages[imageData] = PDImageXObject.createFromFileByContent(imageFile, pdfDoc)

            imageData
        } ?: return null

        // Add that image add the start of the text group.
        val imageObj = createRootImageObject(textFrame, imageData)
        textFrame.objects.add(0, imageObj)

        return textFrame
    }

    /**
     * Create a copy of this group [obj] with nothing but the text objects
     * and the original group structure in it. Text is made transparent so
     * it's selectable but invisible.
     */
    private fun extractTextFromGroup(obj: GroupObject): GroupObject {
        val textGroup = obj.copyWithoutObjects()
        loop@ for (child in obj.objects) {
            textGroup.objects += when (child) {
                is GroupObject -> extractTextFromGroup(child)
                is TextObject -> child.copy(color = Color.TRANSPARENT)
                else -> continue@loop
            }
        }
        return textGroup
    }

    /**
     * Create a rectangle [ShapeObject] with filled with an [image][imageData]
     * to be placed at the root of a [frameGroup].
     */
    private fun createRootImageObject(
        frameGroup: FrameGroup,
        imageData: ImageData
    ): ShapeObject {
        // Create frame image shape
        val w = frameGroup.width
        val h = frameGroup.height
        val imageTransform = AffineTransform(w, 0f, 0f, h, 0f, 0f)
        return ShapeObject(0, listOf(Path.rectangle(0f, 0f, w, h,
            fillStyle = PathFillStyle.Image(0, imageTransform, imageData, false))))
    }

    companion object {
        private const val RASTERIZATION_MAX_TRIES = 3

        private const val IMAGE_COMPLEXITY = 0
        private const val MOVE_TO_COMPLEXITY = 2
        private const val LINE_TO_COMPLEXITY = 2
        private const val QUAD_TO_COMPLEXITY = 4
        private const val CUBIC_TO_COMPLEXITY = 6
        private const val CLOSE_PATH_COMPLEXITY = 0
    }

}
