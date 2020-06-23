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
 * Format [this] float to a String value with [numberFmt].
 */
internal fun Float.format(numberFmt: NumberFormat) = numberFmt.format(this)

internal fun Float.format(precision: Int) = format(getNumberFormat(precision))

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
