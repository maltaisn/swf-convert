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

package com.maltaisn.swfconvert.core

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope


/**
 * Map the values of [this] iterable to a list of type [R] with [block] value.
 *
 * @param parallel Whether to map in parallel or not. The current coroutine context is used.
 * Parallelization provides better performance but it can be unwanted during debugging for example.
 */
suspend fun <T, R> Iterable<T>.mapInParallel(parallel: Boolean = true,
                                             block: suspend (T) -> R): List<R> {
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
