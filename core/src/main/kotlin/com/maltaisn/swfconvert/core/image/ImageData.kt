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

package com.maltaisn.swfconvert.core.image

import java.io.File

/**
 * Data for an image. An image is made of RGB channels encoded in [data],
 * optional alpha channel encoded in [alphaData] (use empty byte array for none),
 * a [format] used to encode both channels data, and dimensions.
 */
class ImageData(
    val data: ByteArray,
    val alphaData: ByteArray,
    val format: ImageFormat,
    val width: Int,
    val height: Int
) {

    var dataFile: File? = null
    var alphaDataFile: File? = null

    private var hash: Int = 0

    override fun toString() = "ImageData(width=$width, height=$height)"

    override fun equals(other: Any?) = when {
        this === other -> true
        other !is ImageData -> false
        hash != 0 && other.hash != 0 && hash != other.hash -> false
        else -> data.contentEquals(other.data) &&
                alphaData.contentEquals(other.alphaData)
    }

    override fun hashCode(): Int {
        if (hash == 0) {
            hash = arrayOf(data, alphaData).contentDeepHashCode()
        }
        return hash
    }
}
