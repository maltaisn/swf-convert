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

package com.maltaisn.swfconvert.app

import com.maltaisn.swfconvert.core.image.Color
import java.io.File


/**
 * Attempts to fix [https://github.com/cbeust/jcommander/issues/365].
 * Unknown options are not getting caught, so this must be checked for
 * each parameter of variable arity.
 */
fun checkNoOptionsInArgs(args: List<String>) {
    val unknown = args.find { it.startsWith("-") } ?: return
    configError("Unknown option '$unknown'.")
}

fun File.isSwfFile() = this.extension.toLowerCase() == "swf"

fun String.toBooleanOrNull() = when (this.toLowerCase()) {
    "true" -> true
    "false" -> false
    else -> null
}

fun String.toColorOrNull(): Color? = when (this.length) {
    7 -> this.substring(1).toIntOrNull(16)?.let { Color(it).withAlpha(0xFF) }
    9 -> this.substring(1).toIntOrNull(16)?.let { Color(it) }
    else -> null
}

/**
 * Try to parse [this] string as a list with [parseElement] lambda. Lambda should
 * return null if element can't be parsed. List must be delimited with square brackets
 * and elements must be separated with commas.
 */
inline fun <T> String.toListOrNull(crossinline parseElement: (String) -> T?): List<T>? {
    if (this.first() != '[' || this.last() != ']') {
        return null
    }
    return this.substring(1, this.length - 1)
            .split(',')
            .map { parseElement(it.trim()) ?: return null }
}
