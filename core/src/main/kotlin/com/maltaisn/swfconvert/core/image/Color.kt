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

import com.maltaisn.swfconvert.core.toHexString

/**
 * Inline color class, stored in ARGB format.
 */
@JvmInline
value class Color(val value: Int) {

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
