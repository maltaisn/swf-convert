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

import com.flagstone.transform.MovieTag
import com.flagstone.transform.Place
import com.flagstone.transform.Place2
import com.flagstone.transform.Place3
import com.flagstone.transform.datatype.CoordTransform
import com.maltaisn.swfconvert.convert.wrapper.WPlace
import com.maltaisn.swfconvert.core.image.Color
import java.awt.geom.AffineTransform
import java.io.ByteArrayInputStream
import java.util.zip.InflaterInputStream
import com.flagstone.transform.datatype.Color as FColor


internal fun FColor.toColor() = Color(this.red, this.green, this.blue, this.alpha)

/**
 * Convert [this] transform to an [AffineTransform].
 * The skewing parameters are reversed to reverse rotation direction since
 * everything is flipped vertically.
 */
internal fun CoordTransform.toAffineTransform() = AffineTransform(
        this.scaleX, this.shearX, this.shearY, this.scaleY,
        this.translateX.toFloat(), this.translateY.toFloat())

internal fun CoordTransform?.toAffineTransformOrIdentity() = this?.toAffineTransform() ?: AffineTransform()

internal fun MovieTag.toPlaceTagOrNull() = when (this) {
    is Place -> WPlace(this)
    is Place2 -> WPlace(this)
    is Place3 -> WPlace(this)
    else -> null
}

internal fun ByteArray.zlibDecompress(): ByteArray {
    val inflaterStream = InflaterInputStream(ByteArrayInputStream(this))
    val decompressed = inflaterStream.readBytes()
    inflaterStream.close()
    return decompressed
}

internal fun validateFilename(filename: String) =
        filename.replace(INVALID_FILENAME_CHARS_PATTERN, "")

private val INVALID_FILENAME_CHARS_PATTERN = """[/\\:*?"<>|]""".toRegex()
