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
