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

import com.flagstone.transform.datatype.Blend
import com.flagstone.transform.datatype.CoordTransform
import com.maltaisn.swfconvert.core.BlendMode
import com.maltaisn.swfconvert.core.image.Color
import java.awt.geom.AffineTransform
import java.io.ByteArrayInputStream
import java.util.zip.InflaterInputStream
import com.flagstone.transform.datatype.Color as FColor

/**
 * Convert a SWF color to a [Color].
 */
internal fun FColor.toColor() = Color(this.red, this.green, this.blue, this.alpha)

/**
 * Convert a SWF blend mode to a [BlendMode].
 */
internal fun Blend.toBlendMode() = BLEND_MODE_MAP[this]

private val BLEND_MODE_MAP = mapOf(
    Blend.NULL to BlendMode.NULL,
    Blend.NORMAL to BlendMode.NORMAL,
    Blend.LAYER to BlendMode.LAYER,
    Blend.MULTIPLY to BlendMode.MULTIPLY,
    Blend.SCREEN to BlendMode.SCREEN,
    Blend.LIGHTEN to BlendMode.LIGHTEN,
    Blend.DARKEN to BlendMode.DARKEN,
    Blend.ADD to BlendMode.ADD,
    Blend.SUBTRACT to BlendMode.SUBTRACT,
    Blend.DIFFERENCE to BlendMode.DIFFERENCE,
    Blend.INVERT to BlendMode.INVERT,
    Blend.ERASE to BlendMode.ERASE,
    Blend.OVERLAY to BlendMode.OVERLAY,
    Blend.HARDLIGHT to BlendMode.HARDLIGHT)

/**
 * Convert [this] transform to an [AffineTransform].
 *
 * transform-swf shear components are reversed compared to the ones [AffineTransform] use.
 * The SWF reference states that the components of the `MATRIX` record are used as followed:
 * ```
 * x' = x * ScaleX + y * RotateSkew1 + TranslateX
 * y' = x * RotateSkew0 + y * ScaleY + TranslateY
 * ```
 * With `RotateSkew0` being encoded just before `RotateSkew1`. This calculation implies
 * `RotateSkew1` should correspond to AffineTransform's shearX and `RotateSkew0` should be shearY.
 */
internal fun CoordTransform.toAffineTransform() = AffineTransform(
    this.scaleX,
    this.shearX,
    this.shearY,
    this.scaleY,
    this.translateX.toFloat(),
    this.translateY.toFloat())

/**
 * Null-safe alternative to [toAffineTransform] that returns the identity matrix if [this] is `null`.
 */
internal fun CoordTransform?.toAffineTransformOrIdentity() =
    this?.toAffineTransform() ?: AffineTransform()

/**
 * Decompress [this] byte array using ZLIB decompression.
 */
internal fun ByteArray.zlibDecompress(): ByteArray {
    val inflaterStream = InflaterInputStream(ByteArrayInputStream(this))
    val decompressed = inflaterStream.readBytes()
    inflaterStream.close()
    return decompressed
}
