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

package com.maltaisn.swfconvert.core

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Utiltiy function to convert [this] integer to a lowercase unsigned hexadecimal string.
 * Mostly to avoid having to use the "magic number" 16 everywhere.
 */
@Suppress("MagicNumber")
fun Int.toHexString(): String = Integer.toHexString(this)

/**
 * Map the values of [this] iterable to a list of type [R] with [block] value.
 *
 * @param parallel Whether to map in parallel or not. The current coroutine context is used.
 * Parallelization provides better performance but it can be unwanted during debugging for example.
 */
suspend fun <T, R> Iterable<T>.mapInParallel(
    parallel: Boolean = true,
    block: suspend (T) -> R
): List<R> {
    return if (parallel) {
        coroutineScope {
            this@mapInParallel.map {
                async {
                    block(it)
                }
            }.awaitAll()
        }
    } else {
        this.map { e ->
            block(e)
        }
    }
}
