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
import com.maltaisn.swfconvert.core.FrameGroup
import com.maltaisn.swfconvert.core.text.Font
import com.maltaisn.swfconvert.core.text.FontId
import com.maltaisn.swfconvert.core.use
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Provider


/**
 * Converts a collection of SWF files to the [FrameGroup] intermediate representation.
 */
internal class SwfsConverter @Inject constructor(
        private val coroutineScope: CoroutineScope,
        private val config: ConvertConfiguration,
        private val swfConverterProvider: Provider<SwfConverter>
) {

    fun createFrameGroups(swfs: List<Movie>, fontsMap: Map<FontId, Font>): List<FrameGroup> {
        val frameGroups = arrayOfNulls<FrameGroup>(swfs.size)
        val progress = AtomicInteger()

        print("Converted SWF 0 / ${swfs.size}\r")
        val jobs = swfs.mapIndexed { i, swf ->
            val job = coroutineScope.async {
                frameGroups[i] = swfConverterProvider.get().use {
                    it.createFrameGroup(swf, fontsMap, i)
                }

                val done = progress.incrementAndGet()
                print("Converted SWF $done / ${swfs.size}\r")
            }
            if (!config.parallelSwfConversion) {
                runBlocking { job.await() }
            }
            job
        }
        if (config.parallelSwfConversion) {
            runBlocking { jobs.awaitAll() }
        }

        println()

        return frameGroups.filterNotNull()
    }

}
