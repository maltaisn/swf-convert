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

import com.flagstone.transform.Movie
import com.flagstone.transform.font.DefineFont
import com.flagstone.transform.font.DefineFont2
import com.flagstone.transform.font.DefineFont3
import com.flagstone.transform.font.DefineFont4
import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.convert.conversionError
import com.maltaisn.swfconvert.convert.wrapper.WDefineFont
import com.maltaisn.swfconvert.core.text.*
import java.io.File
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject


internal class FontConverter @Inject constructor(
        private val config: ConvertConfiguration,
        private val glyphPathParser: GlyphPathParser,
        private val glyphOcr: GlyphOcr,
        private val fontBuilder: FontBuilder
) {

    private val unknownCharsMap = mutableMapOf<GlyphData, Char>()
    private var nextUnknownCharCode = 0

    private val ocrTempDir = File(config.fontsDir, "ocr")

    /**
     * Each SWF file has its own fonts, which are sometimes subsetted.
     * To reduce file size when converting multiple SWFs to a single file, the number of fonts
     * is reduced by merging them. Fonts with common glyph shapes are grouped as a single font.
     * This grouping is not perfect, but can sometimes reduce the number of fonts by 50x-100x.
     */
    fun createFontGroups(swfs: List<Movie>): List<FontGroup> {
        // Create fonts for each font tag in each file.
        val allFonts = createAllFonts(swfs)

        // Merge fonts with the same name if they are compatible.
        print("Creating fonts: merging fonts\r")
        val groups = mergeFonts(allFonts)
        if (config.groupFonts) {
            val ratio = (allFonts.size - groups.size) / allFonts.size.toFloat()
            println("Creating fonts: ${groups.size} font groups created from ${allFonts.size} " +
                    "fonts (-${PERCENT_FMT.format(ratio)})")
        }

        // Assign unique name to each group
        assignUniqueFontNames(groups)

        return groups
    }

    /**
     * Create the TTF font files for a list of font groups.
     */
    fun createFontFiles(fonts: List<BaseFont>) {
        print("Creating fonts: building TTF fonts\r")
        val tempDir = File(config.fontsDir, "temp")
        tempDir.mkdirs()
        for (font in fonts) {
            fontBuilder.buildFont(font, config.fontsDir, tempDir)
        }
        tempDir.deleteRecursively()
        println()
    }

    /**
     * Take a list of font groups and ungroup them to the original fonts,
     * so they have the original glyph indices but they share the same TTF font.
     */
    fun ungroupFonts(groups: List<FontGroup>): Map<FontId, Font> {
        val fonts = mutableMapOf<FontId, Font>()
        for (group in groups) {
            for (font in group.fonts) {
                font.name = group.name
                font.fontFile = group.fontFile
                fonts[font.id] = font
            }
        }
        return fonts
    }

    private fun createAllFonts(swfs: List<Movie>): List<Font> {
        val fonts = mutableListOf<Font>()

        unknownCharsMap.clear()
        nextUnknownCharCode = FIRST_CODE_FOR_UNKNOWN

        for ((i, swf) in swfs.withIndex()) {
            fonts += createSwfFonts(swf, i)
            print("Creating fonts: parsing all fonts, file ${i + 1} / ${swfs.size}\r")
        }
        println()
        return fonts
    }

    private fun createSwfFonts(swf: Movie, fileIndex: Int): List<Font> {
        val fonts = mutableListOf<Font>()
        for (obj in swf.objects) {
            val wfont = when (obj) {
                is DefineFont -> conversionError("Unsupported define font tag")
                is DefineFont2 -> WDefineFont(obj)
                is DefineFont3 -> WDefineFont(obj)
                is DefineFont4 -> conversionError("Unsupported define font tag")
                else -> null
            }
            if (wfont != null) {
                conversionError(wfont.kernings.isEmpty()) { "Unsupported font kerning" }

                // Create glyphs
                val codes = mutableSetOf<Char>()
                val glyphs = mutableListOf<FontGlyph>()
                val glyphsData = glyphPathParser.createFontGlyphsData(wfont)
                for ((i, code) in wfont.codes.withIndex()) {
                    val glyph = createFontGlyph(glyphsData[i], code, codes)
                    glyphs += glyph
                    codes += glyph.char
                }

                // Create font
                val scale = wfont.scale
                val fontId = FontId(fileIndex, wfont.identifier)
                val metrics = FontMetrics(wfont.ascent * scale.scaleX,
                        wfont.descent * scale.scaleX, scale)
                fonts += Font(fontId, wfont.name, metrics, glyphs)
            }
        }
        return fonts
    }

    private fun createFontGlyph(data: GlyphData, code: Int,
                                assignedCodes: Set<Char>): FontGlyph {
        var char = code.toChar()
        when {
            data.isWhitespace -> {
                // Most whitespaces are discarded when TTF is built, and the others are converted to
                // spaces by the text renderer, so use only spaces. Advance width used here is the
                // default used for a space, but that doesn't have much importance since the text
                // renderer will set it manually anyway.
                return FontGlyph(' ', GlyphData(GlyphData.WHITESPACE_ADVANCE_WIDTH, emptyList()))
            }
            char in assignedCodes ||
                    char.isWhitespace() ||
                    char in '\u0000'..'\u001F' ||
                    char in '\uFFF0'..'\uFFFF' -> {
                // This will happen if:
                // - Duplicate code in font, meaning code was already assigned.
                // - Glyph should be a whitespace but it isn't. Ligatures and other unicode characters
                //     are sometimes replaced by an extended ASCII equivalent, often a space.
                // - Control chars: they don't get added to TTF fonts by doubletype.
                // - Specials unicode block chars: they don't seem to work well with PDFBox.
                // So in all these cases, a new code is used.

                val assigned = unknownCharsMap[data]
                if (assigned != null) {
                    // Glyph data is already assigned to a code in other fonts, try it.
                    if (assigned !in assignedCodes) {
                        char = assigned
                    } else {
                        // The existingly used code for this data can be used for this font,
                        // because it's already assigned. Use a new code.
                        char = nextUnknownCharCode.toChar()
                        nextUnknownCharCode++
                    }

                } else {
                    val ocrChar = if (config.ocrDetectGlyphs) {
                        // Try to recognize char with OCR
                        glyphOcr.recognizeGlyphData(data, ocrTempDir)
                    } else {
                        null
                    }
                    if (ocrChar == null || ocrChar in assignedCodes) {
                        // Char couldn't be recognized or recognized char is already assigned.
                        // Use a new code.
                        char = nextUnknownCharCode.toChar()
                        nextUnknownCharCode++
                    } else {
                        char = ocrChar
                    }
                    // Remember the char assigned for this data so the work doesn't have to be
                    // done for another identical char. This also increases mergeability.
                    unknownCharsMap[data] = char
                }
            }
        }

        return FontGlyph(char, data)
    }


    private fun mergeFonts(allFonts: List<Font>): List<FontGroup> {
        val fontsByName = allFonts.groupBy { it.name }
        val allGroups = mutableListOf<FontGroup>()
        for (fonts in fontsByName.values) {
            // Create font groups
            val groups = fonts.map { font ->
                FontGroup(font.name, font.metrics, mutableListOf(font),
                        font.glyphs.associateByTo(mutableMapOf()) { it.char })
            }

            // Merge groups
            allGroups += mergeFontGroups(groups, true)
        }
        // Merge again in case two fonts with different names are the same.
        // Also since this is the last merge, merge even if fonts have no common chars.
        return mergeFontGroups(allGroups, false)
    }

    private fun mergeFontGroups(groups: List<FontGroup>,
                                requireCommon: Boolean): List<FontGroup> {
        if (!config.groupFonts) {
            return groups
        }

        val newGroups = mutableListOf<FontGroup>()
        for (group in groups) {
            var wasMerged = false
            for (newGroup in newGroups) {
                if (group.isCompatibleWith(newGroup, requireCommon)) {
                    // Both fonts are compatible, merge them.
                    newGroup.merge(group)
                    wasMerged = true
                    break
                }
            }
            if (!wasMerged) {
                // Both fonts aren't compatible, add new global font.
                newGroups += group
            }
        }
        return if (groups.size != newGroups.size) {
            // Number of fonts decreased, continue merging. This is necessary since two fonts may 
            // not be mergeable at first if they don't have common characters.
            mergeFontGroups(newGroups, requireCommon)
        } else {
            newGroups
        }
    }

    private fun assignUniqueFontNames(groups: List<FontGroup>) {
        val assignedNames = mutableSetOf<String>()
        for (group in groups) {
            var name = group.name.replace(' ', '-').toLowerCase()
            if (name.isEmpty()) {
                name = UUID.randomUUID().toString()
            }
            if (name in assignedNames) {
                var i = 2
                while ("$name-$i" in assignedNames) {
                    i++
                }
                name = "$name-$i"
            }
            group.name = name
            assignedNames += name
        }
    }

    companion object {
        private const val FIRST_CODE_FOR_UNKNOWN = 0xE000

        private val PERCENT_FMT = NumberFormat.getPercentInstance().apply {
            maximumFractionDigits = 2
        }
    }

}
