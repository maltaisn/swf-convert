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

package com.maltaisn.swfconvert.convert.font

import com.maltaisn.swfconvert.core.text.BaseFont
import com.maltaisn.swfconvert.core.text.Font
import com.maltaisn.swfconvert.core.text.FontGlyph
import com.maltaisn.swfconvert.core.text.FontMetrics
import java.io.File

/**
 * Represents a group of [fonts] objects merged into a single group,
 * that share the same info and the same glyphs.
 */
internal data class FontGroup(
    override var name: String,
    override val metrics: FontMetrics,
    val fonts: MutableList<Font>,
    val glyphsMap: MutableMap<Char, FontGlyph>
) : BaseFont {

    override val glyphs: List<FontGlyph>
        get() = glyphsMap.values.toList()

    override var fontFile: File? = null

    private val isAllWhitespaces: Boolean
        get() = glyphsMap.values.all { it.isWhitespace }

    fun isCompatibleWith(other: FontGroup, requireCommon: Boolean): Boolean {
        return when {
            other.glyphs.size < glyphs.size -> {
                // Use group with the least glyph as comparison base, it'll be faster.
                other.isCompatibleWith(this, requireCommon)
            }
            isAllWhitespaces || other.isAllWhitespaces -> {
                // One font or the other has only whitespace. Since all whitespace is converted
                // to identical spaces, fonts are automatically compatible.
                true
            }
            metrics == other.metrics -> {
                // Both font have same metrics, check glyphs.
                // If they have incompatible chars, they can't be merged.
                // Otherwise check if they have at least one common char (if required).
                !hasIncompatibleCharWith(other) &&
                        (!requireCommon || hasCommonCharWith(other))
            }
            else -> false
        }
    }

    /**
     * Returns `true` if this group has a common non-whitespace char with the [other] group.
     */
    private fun hasCommonCharWith(other: FontGroup) =
        glyphsMap.entries.find { (char, glyph) ->
            !glyph.isWhitespace && glyph == other.glyphsMap[char]
        } != null

    /**
     * Returns `true` if this group has a char with the same code
     * as another char in [other], but different contours.
     * */
    private fun hasIncompatibleCharWith(other: FontGroup) =
        glyphsMap.entries.find { (char, glyph) ->
            val otherGlyph = other.glyphsMap[char]
            otherGlyph != null && glyph != otherGlyph
        } != null

    fun merge(font: FontGroup) {
        glyphsMap += font.glyphsMap
        fonts += font.fonts
    }

    override fun toString() = "FontGroup{name=$name, metrics=$metrics, " +
            "${fonts.size} fonts, ${glyphs.size} glyphs}"

}
