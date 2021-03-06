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

import com.maltaisn.swfconvert.render.svg.writer.format.DEBUG_SVG_PRECISION
import com.maltaisn.swfconvert.render.svg.writer.format.appendValuesList
import com.maltaisn.swfconvert.render.svg.writer.format.appendValuesListOptimized
import com.maltaisn.swfconvert.render.svg.writer.format.getNumberFormat
import java.text.NumberFormat

internal sealed class SvgTransform {

    protected abstract val name: String
    protected abstract val values: FloatArray

    fun toSvg(precision: Int, optimized: Boolean) =
        toSvg(getNumberFormat(precision), optimized)

    fun toSvg(numberFmt: NumberFormat, optimized: Boolean) = buildString {
        append(name)
        append('(')
        if (optimized) {
            appendValuesListOptimized(numberFmt, null, values)
        } else {
            appendValuesList(numberFmt, values)
        }
        append(')')
    }

    override fun toString() = toSvg(DEBUG_SVG_PRECISION, false)

    data class Matrix(
        val a: Float,
        val b: Float,
        val c: Float,
        val d: Float,
        val e: Float,
        val f: Float
    ) : SvgTransform() {
        override val name: String
            get() = "matrix"

        override val values: FloatArray
            get() = floatArrayOf(a, b, c, d, e, f)
    }

    data class Translate(val x: Float, val y: Float = 0f) : SvgTransform() {
        override val name: String
            get() = "translate"

        override val values: FloatArray
            get() = if (y == 0f) {
                floatArrayOf(x)
            } else {
                floatArrayOf(x, y)
            }
    }

    data class Scale(val x: Float, val y: Float = 0f) : SvgTransform() {
        override val name: String
            get() = "scale"

        override val values: FloatArray
            get() = if (x == y) {
                floatArrayOf(x)
            } else {
                floatArrayOf(x, y)
            }
    }

    data class Rotate(val angle: Float, val x: Float = 0f, val y: Float = 0f) : SvgTransform() {
        override val name: String
            get() = "rotate"

        override val values: FloatArray
            get() = if (x == 0f && y == 0f) {
                floatArrayOf(angle)
            } else {
                floatArrayOf(angle, x, y)
            }
    }

    data class SkewX(val angle: Float) : SvgTransform() {
        override val name: String
            get() = "skewX"

        override val values: FloatArray
            get() = floatArrayOf(angle)
    }

    data class SkewY(val angle: Float) : SvgTransform() {
        override val name: String
            get() = "skewY"

        override val values: FloatArray
            get() = floatArrayOf(angle)
    }
}

internal fun List<SvgTransform>.toSvgTransformList(precision: Int, optimized: Boolean): String {
    // Note: arguably, precision should affect the number of significant digits kept for transformations that use the
    // scale and shear components. However that complicates things so just use higher precision if that's problematic.
    val numberFmt = getNumberFormat(precision)
    return this.joinToString(if (optimized) "" else " ") { it.toSvg(numberFmt, optimized) }
}
