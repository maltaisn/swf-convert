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
 * Context for an object in SWF being converted.
 * Object ID is the last of [ids], which form the tree of object IDs leading to this object.
 */
internal class SwfObjectContext(
    parent: SwfFileContext,
    private val ids: List<Int>
) : ConvertContext(parent) {

    override val description: String
        get() = buildString {
            if (ids.isEmpty()) {
                append("root")
            } else {
                append("object ID ")
                append(ids.last())
                if (ids.size > 1) {
                    append(" (${ids.joinToString(" > ")})")
                }
            }
        }
}
