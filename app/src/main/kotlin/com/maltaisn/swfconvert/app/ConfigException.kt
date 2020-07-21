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

package com.maltaisn.swfconvert.app

import kotlin.contracts.contract

/**
 * To be thrown when a configuration parameter value is wrong.
 */
internal class ConfigException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

internal fun configError(message: String, cause: Throwable? = null): Nothing =
    throw ConfigException(message, cause)

internal inline fun configError(condition: Boolean, message: () -> String) {
    contract {
        returns() implies condition
    }
    if (!condition) {
        throw ConfigException(message())
    }
}
