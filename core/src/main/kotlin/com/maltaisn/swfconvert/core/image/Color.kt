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

package com.maltaisn.swfconvert.core.image

import kotlin.math.roundToInt


/**
 * Inline color class, stored in ARGB format.
 */
inline class Color(val value: Int) {

    constructor(r: Int, g: Int, b: Int, a: Int = 0xFF) :
            this((a shl 24) or (r shl 16) or (g shl 8) or b)

    val a: Int get() = value ushr 24
    val r: Int get() = value ushr 16 and 0xFF
    val g: Int get() = value ushr 8 and 0xFF
    val b: Int get() = value and 0xFF

    fun withAlpha(a: Int) = Color(value and 0xFFFFFF or (a shl 24))

    fun divideAlpha(): Color {
        val m = if (a == 0) 0f else 255f / a
        val r = (r * m).roundToInt().coerceAtMost(0xFF)
        val g = (g * m).roundToInt().coerceAtMost(0xFF)
        val b = (b * m).roundToInt().coerceAtMost(0xFF)
        return Color(r, g, b, a)
    }

    fun toAwtColor() = java.awt.Color(value, true)

    override fun toString() = '#' + value.toUInt().toString(16).toUpperCase()


    companion object {

        val TRANSPARENT = Color(0)
        val BLACK = Color(0, 0, 0)

        fun gray(value: Int) = Color(value, value, value)

        fun fromPix15Bytes(arr: ByteArray, offset: Int): Color {
            val v = (arr[offset].toInt() and 0xFF shl 8) or
                    (arr[offset + 1].toInt() and 0xFF)
            val r = v and 0x7C00 ushr 7
            val g = v and 0x3E0 ushr 2
            val b = v and 0x1F shl 3
            return Color(r, g, b)
        }

        fun fromPix24Bytes(arr: ByteArray, offset: Int): Color {
            val r = arr[offset + 1].toInt() and 0xFF
            val g = arr[offset + 2].toInt() and 0xFF
            val b = arr[offset + 3].toInt() and 0xFF
            return Color(r, g, b)
        }

        fun fromRgbBytes(arr: ByteArray, offset: Int): Color {
            val r = arr[offset].toInt() and 0xFF
            val g = arr[offset + 1].toInt() and 0xFF
            val b = arr[offset + 2].toInt() and 0xFF
            return Color(r, g, b)
        }

        fun fromRgbaBytes(arr: ByteArray, offset: Int): Color {
            val r = arr[offset].toInt() and 0xFF
            val g = arr[offset + 1].toInt() and 0xFF
            val b = arr[offset + 2].toInt() and 0xFF
            val a = arr[offset + 3].toInt() and 0xFF
            return Color(r, g, b, a)
        }

        fun fromArgbBytes(arr: ByteArray, offset: Int): Color {
            val a = arr[offset].toInt() and 0xFF
            val r = arr[offset + 1].toInt() and 0xFF
            val g = arr[offset + 2].toInt() and 0xFF
            val b = arr[offset + 3].toInt() and 0xFF
            return Color(r, g, b, a)
        }
    }

}
