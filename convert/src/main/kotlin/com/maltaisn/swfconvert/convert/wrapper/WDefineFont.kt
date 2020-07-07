/*
 * Copyright 2020 Nicolas Maltais
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
