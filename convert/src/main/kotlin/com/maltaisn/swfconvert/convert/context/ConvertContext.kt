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

package com.maltaisn.swfconvert.convert.context

/**
 * Passed around conversion classes and methods to keep track of the file and objects being
 * currently converted to intermediate representation.
 */
abstract class ConvertContext(val parent: ConvertContext?) {

    /**
     * Provides a text description of this context, without describing the parent.
     */
    abstract val description: String

    override fun toString() = buildString {
        var context: ConvertContext? = this@ConvertContext
        while (context != null) {
            insert(0, ", ")
            insert(0, context.description)
            context = context.parent
        }
        delete(length - 2, length)
    }

}
