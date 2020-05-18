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

import kotlin.contracts.contract


class ConversionError(message: String? = "") : IllegalStateException(message)

fun conversionError(message: String): Nothing = throw ConversionError(message)

fun conversionError(condition: Boolean) {
    contract {
        returns() implies condition
    }
    if (!condition) {
        throw ConversionError()
    }
}

inline fun conversionError(condition: Boolean, message: () -> String) {
    contract {
        returns() implies condition
    }
    if (!condition) {
        throw ConversionError(message())
    }
}
