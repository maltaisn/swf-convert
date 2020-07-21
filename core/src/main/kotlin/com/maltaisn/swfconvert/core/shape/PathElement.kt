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

package com.maltaisn.swfconvert.core.shape

import java.text.DecimalFormat

sealed class PathElement(open val x: Float, open val y: Float) {

    abstract fun toSvg(svg: StringBuilder)

    data class MoveTo(override val x: Float, override val y: Float) : PathElement(x, y) {

        override fun toSvg(svg: StringBuilder) {
            svg.append("M ")
            svg.appendValues(x, y)
        }
    }

    data class LineTo(override val x: Float, override val y: Float) : PathElement(x, y) {

        override fun toSvg(svg: StringBuilder) {
            svg.append("L ")
            svg.appendValues(x, y)
        }
    }

    data class QuadTo(
        val cx: Float,
        val cy: Float,
        override val x: Float,
        override val y: Float
    ) : PathElement(x, y) {

        override fun toSvg(svg: StringBuilder) {
            svg.append("Q ")
            svg.appendValues(cx, cy, x, y)
        }

        @Suppress("MagicNumber")
        fun toCubic(sx: Float, sy: Float): CubicTo {
            // Convert quadratic to cubic bezier: https://stackoverflow.com/a/3162732/5288316
            val c1x = sx + 2f / 3f * (cx - sx)
            val c1y = sy + 2f / 3f * (cy - sy)
            val c2x = x + 2f / 3f * (cx - x)
            val c2y = y + 2f / 3f * (cy - y)
            return CubicTo(c1x, c1y, c2x, c2y, x, y)
        }
    }

    data class CubicTo(
        val c1x: Float,
        val c1y: Float,
        val c2x: Float,
        val c2y: Float,
        override val x: Float,
        override val y: Float
    ) : PathElement(x, y) {

        override fun toSvg(svg: StringBuilder) {
            svg.append("C ")
            svg.appendValues(c1x, c1y, c2x, c2y, x, y)
        }
    }

    object ClosePath : PathElement(0f, 0f) {

        override fun toSvg(svg: StringBuilder) {
            svg.append('Z')
        }

        override fun toString() = "Close path"
    }
}

private fun StringBuilder.appendValues(vararg values: Float) {
    for (v in values) {
        append(SVG_NB_FMT.format(v))
        append(' ')
    }
    delete(length - 1, length)
}

private val SVG_NB_FMT = DecimalFormat().apply {
    maximumFractionDigits = DEBUG_SVG_PRECISION
    isGroupingUsed = false
}

private const val DEBUG_SVG_PRECISION = 3
