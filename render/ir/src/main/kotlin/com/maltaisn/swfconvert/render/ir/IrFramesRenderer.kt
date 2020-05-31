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

import com.maltaisn.swfconvert.core.*
import com.maltaisn.swfconvert.render.core.FramesRenderer
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider


/**
 * Convert all frames from the intermediate representation to output format.
 */
class IrFramesRenderer @Inject internal constructor(
        private val config: IrConfiguration,
        private val progressCb: ProgressCallback,
        private val irFrameRendererProvider: Provider<IrFrameRenderer>
) : FramesRenderer {

    override suspend fun renderFrames(frameGroups: List<FrameGroup>) {
        var frames = frameGroups.withIndex().associate { (k, v) -> k to v }

        save@ while (true) {
            frames = renderFrames(frames)

            if (frames.isNotEmpty()) {
                // Some files couldn't be saved. Ask to retry.
                print("Could not save ${frames.size} files. Retry (Y/N)? ")
                retry@ while (true) {
                    when (readLine()?.toLowerCase()) {
                        "y" -> continue@save
                        "n" -> return
                        else -> continue@retry
                    }
                }
            } else {
                return
            }
        }
    }

    /**
     * Render [frameGroups], a map of frame by file index.
     * Returns a similar map for frames that couldn't be saved.
     */
    private suspend fun renderFrames(frameGroups: Map<Int, FrameGroup>): Map<Int, FrameGroup> {
        return progressCb.showStep("Writing JSON frames", true) {
            progressCb.showProgress(frameGroups.size) {
                val failed = ConcurrentHashMap<Int, FrameGroup>()

                frameGroups.entries.mapInParallel(config.parallelFrameRendering) { (i, frameGroup) ->
                    val renderer = irFrameRendererProvider.get()

                    try {
                        renderer.renderFrame(i, frameGroup)
                    } catch (e: IOException) {
                        failed[i] = frameGroup
                    }

                    progressCb.incrementProgress()
                }

                failed
            }
        }
    }

}
