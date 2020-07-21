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
