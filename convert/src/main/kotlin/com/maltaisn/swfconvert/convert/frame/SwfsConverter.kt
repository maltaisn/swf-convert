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

package com.maltaisn.swfconvert.convert.frame

import com.flagstone.transform.Movie
import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.convert.context.ConvertContext
import com.maltaisn.swfconvert.convert.context.SwfFileContext
import com.maltaisn.swfconvert.convert.font.FontsMap
import com.maltaisn.swfconvert.convert.frame.data.SwfFrame
import com.maltaisn.swfconvert.core.FrameGroup
import com.maltaisn.swfconvert.core.ProgressCallback
import com.maltaisn.swfconvert.core.mapInParallel
import com.maltaisn.swfconvert.core.showProgress
import com.maltaisn.swfconvert.core.showStep
import com.maltaisn.swfconvert.core.use
import javax.inject.Inject
import javax.inject.Provider

/**
 * Converts a collection of SWF files to the [FrameGroup] intermediate representation.
 */
internal class SwfsConverter @Inject constructor(
    private val config: ConvertConfiguration,
    private val progressCb: ProgressCallback,
    private val swfFrameBuilderProvider: Provider<SwfFrameBuilder>,
    private val swfFrameConverterProvider: Provider<SwfFrameConverter>
) {

    suspend fun createFrameGroups(
        context: ConvertContext,
        swfs: List<Movie>,
        fontsMap: FontsMap
    ): List<List<FrameGroup>> {
        val frames = findAllFrames(context, swfs)
        return createAllFrameGroups(frames, fontsMap)
    }

    private suspend fun findAllFrames(context: ConvertContext, swfs: List<Movie>): List<List<SwfFrame>> =
        progressCb.showStep("Finding SWF frames") {
            progressCb.showProgress(swfs.size) {
                swfs.withIndex().mapInParallel(config.parallelSwfConversion) { (fileIndex, swf) ->
                    val swfContext = SwfFileContext(context, config.input[fileIndex], fileIndex)
                    val swfFrames = swfFrameBuilderProvider.get().createFrames(swf, swfContext)

                    progressCb.incrementProgress()

                    if (config.ignoreEmptyFrames) {
                        // Keep only non-empty frames.
                        swfFrames.filter { it.objects.isNotEmpty() }
                    } else {
                        swfFrames
                    }
                }
            }
        }

    private suspend fun createAllFrameGroups(
        frames: List<List<SwfFrame>>,
        fontsMap: FontsMap
    ): List<List<FrameGroup>> {
        // Create all frame groups as a single list
        val allFrames = frames.flatten()
        val allFrameGroups = progressCb.showStep("Converting SWF frames") {
            progressCb.showProgress(allFrames.size) {
                allFrames.mapInParallel(config.parallelSwfConversion) { frame ->
                    val frameGroup = swfFrameConverterProvider.get().use {
                        it.createFrameGroup(frame, fontsMap)
                    }
                    progressCb.incrementProgress()
                    frameGroup
                }
            }
        }

        // Unflatten the list of frame groups
        var lastIndex = 0
        return frames.map { swfFrames ->
            val newIndex = lastIndex + swfFrames.size
            val swfFrameGroups = allFrameGroups.subList(lastIndex, newIndex)
            lastIndex = newIndex
            swfFrameGroups
        }
    }

}
