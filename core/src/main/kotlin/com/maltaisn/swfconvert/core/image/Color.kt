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

import com.maltaisn.swfconvert.core.toHexString

/**
 * Inline color class, stored in ARGB format.
 */
inline class Color(val value: Int) {

    /**
     * Create a color from [red][r], [green][g], [blue][b] and optionally [alpha][a] component values.
     * No range check or masking is done, values are expected to be between 0 and 255.
     */
    constructor(r: Int, g: Int, b: Int, a: Int = COMPONENT_MAX) :
            this((a shl SHIFT_ALPHA) or (r shl SHIFT_RED) or
                    (g shl SHIFT_GREEN) or (b shl SHIFT_BLUE))

    val a: Int get() = value ushr SHIFT_ALPHA and MASK_BYTE
    val r: Int get() = value ushr SHIFT_RED and MASK_BYTE
    val g: Int get() = value ushr SHIFT_GREEN and MASK_BYTE
    val b: Int get() = value ushr SHIFT_BLUE and MASK_BYTE

    val floatA: Float get() = a / COMPONENT_MAX_F
    val floatR: Float get() = r / COMPONENT_MAX_F
    val floatG: Float get() = g / COMPONENT_MAX_F
    val floatB: Float get() = b / COMPONENT_MAX_F

    val opaque: Color get() = withAlpha(COMPONENT_MAX)

    fun withAlpha(a: Int) = Color(value and MASK_RGB or (a shl SHIFT_ALPHA))

    fun toAwtColor() = java.awt.Color(value, true)

    override fun toString() = '#' + value.toHexString()

    fun toStringNoAlpha() = '#' + value.toHexString().substring(2)

    companion object {

        private const val SHIFT_ALPHA = 24
        private const val SHIFT_RED = 16
        private const val SHIFT_GREEN = 8
        private const val SHIFT_BLUE = 0

        private const val MASK_RGB = 0xFFFFFF
        private const val MASK_BYTE = 0xFF

        const val COMPONENT_MAX = 255
        const val COMPONENT_MAX_F = 255f

        val TRANSPARENT = Color(0)
        val BLACK = Color(0, 0, 0)
        val RED = Color(255, 0, 0)
        val GREEN = Color(0, 255, 0)
        val BLUE = Color(0, 0, 255)

        fun gray(value: Int) = Color(value, value, value)
    }
}
