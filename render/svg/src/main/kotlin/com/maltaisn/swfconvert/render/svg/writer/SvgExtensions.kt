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

package com.maltaisn.swfconvert.render.svg.writer

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
 * Get a number format from [numberFormat] that has [a maximal number][precision] of fraction digits,
 */
internal fun getNumberFormat(precision: Int): NumberFormat {
    val nbFmt = numberFormat.get()
    if (nbFmt.maximumFractionDigits != precision) {
        nbFmt.maximumFractionDigits = precision
    }
    return nbFmt
}

/**
 * Format [this] float to a String value, rounding to [precision].
 */
internal fun Float.format(precision: Int) = getNumberFormat(precision).format(this)

/**
 * Format [this] float to a String with [numberFmt].
 * - `-0f` is formatted as "0" instead of "-0"
 * - Leading zero on number between -1 and 1 is omitted. (except for zero itself)
 */
internal fun Float.formatOptimized(numberFmt: NumberFormat): String {
    var str = numberFmt.format(this)
    if (str == "-0") {
        str = "0"
    }
    if (this in -1.0..1.0 && '.' in str) {
        str = str.replaceFirst("0", "")
    }
    return str
}

internal fun Float.formatOptimized(precision: Int) = formatOptimized(getNumberFormat(precision))

/**
 * Append a list of [values] to [this] StringBuilder, separated by spaces.
 */
internal fun StringBuilder.appendValuesList(values: Array<String>) {
    for (value in values) {
        this.append(value)
        this.append(' ')
    }
    if (values.isNotEmpty()) {
        this.deleteCharAt(this.length - 1)
    }
}

/**
 * Append a list of [values] formatted with a [numberFmt] to [this] StringBuilder, separated by spaces.
 */
internal fun StringBuilder.appendValuesList(numberFmt: NumberFormat, values: FloatArray) =
        appendValuesList(Array(values.size) { numberFmt.format(values[it]) })

/**
 * Append a list of [values] rounded to [precision] to [this] StringBuilder, separated by spaces.
 */
internal fun StringBuilder.appendValuesList(precision: Int, values: FloatArray) =
        appendValuesList(getNumberFormat(precision), values)

/**
 * Append a list of [values] to [this] StringBuilder, optimized to be as short as possible.
 * Returns the last formatted value appended.
 */
internal fun StringBuilder.appendValuesListOptimized(lastValue: String?, values: Array<String>): String? {
    var lastVal = lastValue
    for (value in values) {
        // Append space separator if value has no sign '-' or decimal point '.'
        // to separate it from previous value, and if value isn't the first after command.
        if (!lastVal.isNullOrEmpty() && '-' !in value &&
                ('.' !in lastVal || !value.startsWith('.'))) {
            append(' ')
        }

        // Append value.
        append(value)

        lastVal = value
    }
    return lastVal
}

/**
 * Append a list of [values] formatted with a [numberFmt] to [this] StringBuilder, optimized to
 * be as short as possible. Returns the last formatted value appended.
 */
internal fun StringBuilder.appendValuesListOptimized(numberFmt: NumberFormat,
                                                     lastValue: String?, values: FloatArray) =
        appendValuesListOptimized(lastValue, Array(values.size) { values[it].formatOptimized(numberFmt) })

/**
 * Append a list of [values] rounded to [precision] to [this] StringBuilder, optimized to
 * be as short as possible. Returns the last formatted value appended.
 */
internal fun StringBuilder.appendValuesListOptimized(precision: Int, lastValue: String?,
                                                     values: FloatArray) {
    appendValuesListOptimized(getNumberFormat(precision), lastValue, values)
}

/**
 * Create a SVG URL reference from [this] String.
 */
internal fun String?.toSvgUrlReference() = "url(#$this)"
