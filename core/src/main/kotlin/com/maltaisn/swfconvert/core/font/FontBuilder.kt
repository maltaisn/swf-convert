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

package com.maltaisn.swfconvert.core.font

import com.maltaisn.swfconvert.core.font.data.FontGlyph
import com.maltaisn.swfconvert.core.font.data.GlyphData
import com.maltaisn.swfconvert.core.shape.path.PathElement.ClosePath
import com.maltaisn.swfconvert.core.shape.path.PathElement.QuadTo
import com.maltaisn.swfconvert.core.validateFilename
import org.doubletype.ossa.Engine
import org.doubletype.ossa.adapter.EContour
import org.doubletype.ossa.adapter.EContourPoint
import java.io.File
import java.util.*
import kotlin.math.roundToInt


/**
 * Based on ffdec, doubletype code was taken there too.
 * [https://github.com/jindrapetrik/jpexs-decompiler/blob/bf2a413725c09eecded4e8f42af4487ecd1842a5/libsrc/ttf/src/fontastic/Fontastic.java]
 */
data class FontBuilder(val name: String) {

    val glyphs = mutableMapOf<Char, FontGlyph>()

    var ascent = 0f
    var descent = 0f

    private val engine = Engine()


    fun build(tempDir: File): File {
        engine.buildNewTypeface(name, tempDir)

        // Set typeface attributes
        engine.typeface.let {
            // No scale is done between the intermediate representation EM square and the one
            // used by doubletype. So make sure they are the same.
            assert(it.em == GlyphData.EM_SQUARE_SIZE.toDouble())

            it.ascender = ascent.toDouble().coerceIn(0.0, it.em)
            it.descender = descent.toDouble().coerceIn(0.0, it.em)

            it.fontFamilyName = name
            it.version = TYPEFACE_VERSION
            it.license = TYPEFACE_LICENSE
            it.author = TYPEFACE_AUTHOR
            it.copyrightYear = TYPEFACE_COPYRIGHT
            it.creationDate = TYPEFACE_DATE
            it.modificationDate = TYPEFACE_DATE
        }

        // Add glyphs to typeface
        for ((char, glyph) in glyphs) {
            val code = char.toLong()
            val data = glyph.data
            engine.checkUnicodeBlock(code)
            val glyphFile = engine.addNewGlyph(code)
            glyphFile.advanceWidth = data.advanceWidth.roundToInt()

            for (contour in data.contours) {
                val econtour = EContour()
                econtour.type = EContour.k_quadratic
                for (e in contour.elements) {
                    if (e is ClosePath) {
                        continue
                    }
                    val epoint = EContourPoint(e.x.toDouble(), e.y.toDouble(), true)
                    if (e is QuadTo) {
                        econtour.addContourPoint(EContourPoint(e.cx.toDouble(), e.cy.toDouble(), false))
                    }
                    // Note: epoint.controlPoint1 only works partially--sometimes it isn't added correctly.
                    econtour.addContourPoint(epoint)
                }
                glyphFile.addContour(econtour)
            }
        }
        engine.typeface.addRequiredGlyphs()

        // Build type face
        engine.buildTrueType()
        return File(tempDir, validateFilename("$name.ttf"))
    }

    companion object {
        private val TYPEFACE_DATE = Date(946684800000)  // 2000-01-01 00:00:00 UTC
        private const val TYPEFACE_COPYRIGHT = "2000"
        private const val TYPEFACE_LICENSE = ""
        private const val TYPEFACE_VERSION = "1.0"
        private const val TYPEFACE_AUTHOR = "unknown"
    }

}
