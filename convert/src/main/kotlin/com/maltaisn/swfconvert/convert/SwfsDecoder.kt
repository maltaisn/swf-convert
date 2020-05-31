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
import com.maltaisn.swfconvert.core.ProgressCallback
import com.maltaisn.swfconvert.core.mapInParallel
import com.maltaisn.swfconvert.core.showProgress
import com.maltaisn.swfconvert.core.showStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject


/**
 * Decodes SWF files with `transform-swf` in parallel.
 */
internal class SwfsDecoder @Inject constructor(
        private val config: ConvertConfiguration,
        private val progressCb: ProgressCallback
) {

    suspend fun decodeFiles(files: List<File>): List<Movie> {
        return progressCb.showStep("Decoding SWFs", true) {
            progressCb.showProgress(files.size) {
                files.mapInParallel(config.parallelSwfDecoding) { file ->
                    val swf = Movie()
                    withContext(Dispatchers.IO) {
                        swf.decodeFromFile(file)
                    }
                    progressCb.incrementProgress()
                    swf
                }
            }
        }
    }

}
