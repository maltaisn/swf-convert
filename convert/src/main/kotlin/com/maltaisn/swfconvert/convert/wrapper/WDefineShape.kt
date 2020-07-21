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

package com.maltaisn.swfconvert.convert.wrapper

import com.flagstone.transform.MovieTag
import com.flagstone.transform.datatype.Bounds
import com.flagstone.transform.fillstyle.FillStyle
import com.flagstone.transform.linestyle.LineStyle
import com.flagstone.transform.shape.DefineShape
import com.flagstone.transform.shape.DefineShape2
import com.flagstone.transform.shape.DefineShape3
import com.flagstone.transform.shape.DefineShape4
import com.flagstone.transform.shape.Shape

internal data class WDefineShape(
    val identifier: Int,
    val shape: Shape,
    val bounds: Bounds,
    val fillStyles: List<FillStyle>,
    val lineStyles: List<LineStyle>
) {

    constructor(s: DefineShape) : this(s.identifier, s.shape, s.bounds, s.fillStyles, s.lineStyles)
    constructor(s: DefineShape2) : this(s.identifier, s.shape, s.bounds, s.fillStyles, s.lineStyles)
    constructor(s: DefineShape3) : this(s.identifier, s.shape, s.bounds, s.fillStyles, s.lineStyles)
    constructor(s: DefineShape4) : this(s.identifier, s.shape, s.bounds, s.fillStyles, s.lineStyles)

}

internal fun MovieTag.toShapeWrapperOrNull() = when (this) {
    is DefineShape -> WDefineShape(this)
    is DefineShape2 -> WDefineShape(this)
    is DefineShape3 -> WDefineShape(this)
    is DefineShape4 -> WDefineShape(this)
    else -> null
}
