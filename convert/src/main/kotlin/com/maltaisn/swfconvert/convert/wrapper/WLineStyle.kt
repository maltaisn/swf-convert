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

import com.flagstone.transform.datatype.Color
import com.flagstone.transform.fillstyle.FillStyle
import com.flagstone.transform.linestyle.CapStyle
import com.flagstone.transform.linestyle.JoinStyle
import com.flagstone.transform.linestyle.LineStyle
import com.flagstone.transform.linestyle.LineStyle1
import com.flagstone.transform.linestyle.LineStyle2
import com.maltaisn.swfconvert.convert.context.ConvertContext
import com.maltaisn.swfconvert.convert.conversionError

internal data class WLineStyle(
    val color: Color,
    val width: Int,
    val capStyle: CapStyle?,
    val joinStyle: JoinStyle?,
    val miterLimit: Int,
    val fillStyle: FillStyle?
) {

    constructor(style: LineStyle1) : this(style.color, style.width, null,
        null, 0, null)

    constructor(style: LineStyle2) : this(style.color, style.width, style.startCap,
        style.joinStyle, style.miterLimit, style.fillStyle)

}

internal fun LineStyle.toLineStyleWrapperOrNull(context: ConvertContext) = when (this) {
    is LineStyle1 -> WLineStyle(this)
    is LineStyle2 -> {
        conversionError(this.fillStyle == null, context) {
            "Unsupported line fill style"
        }
        conversionError(this.startCap == this.endCap, context) {
            "Unsupported different start and end caps on line style"
        }
        WLineStyle(this)
    }
    else -> null
}
