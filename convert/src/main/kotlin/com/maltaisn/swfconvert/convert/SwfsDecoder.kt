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

package com.maltaisn.swfconvert.convert

import com.flagstone.transform.Movie
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject


/**
 * Decodes SWF files with `transform-swf` in parallel.
 */
internal class SwfsDecoder @Inject constructor(
        private val coroutineScope: CoroutineScope,
        private val config: ConvertConfiguration
) {

    fun decodeFiles(files: List<File>): List<Movie> {
        val swfs = arrayOfNulls<Movie>(files.size)
        val progress = AtomicInteger()

        print("Decoded SWF file 0 / ${files.size}\r")
        val jobs = files.mapIndexed { i, file ->
            val job = coroutineScope.async(Dispatchers.IO) {
                val swf = Movie()
                swf.decodeFromFile(file)
                swfs[i] = swf

                val done = progress.incrementAndGet()
                print("Decoded SWF file $done / ${files.size}\r")
            }
            if (!config.parallelSwfDecoding) {
                runBlocking { job.await() }
            }
            job
        }
        if (config.parallelSwfDecoding) {
            runBlocking { jobs.awaitAll() }
        }

        println()

        return swfs.filterNotNull()
    }

}
