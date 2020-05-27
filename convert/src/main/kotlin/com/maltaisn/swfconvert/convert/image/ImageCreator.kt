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
import kotlinx.coroutines.*
import java.io.File
import java.text.NumberFormat
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject


internal class ImageCreator @Inject constructor(
        private val coroutineScope: CoroutineScope,
        private val config: ConvertConfiguration
) {

    /**
     * Create all image files for a [frameGroups], written to [imagesDir].
     * If [ConvertConfiguration.removeDuplicateImages] is `true`, duplicate image
     * data is removed to optimize output size.
     */
    fun createAndOptimizeImages(frameGroups: List<FrameGroup>) {
        // Find all images in all frames
        val allImageFills = frameGroups.map { it.findAllImagesTo(mutableListOf()) }

        // Remove duplicate images
        val allImageData = mutableMapOf<ImageData, ImageData>()
        val imagesCount = AtomicInteger()

        val total = allImageFills.sumBy { it.size }
        val progress = AtomicInteger()

        val jobs = allImageFills.map { imageFills ->
            val job = coroutineScope.async {
                for (imageFill in imageFills) {
                    val data = imageFill.imageData

                    var createImage = false
                    if (!config.removeDuplicateImages) {
                        createImage = true

                    } else {
                        // Check if image was already created.
                        val existingData = allImageData[data]
                        if (existingData == null) {
                            // Image doesn't exist, create it.
                            allImageData[data] = data
                            createImage = true
                        } else {
                            // Image already exists, use existing data in fill object.
                            imageFill.imageData = existingData
                        }
                    }

                    if (createImage) {
                        val count = imagesCount.getAndIncrement()
                        withContext(Dispatchers.IO) {
                            createImageFiles(data, count.toString())
                        }
                    }

                    val done = progress.incrementAndGet()
                    print("Created image $done / $total\r")
                }
            }
            if (!config.parallelImageCreation) {
                runBlocking { job.await() }
            }
            job
        }
        if (config.parallelImageCreation) {
            runBlocking { jobs.awaitAll() }
        }

        if (config.removeDuplicateImages) {
            val count = imagesCount.get()
            print("Created images: $count from $total")
            val ratio = (total - count).toFloat() / total
            if (ratio.isFinite()) {
                print(" (-${PERCENT_FMT.format(ratio)})")
            }
            println()
        }
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
