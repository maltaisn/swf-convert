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

package com.maltaisn.swfconvert.core.text

import com.maltaisn.swfconvert.core.shape.Path

data class GlyphData(
    val advanceWidth: Float,
    val contours: List<Path>
) {

    val isWhitespace: Boolean
        get() = contours.isEmpty()

    override fun toString() = buildString {
        append("GlyphData(advance=")
        append(advanceWidth)
        append(", path='")
        for (contour in contours) {
            append(contour.toSvg())
            append(' ')
        }
        if (contours.isNotEmpty()) {
            deleteCharAt(length - 1)
        }
        append("')")
    }

    companion object {
        /**
         * The size of the EM square in the intermediate representation.
         * This value should be equal to the size of the EM square used by doubletype.
         * The glyph contours data and the advance width are defined in EM square space.
         */
        const val EM_SQUARE_SIZE = 1024f

        /** Advance width used for whitespace characters. */
        const val WHITESPACE_ADVANCE_WIDTH = EM_SQUARE_SIZE / 4
    }

}
