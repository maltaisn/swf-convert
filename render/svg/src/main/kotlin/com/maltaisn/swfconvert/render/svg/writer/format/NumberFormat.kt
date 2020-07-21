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

package com.maltaisn.swfconvert.render.svg.writer.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

internal val numberFormat = ThreadLocal.withInitial { createNumberFormat() }

/**
 * Create a new [DecimalFormat] with [a maximal number][precision] of fraction digits,
 * that doesn't use grouping and uses '.' as a decimal separator.
 */
internal fun createNumberFormat(precision: Int? = null) = DecimalFormat().apply {
    if (precision != null) {
        maximumFractionDigits = precision
    }
    isGroupingUsed = false
    decimalFormatSymbols = DecimalFormatSymbols().apply {
        decimalSeparator = '.'
    }
}

/**
 * Get a number format from [numberFormat] that has [a maximal number][precision] of fraction digits.
 */
internal fun getNumberFormat(precision: Int): NumberFormat {
    val nbFmt = numberFormat.get()
    if (nbFmt.maximumFractionDigits != precision) {
        nbFmt.maximumFractionDigits = precision
    }
    return nbFmt
}

internal const val SVG_MAX_PRECISION = 5

internal const val DEBUG_SVG_PRECISION = 3

fun requireSvgPrecision(precision: Int) = require(precision in 0..SVG_MAX_PRECISION) {
    "Precision must be between 0 and $SVG_MAX_PRECISION."
}
