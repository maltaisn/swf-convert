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
 * @property glyphIndices Indices of each char in the glyphs list of the [font].
 * @property glyphOffsets Custom spacing between glyphs, without including glyph advance width.
 * The first value represents the extra space between the first and the second character of [text].
 * The list has a size between 0 and `text.length - 1`. Trailing zero offsets are omitted.
 * The spacing values are in glyph space units, as defined by [GlyphData.EM_SQUARE_SIZE].
 * Positive values increase spacing, negative values decrease it.
 */
data class TextObject(
    override val id: Int,
    val x: Float,
    val y: Float,
    val fontSize: Float,
    val color: Color,
    val font: Font,
    val text: String,
    val glyphIndices: List<Int>,
    val glyphOffsets: List<Float>
) : FrameObject {

    override fun toString() = "Text[$id](text='$text', x=$x, y=$y, size=$fontSize, " +
            "color=$color, font=${font.name}, glyphOffsets=$glyphOffsets)"
}
