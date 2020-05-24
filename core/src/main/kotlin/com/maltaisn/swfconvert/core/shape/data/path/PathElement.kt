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

package com.maltaisn.swfconvert.core.shape.data.path

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

    data class QuadTo(val cx: Float, val cy: Float,
                      override val x: Float, override val y: Float) : PathElement(x, y) {

        override fun toSvg(svg: StringBuilder) {
            svg.append("Q ")
            svg.appendValues(cx, cy, x, y)
        }

        fun toCubic(sx: Float, sy: Float): CubicTo {
            // Convert quadratic to cubic bezier: https://stackoverflow.com/a/3162732/5288316
            val c1x = sx + 2f / 3f * (cx - sx)
            val c1y = sy + 2f / 3f * (cy - sy)
            val c2x = x + 2f / 3f * (cx - x)
            val c2y = y + 2f / 3f * (cy - y)
            return CubicTo(c1x, c1y, c2x, c2y, x, y)
        }
    }

    data class CubicTo(val c1x: Float, val c1y: Float,
                       val c2x: Float, val c2y: Float,
                       override val x: Float, override val y: Float) : PathElement(x, y) {

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

    data class Rectangle(override val x: Float, override val y: Float,
                         val width: Float, val height: Float) : PathElement(x, y) {

        override fun toSvg(svg: StringBuilder) {
            svg.append("M ")
            svg.appendValues(x, y)
            svg.append(" h ")
            svg.appendValues(width)
            svg.append(" v ")
            svg.appendValues(height)
            svg.append(" h ")
            svg.appendValues(-width)
            svg.append(" Z")
        }
    }
}


private val SVG_NB_FMT = DecimalFormat().apply {
    maximumFractionDigits = 3
    isGroupingUsed = false
}

private fun StringBuilder.appendValues(vararg values: Float) {
    for (v in values) {
        append(SVG_NB_FMT.format(v))
        append(' ')
    }
    delete(length - 1, length)
}
