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
