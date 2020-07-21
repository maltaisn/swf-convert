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

internal data class SvgGradientStop(
    val offset: Float,
    val color: Color,
    val opacity: Float
) {
    init {
        require(offset in 0f..1f) { "Gradient stop offset must be between 0 and 1" }
    }
}

internal fun List<SvgGradientStop>.validateGradientStops() {
    require(this.size >= 2) { "Gradient must have at least 2 stops" }
    require(this.first().offset == 0f) { "First stop offset must be 0." }
    require(this.last().offset == 1f) { "Last stop offset must be 1." }
    var lastOffset = 0f
    for (i in 1 until this.lastIndex) {
        val offset = this[i].offset
        require(offset > lastOffset || offset == 1f) { "Stop offset must be greater than last." }
        lastOffset = offset
    }
}
