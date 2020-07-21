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
