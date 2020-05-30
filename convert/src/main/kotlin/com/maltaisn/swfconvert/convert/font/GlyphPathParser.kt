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

import com.flagstone.transform.shape.Shape
import com.flagstone.transform.shape.ShapeData
import com.maltaisn.swfconvert.convert.shape.ShapeConverter
import com.maltaisn.swfconvert.convert.wrapper.WDefineFont
import com.maltaisn.swfconvert.core.shape.Path
import com.maltaisn.swfconvert.core.shape.PathElement
import com.maltaisn.swfconvert.core.shape.PathElement.MoveTo
import com.maltaisn.swfconvert.core.text.GlyphData
import java.awt.geom.AffineTransform
import javax.inject.Inject


/**
 * Class used to create [GlyphData] from a SWF font tag.
 */
internal class GlyphPathParser @Inject constructor(
        private val converter: ShapeConverter
) {

    fun createFontGlyphsData(wfont: WDefineFont): List<GlyphData> {
        val transform = AffineTransform.getScaleInstance(
                wfont.scale.scaleX.toDouble() * SWF_TO_TTF_EM_SCALE,
                wfont.scale.scaleY.toDouble() * SWF_TO_TTF_EM_SCALE)

        return wfont.codes.indices.map { i ->
            val advance = wfont.advances[i] * wfont.scale.scaleX
            val shapeData = wfont.shapes[i].objects.first() as ShapeData
            val shape = Shape.shapeFromData(shapeData)
            val contours = parseGlyphShape(shape, transform)
            GlyphData(advance, contours)
        }
    }

    private fun parseGlyphShape(shape: Shape, transform: AffineTransform): List<Path> {
        // Create the glyph shape from SWF shape records
        val paths = converter.parseShape(shape,
                emptyList(), emptyList(),
                transform, IDENTITY_TRANSFORM,
                ignoreStyles = true, allowRectangles = false)

        // Separate glyph shape into contours, each having a single move to element.
        val contours = mutableListOf<Path>()
        for (path in paths) {
            val elements = mutableListOf<PathElement>()
            for (element in path.elements) {
                if (element is MoveTo && elements.isNotEmpty()) {
                    contours += Path(elements.toList(), path.fillStyle)
                    elements.clear()
                }
                elements += element
            }
            contours += Path(elements.toList(), path.fillStyle)
        }

        return contours
    }


    companion object {
        private val IDENTITY_TRANSFORM = AffineTransform()

        /** Size of the SWF EM square. See Chapter 10: Fonts and Text, The EM square. */
        private const val SWF_EM_SQUARE_SIZE = 1024f

        /** Scale factor between the SWF EM square size and the IR EM square. */
        private const val SWF_TO_TTF_EM_SCALE = GlyphData.EM_SQUARE_SIZE / SWF_EM_SQUARE_SIZE
    }

}