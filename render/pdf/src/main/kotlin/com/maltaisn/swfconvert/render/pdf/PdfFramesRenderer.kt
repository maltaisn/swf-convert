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
import com.maltaisn.swfconvert.core.ProgressCallback
import com.maltaisn.swfconvert.core.image.ImageData
import com.maltaisn.swfconvert.core.mapInParallel
import com.maltaisn.swfconvert.core.shape.PathFillStyle
import com.maltaisn.swfconvert.core.shape.ShapeObject
import com.maltaisn.swfconvert.core.showProgress
import com.maltaisn.swfconvert.core.showStep
import com.maltaisn.swfconvert.core.text.TextObject
import com.maltaisn.swfconvert.render.core.FramesRenderer
import com.maltaisn.swfconvert.render.core.readAffirmativeAnswer
import com.maltaisn.swfconvert.render.pdf.metadata.PdfMetadata
import com.maltaisn.swfconvert.render.pdf.metadata.PdfOutlineCreator
import com.maltaisn.swfconvert.render.pdf.metadata.PdfPageLabelsCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.kotlin.logger
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider

/**
 * Convert all frames from the intermediate representation to output format.
 */
class PdfFramesRenderer @Inject internal constructor(
    private val config: PdfConfiguration,
    private val progressCb: ProgressCallback,
    private val pdfFrameRendererProvider: Provider<PdfFrameRenderer>,
    private val framesRasterizer: FramesRasterizer,
    private val pdfOutlineCreator: PdfOutlineCreator,
    private val pdfPageLabelsCreator: PdfPageLabelsCreator
) : FramesRenderer {

    private val logger = logger()

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
        val metadata = config.metadata
        if (metadata != null) {
            addMetadataToPdf(pdfDoc, metadata)
        }

        // Export PDF
        progressCb.showStep("Exporting PDF", true) {}
        withContext(Dispatchers.IO) {
            val output = config.output.first()
            trySave(output) {
                pdfDoc.save(output)
                pdfDoc.close()
                for (pdfPage in pdfPages) {
                    pdfPage.close()
                }
            }
        }
    }

    private fun createPdfImages(
        pdfDoc: PDDocument,
        frameGroups: List<FrameGroup>
    ): MutableMap<ImageData, PDImageXObject> {
        progressCb.beginStep("Creating PDF images", true)

        // Find all images in all frames.
        val allImageData = mutableSetOf<ImageData>()
        progressCb.showStep("finding all images", false) {
            for (frameGroup in frameGroups) {
                frameGroup.findAllImageDataTo(allImageData)
            }
        }

        // Create PDF image for each image.
        val pdfImagesMap = ConcurrentHashMap<ImageData, PDImageXObject>()
        progressCb.showStep("creating images", false) {
            progressCb.showProgress(allImageData.size) {
                for (imageData in allImageData) {
                    pdfImagesMap[imageData] = createPdfImage(pdfDoc, imageData)
                    progressCb.incrementProgress()
                }
            }
        }

        progressCb.endStep()
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
        progressCb.beginStep("Creating PDF fonts", true)

        // Find all fonts in all frames.
        val allFonts = mutableSetOf<File>()
        progressCb.showStep("finding all fonts", false) {
            for (frameGroup in frameGroups) {
                frameGroup.findAllFontFilesTo(allFonts)
            }
        }

        // Create PDF image for each image.
        val pdfFontsMap = mutableMapOf<File, PDFont>()
        progressCb.showStep("creating fonts", false) {
            progressCb.showProgress(allFonts.size) {
                for (file in allFonts) {
                    // Load file as PDF font. Don't subset since it's already been done. Also, subsetting
                    // produces garbage text: https://stackoverflow.com/q/47699665/5288316
                    pdfFontsMap[file] = PDType0Font.load(pdfDoc, file.inputStream(), false)
                    progressCb.incrementProgress()
                }
            }
        }

        progressCb.endStep()
        return pdfFontsMap
    }

    /** Find all image data recursively in children of this group, adding them to the [destination] collection. */
    private fun <C : MutableCollection<ImageData>> FrameObject.findAllImageDataTo(destination: C): C {
        when (this) {
            is ShapeObject -> {
                for (path in paths) {
                    if (path.fillStyle is PathFillStyle.Image) {
                        destination += (path.fillStyle as PathFillStyle.Image).imageData
                    }
                }
            }
            is GroupObject -> {
                for (obj in this.objects) {
                    obj.findAllImageDataTo(destination)
                }
            }
        }
        return destination
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

    private suspend fun renderFramesToPdf(
        frameGroups: List<FrameGroup>,
        pdfImages: Map<ImageData, PDImageXObject>,
        pdfFonts: Map<File, PDFont>
    ): List<PDDocument> {
        return progressCb.showStep("Rendering PDF frames", true) {
            progressCb.showProgress(frameGroups.size) {
                frameGroups.mapInParallel(config.parallelFrameRendering) { frameGroup ->
                    val pdfPage = PDDocument(MemoryUsageSetting.setupTempFileOnly())
                    val renderer = pdfFrameRendererProvider.get()
                    renderer.renderFrame(pdfPage, frameGroup, pdfImages, pdfFonts)

                    progressCb.incrementProgress()
                    pdfPage
                }
            }
        }
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

    /**
     * Try to save a [file] by executing [save] block.
     * If [IOException] is thrown, ask user to retry in console.
     */
    private inline fun trySave(file: File, save: () -> Unit) {
        while (true) {
            try {
                save()
                return
            } catch (e: IOException) {
                logger.warn { "Failed to save file to $file" }
                if (readAffirmativeAnswer("Could not save file '${file.path}'.")) {
                    continue
                } else {
                    return
                }
            }
        }
    }
}
