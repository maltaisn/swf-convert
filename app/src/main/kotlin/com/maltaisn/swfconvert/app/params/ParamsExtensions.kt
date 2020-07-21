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

package com.maltaisn.swfconvert.app.params

import com.maltaisn.swfconvert.app.configError
import com.maltaisn.swfconvert.core.image.Color
import java.io.File

/**
 * Attempts to fix [https://github.com/cbeust/jcommander/issues/365].
 * Unknown options are not getting caught, so this must be checked for
 * each parameter of variable arity.
 */
internal fun checkNoOptionsInArgs(args: List<String>) {
    val unknown = args.find { it.startsWith("-") } ?: return
    configError("Unknown option '$unknown'.")
}

internal fun File.isSwfFile() = this.extension.toLowerCase() == "swf"

internal fun String.toBooleanOrNull() = when (this.toLowerCase()) {
    "true" -> true
    "false" -> false
    else -> null
}

@Suppress("MagicNumber")
internal fun String.toColorOrNull(): Color? = when (this.length) {
    7 -> this.substring(1).toIntOrNull(16)?.let { Color(it).opaque }
    9 -> this.substring(1).toIntOrNull(16)?.let { Color(it) }
    else -> null
}

/**
 * Try to parse [this] string as a list with [parseElement] lambda. Lambda should
 * return null if element can't be parsed. List must be delimited with square brackets
 * and elements must be separated with commas.
 */
internal inline fun <T> String.toListOrNull(crossinline parseElement: (String) -> T?): List<T>? {
    if (this.first() != '[' || this.last() != ']') {
        return null
    }
    return this.substring(1, this.length - 1)
        .split(',')
        .map { parseElement(it.trim()) ?: return null }
}
