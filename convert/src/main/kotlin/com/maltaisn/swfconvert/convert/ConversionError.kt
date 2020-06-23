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

package com.maltaisn.swfconvert.convert

import com.maltaisn.swfconvert.convert.context.ConvertContext
import kotlin.contracts.contract

class ConversionError(
    val context: ConvertContext,
    message: String? = ""
) : IllegalStateException("$message, context: $context")

fun conversionError(context: ConvertContext, message: String): Nothing =
    throw ConversionError(context, message)

inline fun conversionError(condition: Boolean, context: ConvertContext, message: () -> String) {
    contract {
        returns() implies condition
    }
    if (!condition) {
        throw ConversionError(context, message())
    }
}
