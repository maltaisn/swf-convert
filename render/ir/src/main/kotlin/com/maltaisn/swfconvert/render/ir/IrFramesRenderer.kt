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

package com.maltaisn.swfconvert.render.ir

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
 * Convert all frames from the intermediate representation to output format.
 */
internal class IrFramesRenderer @Inject internal constructor(
    private val config: IrConfiguration,
    private val progressCb: ProgressCallback,
    private val irFrameRendererProvider: Provider<IrFrameRenderer>
) : FramesRenderer {

    private val logger = logger()

    override suspend fun renderFrames(frameGroups: List<List<FrameGroup>>) {
        // Move images and fonts from temp dir to output dir
        val outputDir = config.output.first().parentFile

        val outputImagesDir = File(outputDir, "images")
        progressCb.showStep("Copying images to output") {
            val tempImagesDir = File(config.tempDir, "images")
            tempImagesDir.copyRecursively(outputImagesDir, true)
        }

        val outputFontsDir = File(outputDir, "fonts")
        progressCb.showStep("Copying fonts to output") {
            val tempFontsDir = File(config.tempDir, "fonts")
            tempFontsDir.copyRecursively(outputFontsDir, true)
        }

        // Write frames
        var frames = frameGroups.toFramesMap()
        while (true) {
            frames = renderFrames(frames)

            if (frames.isNotEmpty()) {
                // Some files couldn't be saved. Ask to retry.
                if (readAffirmativeAnswer("Could not save ${frames.size} files.")) {
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
     * Render frames in [framesMap].
     * Returns another frames map for frames that couldn't be saved.
     */
    private suspend fun renderFrames(framesMap: FramesMap): FramesMap {
        return progressCb.showStep("Writing JSON frames") {
            progressCb.showProgress(framesMap.size) {
                val failed = ConcurrentHashMap<FrameKey, FrameGroup>()

                framesMap.entries.mapInParallel(config.parallelFrameRendering) { (key, frameGroup) ->
                    val renderer = irFrameRendererProvider.get()

                    val singleFrame = FrameKey(key.fileIndex, 1) !in framesMap
                    val outputFile = config.getOutputFileForFrame(key, singleFrame)
                    try {
                        renderer.renderFrame(outputFile, frameGroup)
                    } catch (e: IOException) {
                        logger.info(e) { "Failed to save file $outputFile" }
                        failed[key] = frameGroup
                    }

                    progressCb.incrementProgress()
                }

                failed
            }
        }
    }

}
