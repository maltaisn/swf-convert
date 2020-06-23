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

import kotlin.contracts.contract

/**
 * To be thrown when a configuration parameter value is wrong.
 */
class ConfigException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

fun configError(message: String, cause: Throwable? = null): Nothing =
    throw ConfigException(message, cause)

inline fun configError(condition: Boolean, message: () -> String) {
    contract {
        returns() implies condition
    }
    if (!condition) {
        throw ConfigException(message())
    }
}
