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

// Decoding SWF color records is a bit more lower level than the rest so we'll suppress
// the magic number lint for this file, because named constants would make even less sense.
@file:Suppress("MagicNumber")

package com.maltaisn.swfconvert.convert.image

import com.maltaisn.swfconvert.core.image.Color
import kotlin.math.roundToInt

internal fun ByteArray.pix15BytesToColor(offset: Int): Color {
    // PIX15 record is 2 bytes wide, color components each take 5 bits (MSB is always 0).
    val v = (this[offset].asUInt() shl 8) or
            (this[offset + 1].asUInt())
    val r = v and 0b111110000000000 ushr 7
    val g = v and 0b1111100000 ushr 2
    val b = v and 0b11111 shl 3
    return Color(r, g, b)
}

internal fun ByteArray.pix24BytesToColor(offset: Int): Color {
    // PIX24 record is 3 bytes wide, color components each take one byte.
    val r = this[offset + 1].asUInt()
    val g = this[offset + 2].asUInt()
    val b = this[offset + 3].asUInt()
    return Color(r, g, b)
}

internal fun ByteArray.rgbBytesToColor(offset: Int): Color {
    val r = this[offset].asUInt()
    val g = this[offset + 1].asUInt()
    val b = this[offset + 2].asUInt()
    return Color(r, g, b)
}

internal fun ByteArray.rgbaBytesToColor(offset: Int): Color {
    val r = this[offset].asUInt()
    val g = this[offset + 1].asUInt()
    val b = this[offset + 2].asUInt()
    val a = this[offset + 3].asUInt()
    return Color(r, g, b, a)
}

internal fun ByteArray.argbBytesToColor(offset: Int): Color {
    val r = this[offset + 1].asUInt()
    val g = this[offset + 2].asUInt()
    val b = this[offset + 3].asUInt()
    val a = this[offset].asUInt()
    return Color(r, g, b, a)
}

internal fun Color.divideAlpha(): Color {
    val m = if (a == 0) 0f else Color.COMPONENT_MAX_F / a
    val r = (r * m).roundToInt().coerceAtMost(Color.COMPONENT_MAX)
    val g = (g * m).roundToInt().coerceAtMost(Color.COMPONENT_MAX)
    val b = (b * m).roundToInt().coerceAtMost(Color.COMPONENT_MAX)
    return Color(r, g, b, a)
}

/** Short version of `Byte.toUByte().toInt()`. */
private fun Byte.asUInt() = this.toInt() and 0xFF
