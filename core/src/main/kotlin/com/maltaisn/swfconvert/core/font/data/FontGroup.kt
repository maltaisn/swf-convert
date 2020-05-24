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
        if (other.glyphs.size < glyphs.size) {
            // Use group with the least glyph as comparison base.
            return other.isCompatibleWith(this, requireCommon)
        }
        return when {
            isAllWhitespaces || other.isAllWhitespaces -> {
                // One font or the other has only whitespace. Since all whitespace is converted
                // to identical spaces, fonts are automatically compatible.
                true
            }
            metrics == other.metrics -> {
                // Both font have same metrics, check glyphs.
                var hasCommonChar = false
                for ((char, glyph) in glyphsMap) {
                    val otherGlyph = other.glyphsMap[char]
                    if (otherGlyph != null) {
                        if (glyph != otherGlyph) {
                            // Two glyphs with same character but different shape, so these two fonts are different.
                            return false
                        } else if (!glyph.isWhitespace) {
                            hasCommonChar = true
                        }
                    }
                }
                // If no character is common between the two fonts, we can't say if they
                // are compatible, so it's better to assume they're not for the moment, to
                // make further merging more efficient.
                hasCommonChar || !requireCommon
            }
            else -> false
        }
    }

    fun merge(font: FontGroup) {
        glyphsMap += font.glyphsMap
        fonts += font.fonts
    }

    override fun toString() = "FontGroup{name=$name, metrics=$metrics, " +
            "${fonts.size} fonts, ${glyphs.size} glyphs}"

}
