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

package com.maltaisn.swfconvert.render.svg

import com.maltaisn.swfconvert.core.FrameGroup
import com.maltaisn.swfconvert.core.ProgressCallback
import com.maltaisn.swfconvert.core.mapInParallel
import com.maltaisn.swfconvert.core.showProgress
import com.maltaisn.swfconvert.core.showStep
import com.maltaisn.swfconvert.render.core.FrameKey
import com.maltaisn.swfconvert.render.core.FramesMap
import com.maltaisn.swfconvert.render.core.FramesRenderer
import com.maltaisn.swfconvert.render.core.readAffirmativeAnswer
import com.maltaisn.swfconvert.render.core.toFramesMap
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider

/**
 * Convert all frames from the intermediate representation to SVG.
 */
class SvgFramesRenderer @Inject internal constructor(
    private val config: SvgConfiguration,
    private val progressCb: ProgressCallback,
    private val svgFrameRendererProvider: Provider<SvgFrameRenderer>
) : FramesRenderer {

    private val logger = logger()

    override suspend fun renderFrames(frameGroups: List<List<FrameGroup>>) {
        // Move images and fonts from temp dir to output dir
        val outputDir = config.output.first().parentFile
        val outputImagesDir = File(outputDir, "images")
        val outputFontsDir = File(outputDir, "fonts")
        progressCb.showStep("Copying images and fonts to output", false) {

            val tempImagesDir = File(config.tempDir, "images")
            tempImagesDir.copyRecursively(outputImagesDir, true)

            val tempFontsDir = File(config.tempDir, "fonts")
            tempFontsDir.copyRecursively(outputFontsDir, true)
        }

        // Write frames
        var framesMap = frameGroups.toFramesMap()
        while (true) {
            framesMap = renderFrames(framesMap, outputImagesDir, outputFontsDir)

            if (framesMap.isNotEmpty()) {
                // Some files couldn't be saved. Ask to retry.
                if (readAffirmativeAnswer("Could not save ${framesMap.size} files.")) {
                    continue
                } else {
                    return
                }
            } else {
                return
            }
        }
    }

    /**
     * Render [framesMap], a map of frame by file index.
     * Returns a similar map for frames that couldn't be saved.
     */
    private suspend fun renderFrames(
        framesMap: FramesMap,
        imagesDir: File,
        fontsDir: File
    ): FramesMap {
        return progressCb.showStep("Writing SVG frames", true) {
            progressCb.showProgress(framesMap.size) {
                val failed = ConcurrentHashMap<FrameKey, FrameGroup>()

                framesMap.entries.mapInParallel(config.parallelFrameRendering) { (key, frameGroup) ->
                    val renderer = svgFrameRendererProvider.get()

                    val singleFrame = FrameKey(key.fileIndex, 1) !in framesMap
                    val outputFile = config.getOutputFileForFrame(key, singleFrame)
                    try {
                        renderer.renderFrame(frameGroup, outputFile, imagesDir, fontsDir)
                    } catch (e: IOException) {
                        logger.warn { "Failed to save file $outputFile" }
                        failed[key] = frameGroup
                    }

                    progressCb.incrementProgress()
                }

                failed
            }
        }
    }

}
