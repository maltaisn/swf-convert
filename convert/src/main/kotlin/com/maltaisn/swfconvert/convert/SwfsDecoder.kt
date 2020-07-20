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
import com.maltaisn.swfconvert.convert.context.ConvertContext
import com.maltaisn.swfconvert.convert.context.SwfFileContext
import com.maltaisn.swfconvert.core.ProgressCallback
import com.maltaisn.swfconvert.core.mapInParallel
import com.maltaisn.swfconvert.core.showProgress
import com.maltaisn.swfconvert.core.showStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.zip.DataFormatException
import javax.inject.Inject

/**
 * Decodes SWF files with `transform-swf` in parallel.
 */
internal class SwfsDecoder @Inject constructor(
    private val config: ConvertConfiguration,
    private val progressCb: ProgressCallback
) {

    suspend fun decodeFiles(context: ConvertContext, files: List<File>): List<Movie> {
        return progressCb.showStep("Decoding SWFs") {
            progressCb.showProgress(files.size) {
                files.withIndex().mapInParallel(config.parallelSwfDecoding) { (i, file) ->
                    val swf = decodeFile(SwfFileContext(context, file, i), file)
                    progressCb.incrementProgress()
                    swf
                }
            }
        }
    }

    private suspend fun decodeFile(fileContext: SwfFileContext, file: File): Movie {
        val swf = Movie()
        withContext(Dispatchers.IO) {
            try {
                swf.decodeFromFile(file)
            } catch (e: DataFormatException) {
                conversionError(fileContext, "Error while decoding SWF file: bad format")
            } catch (e: IOException) {
                conversionError(fileContext, "Error while decoding SWF file: I/O exception")
            }
        }
        return swf
    }

}
