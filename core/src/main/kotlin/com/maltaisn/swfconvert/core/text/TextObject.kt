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

package com.maltaisn.swfconvert.core.text

import com.maltaisn.swfconvert.core.FrameObject
import com.maltaisn.swfconvert.core.image.Color


/**
 * A object for text in the intermediate representation.
 * The is drawn from its bottom left corner at [x] and [y] coordinates.
 *
 * @property fontSize Size of the font to use, in twips.
 * @property color Color of the text to draw.
 * @property font Font to use.
 * @property text Text to draw.
 * @property glyphOffsets Custom spacing between glyphs, without including glyph advance width.
 * The first value represents the extra space between the first and the second character of [text].
 * The list should have either a size of 0 (no custom spacing) or `text.length - 1`.
 * The spacing values are in glyph space units, as defined by [GlyphData.EM_SQUARE_SIZE].
 * Positive values increase spacing, negative values decrease it.
 */
data class TextObject(override val id: Int,
                      val x: Float,
                      val y: Float,
                      val fontSize: Float,
                      val color: Color,
                      val font: Font,
                      val text: String,
                      val glyphOffsets: List<Float>) : FrameObject(id) {

    override fun toString() = "Text[$id](text='$text', x=$x, y=$y, size=$fontSize, " +
            "color=$color, font=${font.name}, glyphOffsets=$glyphOffsets)"

}
