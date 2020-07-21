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

import com.flagstone.transform.DefineTag
import com.flagstone.transform.font.DefineFont
import com.flagstone.transform.font.DefineFont2
import com.flagstone.transform.font.DefineFont3
import com.flagstone.transform.font.DefineFont4
import com.flagstone.transform.font.Kerning
import com.flagstone.transform.shape.Shape
import com.maltaisn.swfconvert.convert.context.ConvertContext
import com.maltaisn.swfconvert.convert.conversionError
import com.maltaisn.swfconvert.core.text.FontScale

internal data class WDefineFont(
    val identifier: Int,
    val name: String,
    val ascent: Int,
    val descent: Int,
    val codes: List<Int>,
    val shapes: List<Shape>,
    val advances: List<Int>,
    val kernings: List<Kerning>,
    val scale: FontScale
) {

    constructor(font: DefineFont2, scale: FontScale) : this(font.identifier, font.name, font.ascent, font.descent,
        font.codes, font.shapes, font.advances, font.kernings, scale)

    constructor(font: DefineFont3, scale: FontScale) : this(font.identifier, font.name, font.ascent, font.descent,
        font.codes, font.shapes, font.advances, font.kernings, scale)

}

internal fun DefineTag.toFontWrapperOrNull(
    context: ConvertContext,
    fontScale2: FontScale,
    fontScale3: FontScale
) = when (this) {
    is DefineFont -> conversionError(context, "Unsupported DefineFont tag")
    is DefineFont2 -> WDefineFont(this, fontScale2)
    is DefineFont3 -> WDefineFont(this, fontScale3)
    is DefineFont4 -> conversionError(context, "Unsupported DefineFont4 tag")
    else -> null
}
