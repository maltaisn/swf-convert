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
internal class SvgFramesRenderer @Inject internal constructor(
    private val config: SvgConfiguration,
    private val progressCb: ProgressCallback,
    private val svgFrameRendererProvider: Provider<SvgFrameRenderer>
) : FramesRenderer {

    private val logger = logger()

    override suspend fun renderFrames(frameGroups: List<List<FrameGroup>>) {
        // Move images and fonts from temp dir to output dir
        val outputDir = config.output.first().parentFile

        val outputImagesDir = File(outputDir, "images")
        if (!config.imagesMode.embedded) {
            progressCb.showStep("Copying images to output") {
                val tempImagesDir = File(config.tempDir, "images")
                tempImagesDir.copyRecursively(outputImagesDir, true)
            }
        }

        val outputFontsDir = File(outputDir, "fonts")
        if (!config.fontsMode.embedded) {
            progressCb.showStep("Copying fonts to output") {
                val tempFontsDir = File(config.tempDir, "fonts")
                tempFontsDir.copyRecursively(outputFontsDir, true)
            }
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
        return progressCb.showStep("Writing SVG frames") {
            progressCb.showProgress(framesMap.size) {
                val failed = ConcurrentHashMap<FrameKey, FrameGroup>()

                framesMap.entries.mapInParallel(config.parallelFrameRendering) { (key, frameGroup) ->
                    val renderer = svgFrameRendererProvider.get()

                    val singleFrame = FrameKey(key.fileIndex, 1) !in framesMap
                    val outputFile = config.getOutputFileForFrame(key, singleFrame)
                    try {
                        renderer.renderFrame(frameGroup, outputFile, imagesDir, fontsDir)
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
