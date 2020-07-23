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

/**
 * A set of scale values used to convert font tags.
 *
 * - `scaleX` and `scaleY` refer to the scale factor applied on the glyph paths to transform them into glyph space.
 * Glyph space is a square of [GlyphData.EM_SQUARE_SIZE] units. `scaleY` must also include the negation of the Y
 * coordinate since SWF glyphs have the Y axis pointing down while glyphs in intermediate representation and in TTF have
 * the Y axis pointing up. For example, the `DefineFont3` tag in SWF uses a EM square size of 20,480 (1024 x 20),
 * so `scaleX` is `0.05` and `scaleY is `-0.05`.
 *
 * - `unscaleX` and `unscaleY` refer to the scale factor applied on text objects using the font. By default, both
 * values are always set to `1`.
 *
 * The need for "unscale" values arises from the fact that swf-tools' `pdf2swf` scales all DefineFont2 glyphs by 20,
 * and inverts the Y axis. All text is scaled back with a transform matrix when placing it.
 * For swf-tools purposes this doesn't matter because text can't be selected in a SWf file, so everything appears fine.
 * When converting these SWF files using default values, everything also appears fine. However, text selection (in a PDF
 * viewer for example) will appear inverted and 20x too small, creating a very bad user experience.
 *
 * Why does swf-tools do that? Probably because in the early versions of Flash the DefineFont3 didn't exist and this was
 * the only way of increasing the glyphs precision. (I don't know why Y is inverted though)
 *
 * To counteract this, it was made possible to use custom font scale values for these files. By using a scale of 0.05,
 * the 20x scale is cancelled out, and by using 20x unscale, the transform applied on text is also cancelled out.
 * (`FontScale(0.05, 0.05, 20, -20)`).
 */
data class FontScale(
    val scaleX: Float,
    val scaleY: Float,
    val unscaleX: Float,
    val unscaleY: Float
)
