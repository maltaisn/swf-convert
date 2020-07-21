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

import com.flagstone.transform.fillstyle.FillStyle
import com.flagstone.transform.linestyle.LineStyle
import com.flagstone.transform.shape.ShapeRecord
import com.flagstone.transform.shape.ShapeStyle
import com.flagstone.transform.shape.ShapeStyle2

internal data class WShapeStyle(
    val moveX: Int?,
    val moveY: Int?,
    val fillStyle0: Int?,
    val fillStyle1: Int?,
    val lineStyle: Int?,
    val fillStyles: List<FillStyle>,
    val lineStyles: List<LineStyle>
) {

    constructor(style: ShapeStyle) : this(style.moveX, style.moveY, style.fillStyle,
        style.altFillStyle, style.lineStyle, style.fillStyles, style.lineStyles)

    constructor(style: ShapeStyle2) : this(style.moveX, style.moveY, style.fillStyle,
        style.altFillStyle, style.lineStyle, style.fillStyles, style.lineStyles)

}

internal fun ShapeRecord.toShapeStyleWrapperOrNull() = when (this) {
    is ShapeStyle -> WShapeStyle(this)
    is ShapeStyle2 -> WShapeStyle(this)
    else -> null
}
