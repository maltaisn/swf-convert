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

package com.maltaisn.swfconvert.convert.image

import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.core.FrameGroup
import com.maltaisn.swfconvert.core.ProgressCallback
import com.maltaisn.swfconvert.core.findAllImagesTo
import com.maltaisn.swfconvert.core.image.ImageData
import com.maltaisn.swfconvert.core.mapInParallel
import com.maltaisn.swfconvert.core.shape.PathFillStyle
import com.maltaisn.swfconvert.core.showProgress
import com.maltaisn.swfconvert.core.showStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

internal class ImageCreator @Inject constructor(
    private val config: ConvertConfiguration,
    private val progressCb: ProgressCallback
) {

    /**
     * Create all image files for a [frameGroups], written to images temp dir.
     * If [ConvertConfiguration.keepDuplicateImages] is `false`, duplicate image
     * data is removed to optimize output size.
     */
    suspend fun createAndOptimizeImages(frameGroups: List<FrameGroup>) {
        progressCb.beginStep("Creating images")

        // Find all images in all frames
        val allImageFills = mutableListOf<PathFillStyle.Image>()
        for (frameGroup in frameGroups) {
            frameGroup.findAllImagesTo(allImageFills)
        }

        val allImages = if (config.keepDuplicateImages) {
            // Use all images as is, keep duplicates.
            allImageFills.map { it.imageData }

        } else {
            // Remove duplicate images in all frames.
            val total = allImageFills.size
            val allImageData = mutableMapOf<ImageData, ImageData>()
            progressCb.showStep("checking for duplicates") {
                progressCb.showProgress(allImageFills.size) {
                    for (imageFill in allImageFills) {
                        // Check if image already exists.
                        val data = imageFill.imageData

                        val existingData = allImageData[data]
                        if (existingData == null) {
                            // Image doesn't exist, add it.
                            allImageData[data] = data
                        } else {
                            // Image already exists, use existing identical data in fill object.
                            imageFill.imageData = existingData
                        }

                        progressCb.incrementProgress()
                    }
                }
            }

            // Show number of duplicate images
            val removed = total - allImageData.size
            var step = "removed $removed duplicates from $total images"
            val ratio = removed.toFloat() / total
            if (ratio.isFinite()) {
                step += " (-${PERCENT_FMT.format(ratio)})"
            }
            progressCb.showStep(step) {}

            allImageData.keys
        }

        // Create image files
        progressCb.showStep("writing files") {
            progressCb.showProgress(allImages.size) {
                val idCounter = AtomicInteger()
                allImages.mapInParallel(config.parallelImageCreation) { imageData ->
                    val id = idCounter.getAndIncrement()
                    withContext(Dispatchers.IO) {
                        createImageFiles(imageData, id.toString())
                    }
                    progressCb.incrementProgress()
                }
            }
        }

        progressCb.endStep()
    }

    private fun createImageFiles(data: ImageData, name: String) {
        // Output image data file.
        val ext = data.format.extension
        val dataFile = File(config.imagesDir, "$name.$ext")
        dataFile.writeBytes(data.data)
        data.dataFile = dataFile

        if (data.alphaData.isNotEmpty()) {
            // Save image alpha data file.
            val alphaDataFile = File(config.imagesDir, "${name}_mask.$ext")
            alphaDataFile.writeBytes(data.alphaData)
            data.alphaDataFile = alphaDataFile
        }
    }

    companion object {
        private val PERCENT_FMT = NumberFormat.getPercentInstance()
    }

}
