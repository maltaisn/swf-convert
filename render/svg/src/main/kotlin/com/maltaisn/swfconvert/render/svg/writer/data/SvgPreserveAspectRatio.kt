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

internal data class SvgPreserveAspectRatio(val align: Align, val slice: Boolean = false) {

    fun toSvg() = align.svgName + if (slice) " slice" else ""

    enum class Align(val svgName: String) {
        NONE("none"),
        X_MIN_Y_MIN("xMinYMin"),
        X_MID_Y_MIN("xMidYMin"),
        X_MAX_Y_MIN("xMaxYMin"),
        X_MIN_Y_MID("xMinYMid"),
        X_MID_Y_MID("xMidYMid"),
        X_MAX_Y_MID("xMaxYMid"),
        X_MIN_Y_MAX("xMinYMax"),
        X_MID_Y_MAX("xMidYMax"),
        X_MAX_Y_MAX("xMaxYMax")
    }

    enum class MeetOrSlice(val svgName: String) {
        MEET("meet"),
        SLICE("slice")
    }

    companion object {
        val NONE = SvgPreserveAspectRatio(Align.NONE)
    }

}
