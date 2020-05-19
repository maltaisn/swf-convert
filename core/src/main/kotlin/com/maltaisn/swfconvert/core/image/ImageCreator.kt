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

package com.maltaisn.swfconvert.core.image

import com.maltaisn.swfconvert.core.config.Configuration
import com.maltaisn.swfconvert.core.config.MainConfiguration
import com.maltaisn.swfconvert.core.frame.data.FrameGroup
import com.maltaisn.swfconvert.core.frame.data.GroupObject
import com.maltaisn.swfconvert.core.frame.data.ShapeObject
import com.maltaisn.swfconvert.core.image.data.ImageData
import com.maltaisn.swfconvert.core.shape.path.PathFillStyle
import kotlinx.coroutines.*
import java.io.File
import java.text.NumberFormat
import java.util.concurrent.atomic.AtomicInteger


class ImageCreator(private val coroutineScope: CoroutineScope,
                   private val config: Configuration) {

    /**
     * Create image files for a [frameGroup], written to [imagesDir].
     * All images are created, no optimization is done.
     */
    fun createImages(frameGroup: FrameGroup, imagesDir: File) {
        imagesDir.mkdirs()

        // Get all images in frame
        val imageFills = mutableListOf<PathFillStyle.Image>()
        findAllImageFillsInGroup(frameGroup, imageFills)

        // Create image files
        for ((i, imageFill) in imageFills.withIndex()) {
            createImageFiles(imageFill.imageData, i.toString(), imagesDir)
        }
    }

    /**
     * Create all image files for a [frameGroups], written to [imagesDir].
     * If [MainConfiguration.removeDuplicateImages] is `true`, duplicate image
     * data is removed to optimize output size.
     */
    fun createAndOptimizeImages(frameGroups: List<FrameGroup>, imagesDir: File) {
        imagesDir.mkdirs()

        // Find all images in all frames
        val allImageFills = findAllImageFillsInFrames(frameGroups)

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
                    if (!config.main.removeDuplicateImages) {
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
                            createImageFiles(data, count.toString(), imagesDir)
                        }
                    }

                    val done = progress.incrementAndGet()
                    print("Created image $done / $total\r")
                }
            }
            if (!config.main.parallelImageCreation) {
                runBlocking { job.await() }
            }
            job
        }
        if (config.main.parallelImageCreation) {
            runBlocking { jobs.awaitAll() }
        }

        if (config.main.removeDuplicateImages) {
            val count = imagesCount.get()
            print("Created images: $count from $total")
            val ratio = (total - count).toFloat() / total
            if (ratio.isFinite()) {
                print(" (-${PERCENT_FMT.format(ratio)})")
            }
            println()
        }
    }

    private fun findAllImageFillsInFrames(frameGroups: List<FrameGroup>): List<List<PathFillStyle.Image>> {
        val allImages = mutableListOf<List<PathFillStyle.Image>>()
        for (frameGroup in frameGroups) {
            val images = mutableListOf<PathFillStyle.Image>()
            findAllImageFillsInGroup(frameGroup, images)
            allImages += images
        }
        return allImages
    }

    private fun findAllImageFillsInGroup(group: GroupObject,
                                         images: MutableList<PathFillStyle.Image>) {
        for (obj in group.objects) {
            if (obj is ShapeObject) {
                for (path in obj.paths) {
                    if (path.fillStyle is PathFillStyle.Image) {
                        images += path.fillStyle
                    }
                }
            } else if (obj is GroupObject) {
                findAllImageFillsInGroup(obj, images)
            }
        }
    }

    private fun createImageFiles(data: ImageData, name: String, imagesDir: File) {
        // Output image data file.
        val ext = data.format.extension
        val dataFile = File(imagesDir, "$name.$ext")
        dataFile.writeBytes(data.data)
        data.dataFile = dataFile

        if (data.alphaData.isNotEmpty()) {
            // Save image alpha data file.
            val alphaDataFile = File(imagesDir, "${name}_mask.$ext")
            alphaDataFile.writeBytes(data.alphaData)
            data.alphaDataFile = alphaDataFile
        }
    }

    companion object {
        private val PERCENT_FMT = NumberFormat.getPercentInstance()
    }

}
