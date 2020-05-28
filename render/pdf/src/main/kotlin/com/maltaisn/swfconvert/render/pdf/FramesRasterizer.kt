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

import com.maltaisn.swfconvert.core.FrameGroup
import com.maltaisn.swfconvert.core.FrameObject
import com.maltaisn.swfconvert.core.GroupObject
import com.maltaisn.swfconvert.core.image.Color
import com.maltaisn.swfconvert.core.image.ImageData
import com.maltaisn.swfconvert.core.image.ImageDataCreator
import com.maltaisn.swfconvert.core.mapInParallel
import com.maltaisn.swfconvert.core.shape.Path
import com.maltaisn.swfconvert.core.shape.PathElement.*
import com.maltaisn.swfconvert.core.shape.PathFillStyle
import com.maltaisn.swfconvert.core.shape.ShapeObject
import com.maltaisn.swfconvert.core.text.TextObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.geom.AffineTransform
import java.io.File
import javax.inject.Inject
import javax.inject.Provider


/**
 * Rasterizes frame groups to optimize their size, if needed and enabled.
 */
internal class FramesRasterizer @Inject constructor(
        private val config: PdfConfiguration,
        private val pdfFrameRendererProvider: Provider<PdfFrameRenderer>,
        private val imageDataCreatorProvider: Provider<ImageDataCreator>
) {

    suspend fun rasterizeFramesIfNeeded(pdfDoc: PDDocument,
                                frameGroups: List<FrameGroup>, imagesDir: File,
                                pdfImages: MutableMap<ImageData, PDImageXObject>,
                                pdfFonts: Map<File, PDFont>): List<FrameGroup> {
        if (!config.rasterizationEnabled) {
            return frameGroups
        }

        // Find which frames to optimize
        print("Rasterizing frames: evaluating which frames need rasterization\r")

        val framesToRasterize = mutableListOf<Int>()
        for ((i, frameGroup) in frameGroups.withIndex()) {
            val complexity = evaluateShapeComplexity(frameGroup)
            if (complexity >= config.rasterizationThreshold) {
                // Frame is too complex, rasterize.
                framesToRasterize += i
            }
        }

        if (framesToRasterize.isEmpty()) {
            println("Rasterizing frames: found no frames to rasterize")
            return frameGroups
        }

        val newFrameGroups = frameGroups.toTypedArray()

        print("Rasterizing frames: rasterized frame 0 / ${framesToRasterize.size}\r")
        framesToRasterize.mapInParallel(config.parallelRasterization) { i, progress ->
            val frameGroup = frameGroups[i]
            val frameImagesDir = File(imagesDir, i.toString())

            newFrameGroups[i] = rasterizeFrame(pdfDoc, frameGroup,
                    frameImagesDir, pdfImages, pdfFonts)

            print("Rasterizing frames: rasterized frame $progress / ${framesToRasterize.size}\r")
        }
        println()

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
                is PathFillStyle.Image -> 0
                else -> it.elements.sumBy { element ->
                    when (element) {
                        is MoveTo -> 2
                        is LineTo -> 2
                        is QuadTo -> 4
                        is CubicTo -> 6
                        is Rectangle -> 4
                        is ClosePath -> 0
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
    private suspend fun rasterizeFrame(pdfDoc: PDDocument,
                               frameGroup: FrameGroup, imagesDir: File,
                               pdfImages: MutableMap<ImageData, PDImageXObject>,
                               pdfFonts: Map<File, PDFont>): FrameGroup {
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
            val pdfImage = pdfRenderer.renderImageWithDPI(
                    0, config.rasterizationDpi, ImageType.RGB)
            frameDoc.close()

            // Create image data
            val imageData = imageDataCreatorProvider.get().createImageData(pdfImage,
                    config.rasterizationFormat, config.rasterizationJpegQuality)

            // Create PDF image
            val imageFile = File(imagesDir, "frame.${imageData.format.extension}")
            imageFile.writeBytes(imageData.data)
            imageData.dataFile = imageFile
            pdfImages[imageData] = PDImageXObject.createFromFileByContent(imageFile, pdfDoc)

            imageData
        }

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
    private fun createRootImageObject(frameGroup: FrameGroup,
                                      imageData: ImageData): ShapeObject {
        // Create frame image shape
        val w = frameGroup.width
        val h = frameGroup.height
        val imageTransform = AffineTransform(w, 0f, 0f, h, 0f, 0f)
        return ShapeObject(0, listOf(Path(listOf(Rectangle(0f, 0f, w, h)),
                fillStyle = PathFillStyle.Image(0, imageTransform, imageData, false))))
    }

}
