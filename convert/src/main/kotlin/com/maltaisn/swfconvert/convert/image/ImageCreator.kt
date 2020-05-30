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

package com.maltaisn.swfconvert.convert.image

import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.core.FrameGroup
import com.maltaisn.swfconvert.core.image.ImageData
import com.maltaisn.swfconvert.core.mapInParallel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject


internal class ImageCreator @Inject constructor(
        private val config: ConvertConfiguration
) {

    /**
     * Create all image files for a [frameGroups], written to images temp dir.
     * If [ConvertConfiguration.removeDuplicateImages] is `true`, duplicate image
     * data is removed to optimize output size.
     */
    suspend fun createAndOptimizeImages(frameGroups: List<FrameGroup>) {
        // Find all images in all frames
        val allImageFills = frameGroups.flatMap { it.findAllImagesTo(mutableListOf()) }

        val allImages = if (config.removeDuplicateImages) {
            // Remove duplicate images in all frames.
            val total = allImageFills.size
            val allImageData = mutableMapOf<ImageData, ImageData>()
            print("Creating images: checking for duplicates 0 / $total\r")
            for ((i, imageFill) in allImageFills.withIndex()) {
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

                print("Creating images: checking for duplicates ${i + 1} / $total\r")
            }
            println()

            // Show number of duplicate images
            val removed = total - allImageData.size
            print("Creating images: removed $removed duplicates from $total images")
            val ratio = removed.toFloat() / total
            if (ratio.isFinite()) {
                print(" (-${PERCENT_FMT.format(ratio)})")
            }
            println()

            allImageData.keys

        } else {
            // Use all images as is, keep duplicates.
            allImageFills.map { it.imageData }
        }

        // Create image files
        print("Creating images: created file 0 / ${allImages.size}\r")
        val progress = AtomicInteger()
        val idCounter = AtomicInteger()
        allImages.mapInParallel(config.parallelImageCreation) { imageData ->
            val id = idCounter.getAndIncrement()
            withContext(Dispatchers.IO) {
                createImageFiles(imageData, id.toString())
            }
            print("Creating images: created file ${progress.incrementAndGet()} / ${allImages.size}\r")
        }
        println()
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
