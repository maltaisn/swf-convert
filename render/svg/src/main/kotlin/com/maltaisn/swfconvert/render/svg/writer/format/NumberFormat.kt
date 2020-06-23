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

/**
 * Maximum precision that can be used for SVG values.
 * Float has a maximum of 7 decimal digits precision, but floating point errors
 * happens all the time so this was reduced to 5.
 */
internal const val SVG_MAX_PRECISION = 5

internal const val DEBUG_SVG_PRECISION = 3

fun requireSvgPrecision(precision: Int) = require(precision in 0..SVG_MAX_PRECISION) {
    "Precision must be between 0 and $SVG_MAX_PRECISION."
}
