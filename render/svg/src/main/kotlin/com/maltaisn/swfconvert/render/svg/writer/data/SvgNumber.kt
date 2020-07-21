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
import com.maltaisn.swfconvert.render.svg.writer.format.format
import com.maltaisn.swfconvert.render.svg.writer.format.formatOptimized
import kotlin.math.sign

/**
 * A numeric value used in SVG, can be associated with a length unit
 * or be dimensionless with [SvgUnit.USER].
 */
internal data class SvgNumber(
    val value: Float,
    val units: SvgUnit = SvgUnit.USER
) {

    fun toSvg(precision: Int, optimized: Boolean) = if (optimized) {
        value.formatOptimized(precision)
    } else {
        value.format(precision)
    } + units.symbol

    override fun toString() = toSvg(DEBUG_SVG_PRECISION, false)

    operator fun compareTo(other: SvgNumber): Int {
        require(units == other.units || value.sign != other.value.sign || value == 0f) {
            "Can't compare numbers with different units"
        }
        return value.compareTo(other.value)
    }

    companion object {
        val ZERO = SvgNumber(0f)
    }

}
