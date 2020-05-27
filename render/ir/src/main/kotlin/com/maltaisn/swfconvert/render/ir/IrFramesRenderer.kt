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
import com.maltaisn.swfconvert.render.core.FramesRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Provider


/**
 * Convert all frames from the intermediate representation to output format.
 */
class IrFramesRenderer @Inject internal constructor(
        private val coroutineScope: CoroutineScope,
        private val config: IrConfiguration,
        private val irFrameRendererProvider: Provider<IrFrameRenderer>
) : FramesRenderer {

    override fun renderFrames(frameGroups: List<FrameGroup>) {
        val progress = AtomicInteger()
        val jobs = frameGroups.mapIndexed { index, frameGroup ->
            val job = coroutineScope.async {
                val renderer = irFrameRendererProvider.get()
                renderer.renderFrame(index, frameGroup)

                val done = progress.incrementAndGet()
                print("Rendered frame $done / ${frameGroups.size}\r")
            }
            if (!config.parallelFrameRendering) {
                runBlocking { job.await() }
            }
            job
        }
        if (config.parallelFrameRendering) {
            runBlocking { jobs.awaitAll() }
        }

        println()
    }

}
