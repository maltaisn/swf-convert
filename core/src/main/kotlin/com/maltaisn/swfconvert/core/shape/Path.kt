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

package com.maltaisn.swfconvert.core.shape

import com.maltaisn.swfconvert.core.shape.PathElement.ClosePath
import com.maltaisn.swfconvert.core.shape.PathElement.LineTo
import com.maltaisn.swfconvert.core.shape.PathElement.MoveTo

data class Path(
    val elements: List<PathElement>,
    val fillStyle: PathFillStyle? = null,
    val lineStyle: PathLineStyle? = null
) {

    fun toSvg(): String {
        val svg = StringBuilder()
        if (elements.isNotEmpty()) {
            for (element in elements) {
                element.toSvg(svg)
                svg.append(' ')
            }
            svg.delete(svg.length - 1, svg.length)
        }
        return svg.toString()
    }

    override fun toString() = buildString {
        append("Path(path='")
        append(toSvg())
        append("'")
        if (lineStyle != null) {
            append(", line=")
            append(lineStyle)
        }
        if (fillStyle != null) {
            append(", fill=")
            append(fillStyle)
        }
        append(")")
    }

    companion object {

        fun rectangle(
            x: Float,
            y: Float,
            w: Float,
            h: Float,
            fillStyle: PathFillStyle? = null,
            lineStyle: PathLineStyle? = null
        ) = Path(mutableListOf(
            MoveTo(x, y),
            LineTo(x + w, y),
            LineTo(x + w, y + h),
            LineTo(x, y + h),
            ClosePath
        ), fillStyle, lineStyle)
    }

}
