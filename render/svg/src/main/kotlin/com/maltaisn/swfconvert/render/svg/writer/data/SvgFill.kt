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

package com.maltaisn.swfconvert.render.svg.writer.data

import com.maltaisn.swfconvert.core.image.Color
import com.maltaisn.swfconvert.render.svg.writer.toSvgUrlReference

internal sealed class SvgFill {
    abstract fun toSvg(): String
    override fun toString() = toSvg()
}

internal data class SvgFillColor(val color: Color) : SvgFill() {
    init {
        require(color.a == Color.COMPONENT_MAX) { "Must use fill-opacity to define alpha component instead." }
    }

    override fun toSvg(): String {
        val r = color.r
        val g = color.g
        val b = color.b
        return if (r.isColorComponentFoldable && g.isColorComponentFoldable && b.isColorComponentFoldable) {
            // Can use #xxx shorthand
            String(charArrayOf('#', HEX_CHARS[r and 0xF], HEX_CHARS[g and 0xF], HEX_CHARS[b and 0xF]))
        } else {
            // #xxxxxx
            color.toStringNoAlpha()
        }
    }
}

internal data class SvgFillId(val id: String) : SvgFill() {
    override fun toSvg() = id.toSvgUrlReference()
}

internal object SvgFillNone : SvgFill() {
    override fun toSvg() = "none"
}

/**
 * Returns whether the 4 most significant bits are equal to the 4 least
 * significant bits in the least significant byte of [this] int.
 * Basically this returns `true` for 0x00, 0x11, 0x22, 0x33, 0x44, etc.
 */
private val Int.isColorComponentFoldable: Boolean
    get() = (this and LOWER_NIB) == (this ushr UPPER_NIB_SHIFT)

private const val HEX_CHARS = "0123456789abcdef"

private const val LOWER_NIB = 0x0F
private const val UPPER_NIB_SHIFT = 4
