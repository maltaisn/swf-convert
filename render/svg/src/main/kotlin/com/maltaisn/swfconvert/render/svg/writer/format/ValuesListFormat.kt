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

import java.text.NumberFormat

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
 * Append a list of [values] to [this] StringBuilder, optimized to be as short as possible
 * by avoiding useless separators between values. Returns the last formatted value appended.
 */
internal fun StringBuilder.appendValuesListOptimized(lastValue: String?, values: Array<String>): String? {
    var lastVal = lastValue
    for (value in values) {
        if (!lastVal.isNullOrEmpty()) {
            val canUseSignAsSeparator = '-' in value // e.g. "12-12" instead of "12 -12".
            val canUsePointAsSeparator = '.' in lastVal && value.startsWith('.') // e.g. ".1.2" instead of ".1 .2".
            if (!canUseSignAsSeparator && !canUsePointAsSeparator) {
                // Neither decimal sign nor decimal point can be used as separator, add a space.
                append(' ')
            }
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
internal fun StringBuilder.appendValuesListOptimized(
    numberFmt: NumberFormat,
    lastValue: String?,
    values: FloatArray
) =
    appendValuesListOptimized(lastValue, Array(values.size) { values[it].formatOptimized(numberFmt) })

/**
 * Append a list of [values] rounded to [precision] to [this] StringBuilder, optimized to
 * be as short as possible. Returns the last formatted value appended.
 */
internal fun StringBuilder.appendValuesListOptimized(
    precision: Int,
    lastValue: String?,
    values: FloatArray
) {
    appendValuesListOptimized(getNumberFormat(precision), lastValue, values)
}
