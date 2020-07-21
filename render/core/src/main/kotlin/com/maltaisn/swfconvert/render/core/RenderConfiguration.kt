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

package com.maltaisn.swfconvert.render.core

import java.io.File

/**
 * Configuration used to convert from the intermediate representation to an output format.
 */
interface RenderConfiguration {

    /** Output files, size should be 1 or same as input size. */
    val output: List<File>

    /** Directory to which temp and debug files are written. */
    val tempDir: File

    /** Whether to convert input files from intermediate format to output format in parallel. */
    val parallelFrameRendering: Boolean

    /**
     * Get the output file for a frame with a [key].
     * @param singleFrame Whether the source file for that frame has a single frame or not.
     */
    fun getOutputFileForFrame(key: FrameKey, singleFrame: Boolean): File {
        val file = output[key.fileIndex]
        val filename = buildString {
            append(file.nameWithoutExtension)
            if (!singleFrame) {
                append('-')
                append(key.frameIndex)
            }
            append('.')
            append(file.extension)
        }
        return file.resolveSibling(filename)
    }
}
