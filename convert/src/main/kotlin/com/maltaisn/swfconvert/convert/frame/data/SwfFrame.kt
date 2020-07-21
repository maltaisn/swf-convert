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

package com.maltaisn.swfconvert.convert.frame.data

import com.flagstone.transform.DefineTag
import com.maltaisn.swfconvert.convert.context.SwfObjectContext

internal data class SwfFrame(
    override val context: SwfObjectContext,
    override val id: Int,
    val dictionary: SwfDictionary,
    val width: Int,
    val height: Int,
    override val objects: List<SwfFrameObject>
) : SwfObjectGroup

internal typealias SwfDictionary = Map<Int, DefineTag>
