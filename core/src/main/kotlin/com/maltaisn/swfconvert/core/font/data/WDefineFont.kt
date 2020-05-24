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

package com.maltaisn.swfconvert.core.font.data

import com.flagstone.transform.font.DefineFont2
import com.flagstone.transform.font.DefineFont3
import com.flagstone.transform.font.Kerning
import com.flagstone.transform.shape.Shape


internal data class WDefineFont(val identifier: Int,
                                val name: String,
                                val ascent: Int,
                                val descent: Int,
                                val codes: List<Int>,
                                val shapes: List<Shape>,
                                val advances: List<Int>,
                                val kernings: List<Kerning>,
                                val scale: FontScale) {

    constructor(font: DefineFont2) : this(font.identifier, font.name, font.ascent, font.descent,
            font.codes, font.shapes, font.advances, font.kernings, DEFINEFONT2_SCALE)

    constructor(font: DefineFont3) : this(font.identifier, font.name, font.ascent, font.descent,
            font.codes, font.shapes, font.advances, font.kernings, DEFINEFONT3_SCALE)

    companion object {
        // TODO allow custom values
        private val DEFINEFONT2_SCALE = FontScale(0.05f, 0.05f, 20f, -20f)
        private val DEFINEFONT3_SCALE = FontScale(0.05f, -0.05f, 1f, 1f)
    }

}
