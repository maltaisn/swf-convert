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

package com.maltaisn.swfconvert.core.shape.path


data class Path(val elements: List<PathElement>,
                val fillStyle: PathFillStyle? = null,
                val lineStyle: PathLineStyle? = null) {

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

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Path(path='")
        sb.append(toSvg())
        sb.append("'")
        if (lineStyle != null) {
            sb.append(", line=")
            sb.append(lineStyle)
        }
        if (fillStyle != null) {
            sb.append(", fill=")
            sb.append(fillStyle)
        }
        sb.append(")")
        return sb.toString()
    }

}