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

package com.maltaisn.swfconvert.convert

import com.maltaisn.swfconvert.convert.context.ConvertContext
import kotlin.contracts.contract

class ConversionError(
    val context: ConvertContext,
    message: String? = ""
) : IllegalStateException("$message, context: $context")

internal fun conversionError(context: ConvertContext, message: String): Nothing =
    throw ConversionError(context, message)

internal inline fun conversionError(condition: Boolean, context: ConvertContext, message: () -> String) {
    contract {
        returns() implies condition
    }
    if (!condition) {
        throw ConversionError(context, message())
    }
}
