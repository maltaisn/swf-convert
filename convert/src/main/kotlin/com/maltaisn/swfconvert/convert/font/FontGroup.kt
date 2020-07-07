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
