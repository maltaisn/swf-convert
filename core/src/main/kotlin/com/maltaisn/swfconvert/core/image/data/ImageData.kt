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

package com.maltaisn.swfconvert.core.image.data

import com.maltaisn.swfconvert.core.image.ImageFormat
import java.io.File


/**
 * Data for an image. An image is made of RGB channels encoded in [data],
 * optional alpha channel encoded in [alphaData] (use empty byte array for none),
 * a [format] used to encode both channels data, and dimensions.
 */
class ImageData(val data: ByteArray,
                val alphaData: ByteArray,
                val format: ImageFormat,
                val width: Int,
                val height: Int) {

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
