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

package com.maltaisn.swfconvert.render.svg.writer.data

import com.maltaisn.swfconvert.render.svg.writer.appendValuesList


internal sealed class SvgTransform {

    protected abstract val name: String
    protected abstract val values: FloatArray

    fun toSvg(precision: Int) = buildString {
        append(name)
        append('(')
        appendValuesList(precision, *values)
        append(')')
    }

    override fun toString() = toSvg(3)


    data class Matrix(val a: Float, val b: Float, val c: Float,
                      val d: Float, val e: Float, val f: Float) : SvgTransform() {
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

internal fun List<SvgTransform>.toSvgTransformList(precision: Int) =
        this.joinToString("") { it.toSvg(precision) }
