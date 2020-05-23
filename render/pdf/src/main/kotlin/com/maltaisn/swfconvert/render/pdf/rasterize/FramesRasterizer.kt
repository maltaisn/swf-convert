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

package com.maltaisn.swfconvert.render.pdf.rasterize

import com.maltaisn.swfconvert.core.config.Configuration
import com.maltaisn.swfconvert.core.frame.data.*
import com.maltaisn.swfconvert.core.image.ImageDecoder
import com.maltaisn.swfconvert.core.image.ImageFormat
import com.maltaisn.swfconvert.core.image.data.Color
import com.maltaisn.swfconvert.core.image.data.ImageData
import com.maltaisn.swfconvert.core.shape.path.Path
import com.maltaisn.swfconvert.core.shape.path.PathElement.*
import com.maltaisn.swfconvert.core.shape.path.PathFillStyle
import com.maltaisn.swfconvert.render.pdf.PdfConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.geom.AffineTransform
import java.io.File
import java.util.concurrent.atomic.AtomicInteger


/**
 * Rasterizes frame groups to optimize their size, if needed and enabled.
 */
internal class FramesRasterizer(private val coroutineScope: CoroutineScope,
                                private val config: Configuration) {

    private val pdfConfig: PdfConfiguration
        get() = config.format as PdfConfiguration

    init {
        // Register frame rasterizers.
        FrameRasterizer.register(PdfBoxFrameRasterizer.NAME, PdfBoxFrameRasterizer(config))
        FrameRasterizer.register(null, ExternalFrameRasterizer(config))
    }


    fun rasterizeFramesIfNeeded(pdfDoc: PDDocument,
                                frameGroups: List<FrameGroup>, imagesDir: File,
                                pdfImages: MutableMap<ImageData, PDImageXObject>,
                                pdfFonts: Map<File, PDFont>): List<FrameGroup> {
        if (!pdfConfig.rasterizationEnabled) {
            return frameGroups
        }

        // Find which frames to optimize
        print("Rasterizing frames: evaluating which frames need rasterization\r")

        val framesToRasterize = mutableListOf<Int>()
        for ((i, frameGroup) in frameGroups.withIndex()) {
            val complexity = evaluateShapeComplexity(frameGroup)
            if (complexity >= pdfConfig.rasterizationThreshold) {
                // Frame is too complex, rasterize.
                framesToRasterize += i
            }
        }

        if (framesToRasterize.isEmpty()) {
            println("Rasterizing frames: found no frames to rasterize")
            return frameGroups
        }

        val newFrameGroups = frameGroups.toTypedArray()
        val progress = AtomicInteger()

        print("Rasterizing frames: rasterized frame 0 / ${framesToRasterize.size}\r")
        val jobs = framesToRasterize.map { i ->
            val job = coroutineScope.async {
                val frameGroup = frameGroups[i]
                val frameImagesDir = File(imagesDir, i.toString())

                newFrameGroups[i] = rasterizeFrame(pdfDoc, frameGroup,
                        frameImagesDir, pdfImages, pdfFonts)

                val done = progress.incrementAndGet()
                print("Rasterizing frames: rasterized frame $done / ${framesToRasterize.size}\r")
            }
            if (!pdfConfig.parallelRasterization) {
                runBlocking { job.await() }
            }
            job
        }
        if (pdfConfig.parallelRasterization) {
            runBlocking { jobs.awaitAll() }
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
    fun rasterizeFrame(pdfDoc: PDDocument,
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

        // Render the PDF to an image
        val rasterizer = FrameRasterizer.getByName(pdfConfig.rasterizer)
                ?: FrameRasterizer.getByName(null)!!
        var pdfImage = rasterizer.rasterizeFrame(croppedFrame, imagesDir, pdfImages, pdfFonts)
        if (pdfImage == null) {
            // Use PDFBox rasterizer if the other fails.
            val pdfBoxRasterizer = FrameRasterizer.getByName(PdfBoxFrameRasterizer.NAME) as PdfBoxFrameRasterizer
            pdfImage = pdfBoxRasterizer.rasterizeFrame(croppedFrame, imagesDir, pdfImages, pdfFonts)
        }

        // Create image data
        val imageDecoder = ImageDecoder(config)
        val imageData = imageDecoder.createImageData(pdfImage, null, null, ImageFormat.JPG)
        imageDecoder.dispose()

        // Create PDF image
        val imageFile = File(imagesDir, "frame.${imageData.format.extension}")
        imageFile.writeBytes(imageData.data)
        imageData.dataFile = imageFile
        pdfImages[imageData] = PDImageXObject.createFromFileByContent(imageFile, pdfDoc)

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
     * Create a rectangle [ShapeObject] with filled with an [image]
     * to be placed at the root of a [frameGroup].
     */
    private fun createRootImageObject(frameGroup: FrameGroup,
                                      imageData: ImageData): ShapeObject {
        // Create frame image shape
        val w = frameGroup.width
        val h = frameGroup.height
        val imageTransform = AffineTransform(w, 0f, 0f, h, 0f, 0f)
        return ShapeObject(0, listOf(Path(listOf(Rectangle(0f, 0f, w, h)),
                fillStyle = PathFillStyle.Image(0, imageTransform, imageData))))
    }

}
