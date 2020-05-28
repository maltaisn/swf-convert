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
import com.maltaisn.swfconvert.core.GroupObject
import com.maltaisn.swfconvert.core.image.ImageData
import com.maltaisn.swfconvert.core.mapInParallel
import com.maltaisn.swfconvert.core.shape.PathFillStyle
import com.maltaisn.swfconvert.core.text.TextObject
import com.maltaisn.swfconvert.render.core.FramesRenderer
import com.maltaisn.swfconvert.render.pdf.metadata.PdfMetadata
import com.maltaisn.swfconvert.render.pdf.metadata.PdfOutlineCreator
import com.maltaisn.swfconvert.render.pdf.metadata.PdfPageLabelsCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider


/**
 * Convert all frames from the intermediate representation to output format.
 */
class PdfFramesRenderer @Inject internal constructor(
        private val config: PdfConfiguration,
        private val pdfFrameRendererProvider: Provider<PdfFrameRenderer>,
        private val framesRasterizer: FramesRasterizer,
        private val pdfOutlineCreator: PdfOutlineCreator,
        private val pdfPageLabelsCreator: PdfPageLabelsCreator
) : FramesRenderer {

    override suspend fun renderFrames(frameGroups: List<FrameGroup>) {
        var currFrameGroups = frameGroups

        // Create PDF document using only temp files as memory buffer.
        // Using any amount of RAM instead almost always results in OutOfMemory errors.
        val pdfDoc = PDDocument(MemoryUsageSetting.setupTempFileOnly())

        // Create PDF images and fonts
        val pdfImages = withContext(Dispatchers.IO) {
            createPdfImages(pdfDoc, currFrameGroups)
        }
        val pdfFonts = withContext(Dispatchers.IO) {
            createPdfFonts(pdfDoc, currFrameGroups)
        }

        // Rasterize pages
        val imagesDir = File(config.tempDir, "images")
        currFrameGroups = framesRasterizer.rasterizeFramesIfNeeded(
                pdfDoc, currFrameGroups, imagesDir, pdfImages, pdfFonts)

        // Render all frames to PDF
        val pdfPages = renderFramesToPdf(currFrameGroups, pdfImages, pdfFonts)

        // Append all pages to document
        for (pdfPage in pdfPages) {
            pdfDoc.addPage(pdfPage.getPage(0))
        }

        // Add metadata
        println("Exporting PDF")
        val metadata = config.metadata
        if (metadata != null) {
            addMetadataToPdf(pdfDoc, metadata)
        }

        // Export PDF
        // TODO handle errors
        withContext(Dispatchers.IO) {
            pdfDoc.save(config.output.first())
            pdfDoc.close()
            for (pdfPage in pdfPages) {
                pdfPage.close()
            }
        }

        println()
    }

    private fun createPdfImages(pdfDoc: PDDocument,
                                frameGroups: List<FrameGroup>): MutableMap<ImageData, PDImageXObject> {
        // Find all images in all frames.
        print("Creating PDF images: finding all images")
        val allImages = mutableSetOf<PathFillStyle.Image>()
        for (frameGroup in frameGroups) {
            frameGroup.findAllImagesTo(allImages)
        }

        // Create PDF image for each image.
        print("Creating PDF images: created image 0 / ${allImages.size}\r")
        val pdfImagesMap = ConcurrentHashMap<ImageData, PDImageXObject>()
        for ((i, image) in allImages.withIndex()) {
            val data = image.imageData
            pdfImagesMap[data] = createPdfImage(pdfDoc, data)
            print("Creating PDF images: created image ${i + 1} / ${allImages.size}\r")
        }

        println()
        return pdfImagesMap
    }

    private fun createPdfImage(pdfDoc: PDDocument, data: ImageData): PDImageXObject {
        val pdfImage = PDImageXObject.createFromFileByExtension(data.dataFile!!, pdfDoc)

        val alphaDataFile = data.alphaDataFile
        if (alphaDataFile != null) {
            // Add soft mask to image to create alpha channel.
            val alphaPdfImage = PDImageXObject.createFromFileByExtension(alphaDataFile, pdfDoc)
            pdfImage.cosObject.setItem(COSName.SMASK, alphaPdfImage)
        }
        return pdfImage
    }

    private fun createPdfFonts(pdfDoc: PDDocument, frameGroups: List<FrameGroup>): Map<File, PDFont> {
        print("Creating PDF fonts: finding all fonts")
        val allFonts = mutableSetOf<File>()
        for (frameGroup in frameGroups) {
            frameGroup.findAllFontFilesTo(allFonts)
        }

        // Create PDF image for each image.
        print("Creating PDF fonts: created font 0 / ${allFonts.size}\r")
        val pdfFontsMap = mutableMapOf<File, PDFont>()
        for ((i, file) in allFonts.withIndex()) {
            // Load file as PDF font. Don't subset since it's already been done. Also, subsetting
            // produces garbage text: https://stackoverflow.com/q/47699665/5288316
            pdfFontsMap[file] = PDType0Font.load(pdfDoc, file.inputStream(), false)
            print("Creating PDF fonts: created font ${i + 1} / ${allFonts.size}\r")
        }

        println()
        return pdfFontsMap
    }

    /** Find all fonts recursively in children of this group, adding them to the [destination] collection. */
    private fun <C : MutableCollection<File>> GroupObject.findAllFontFilesTo(destination: C): C {
        for (obj in this.objects) {
            if (obj is TextObject) {
                destination += obj.font.fontFile!!
            } else if (obj is GroupObject) {
                obj.findAllFontFilesTo(destination)
            }
        }
        return destination
    }

    private suspend fun renderFramesToPdf(frameGroups: List<FrameGroup>,
                                          pdfImages: Map<ImageData, PDImageXObject>,
                                          pdfFonts: Map<File, PDFont>): List<PDDocument> {
        print("Rendered frame 0 / ${frameGroups.size}\r")
        val pdfPages = frameGroups.mapInParallel(config.parallelFrameRendering) { frameGroup, progress ->
            val pdfPage = PDDocument(MemoryUsageSetting.setupTempFileOnly())
            val renderer = pdfFrameRendererProvider.get()
            renderer.renderFrame(pdfPage, frameGroup, pdfImages, pdfFonts)

            print("Rendered frame $progress / ${frameGroups.size}\r")
            pdfPage
        }
        println()
        return pdfPages
    }

    private fun addMetadataToPdf(pdfDoc: PDDocument, metadata: PdfMetadata) {
        pdfOutlineCreator.createOutline(pdfDoc, metadata.outline, metadata.outlineOpenLevel)

        val pageLabels = metadata.pageLabels
        if (pageLabels != null) {
            pdfPageLabelsCreator.createPageLabels(pdfDoc, metadata.pageLabels)
        }

        val pdfInfo = pdfDoc.documentInformation
        for ((key, value) in metadata.metadata) {
            pdfInfo.setCustomMetadataValue(key, value)
        }
    }

}
