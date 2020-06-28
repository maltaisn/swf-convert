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
import com.maltaisn.swfconvert.convert.context.SwfGlyphContext
import com.maltaisn.swfconvert.convert.context.SwfObjectContext
import com.maltaisn.swfconvert.convert.shape.ShapeConverter
import com.maltaisn.swfconvert.convert.wrapper.WDefineFont
import com.maltaisn.swfconvert.core.shape.Path
import com.maltaisn.swfconvert.core.shape.PathElement
import com.maltaisn.swfconvert.core.shape.PathElement.MoveTo
import com.maltaisn.swfconvert.core.text.GlyphData
import org.apache.logging.log4j.kotlin.logger
import java.awt.geom.AffineTransform
import java.io.IOException
import javax.inject.Inject

/**
 * Class used to create [GlyphData] from a SWF font tag.
 */
internal class GlyphPathParser @Inject constructor(
    private val converter: ShapeConverter
) {

    private val logger = logger()

    fun createFontGlyphsData(context: SwfObjectContext, wfont: WDefineFont): List<GlyphData> {
        val transform = AffineTransform.getScaleInstance(
            wfont.scale.scaleX.toDouble() * SWF_TO_TTF_EM_SCALE,
            wfont.scale.scaleY.toDouble() * SWF_TO_TTF_EM_SCALE)

        return wfont.codes.indices.map { i ->
            val glyphContext = SwfGlyphContext(context, i)
            val advance = if (wfont.advances.isEmpty()) {
                // FontFlagsHasLayout is false
                0f
            } else {
                wfont.advances[i] * wfont.scale.scaleX
            }

            val shapeData = wfont.shapes[i].objects.first() as ShapeData
            val contours = if (shapeData.data.size == 1) {
                // swftools sometimes create empty SHAPE records with a single byte, where as shapes with no
                // record should have at least a EndShapeRecord (6 x 0 bits) according to the SWF reference.
                // transform-swf doesn't support those shapes, so it's handled separatedly.
                emptyList()
            } else try {
                // transform-swf lazily decodes glyph data with a [ShapeData] record
                // that must be decoded later to a shape.
                val shape = Shape.shapeFromData(shapeData)
                parseGlyphShape(glyphContext, shape, transform)
            } catch (e: IOException) {
                logger.error(e) { "Could not parse glyph shape data at $glyphContext" }
                emptyList()
            }

            GlyphData(advance, contours)
        }
    }

    private fun parseGlyphShape(
        context: SwfGlyphContext,
        shape: Shape,
        transform: AffineTransform
    ): List<Path> {
        // Create the glyph shape from SWF shape records
        val paths = converter.parseShape(context, shape,
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
