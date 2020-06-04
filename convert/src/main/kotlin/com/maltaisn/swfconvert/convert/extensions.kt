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

import com.flagstone.transform.datatype.CoordTransform
import com.maltaisn.swfconvert.core.FrameGroup
import com.maltaisn.swfconvert.core.YAxisDirection
import com.maltaisn.swfconvert.core.image.Color
import java.awt.geom.AffineTransform
import java.io.ByteArrayInputStream
import java.util.zip.InflaterInputStream
import com.flagstone.transform.datatype.Color as FColor


internal fun FColor.toColor() = Color(this.red, this.green, this.blue, this.alpha)

/**
 * Convert [this] transform to an [AffineTransform].
 * @param yAxisDirection If direction is [YAxisDirection.UP], the shearing components are
 * swapped to reverse the rotation direction back, because the flipping of the [FrameGroup]
 * has inverted the rotation once.
 */
internal fun CoordTransform.toAffineTransform(yAxisDirection: YAxisDirection) = when (yAxisDirection) {
    YAxisDirection.UP -> AffineTransform(
            this.scaleX,
            this.shearX,
            this.shearY,
            this.scaleY,
            this.translateX.toFloat(),
            this.translateY.toFloat())
    YAxisDirection.DOWN -> AffineTransform(
            this.scaleX,
            this.shearY,
            this.shearX,
            this.scaleY,
            this.translateX.toFloat(),
            this.translateY.toFloat())
}

/**
 * Null-safe alternative to [toAffineTransform] that returns the identity matrix if [this] is `null`.
 */
internal fun CoordTransform?.toAffineTransformOrIdentity(yAxisDirection: YAxisDirection) =
        this?.toAffineTransform(yAxisDirection) ?: AffineTransform()


internal fun ByteArray.zlibDecompress(): ByteArray {
    val inflaterStream = InflaterInputStream(ByteArrayInputStream(this))
    val decompressed = inflaterStream.readBytes()
    inflaterStream.close()
    return decompressed
}
