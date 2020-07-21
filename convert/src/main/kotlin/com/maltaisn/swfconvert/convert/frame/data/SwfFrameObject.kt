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
import com.maltaisn.swfconvert.convert.wrapper.WPlace

internal sealed class SwfFrameObject {
    abstract val context: SwfObjectContext
    abstract val id: Int
    abstract val place: WPlace
    abstract val tag: DefineTag
}

internal data class SwfObject(
    override val context: SwfObjectContext,
    override val id: Int,
    override val place: WPlace,
    override val tag: DefineTag
) : SwfFrameObject()

internal data class SwfSprite(
    override val context: SwfObjectContext,
    override val id: Int,
    override val place: WPlace,
    override val tag: DefineTag,
    override val objects: List<SwfFrameObject>
) : SwfFrameObject(),
    SwfObjectGroup
