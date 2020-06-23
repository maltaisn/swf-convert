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

package com.maltaisn.swfconvert.convert.frame

import com.flagstone.transform.text.GlyphIndex
import com.flagstone.transform.text.StaticTextTag
import com.flagstone.transform.text.TextSpan
import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.convert.context.SwfFileContext
import com.maltaisn.swfconvert.convert.context.SwfObjectContext
import com.maltaisn.swfconvert.convert.conversionError
import com.maltaisn.swfconvert.convert.image.CompositeColorTransform
import com.maltaisn.swfconvert.convert.toAffineTransformOrIdentity
import com.maltaisn.swfconvert.convert.toColor
import com.maltaisn.swfconvert.core.FrameObject
import com.maltaisn.swfconvert.core.GroupObject
import com.maltaisn.swfconvert.core.YAxisDirection
import com.maltaisn.swfconvert.core.image.Color
import com.maltaisn.swfconvert.core.shape.Path
import com.maltaisn.swfconvert.core.shape.PathElement
import com.maltaisn.swfconvert.core.shape.ShapeObject
import com.maltaisn.swfconvert.core.text.Font
import com.maltaisn.swfconvert.core.text.FontId
import com.maltaisn.swfconvert.core.text.FontScale
import com.maltaisn.swfconvert.core.text.GlyphData
import com.maltaisn.swfconvert.core.text.TextObject
import java.awt.geom.AffineTransform
import javax.inject.Inject

/**
 * Converts SWF text tags to [TextObject] intermediate representation.
 */
internal class TextConverter @Inject constructor(
    private val config: ConvertConfiguration
) {

    private lateinit var context: SwfObjectContext

    private lateinit var textTag: StaticTextTag
    private lateinit var colorTransform: CompositeColorTransform
    private lateinit var fontsMap: Map<FontId, Font>

    // Text span style
    private var font: Font? = null
    private var fontSize: Float? = null
    private var color: Color? = null
    private var offsetX = 0f
    private var offsetY = 0f

    private val yAxisMultiplier = when (config.yAxisDirection) {
        // The Y scale is negated because glyphs in font files are upright,
        // and they must be flipped back because frame is already flipped.
        YAxisDirection.UP -> -1
        YAxisDirection.DOWN -> 1
    }

    fun createTextObjects(
        context: SwfObjectContext,
        textTag: StaticTextTag,
        colorTransform: CompositeColorTransform,
        fontsMap: Map<FontId, Font>
    ): List<FrameObject> {
        this.context = context
        this.textTag = textTag
        this.colorTransform = colorTransform
        this.fontsMap = fontsMap

        resetStyles()

        val fontScale = getTextScale() ?: return emptyList()
        val transform = getTextTransform(fontScale)

        // If transform is not identity, wrap text objects in a transform group.
        var textObjects = mutableListOf<FrameObject>()
        val objects: MutableList<FrameObject> = if (transform.isIdentity) {
            textObjects
        } else {
            // Note: it would be possible to extract translate and
            // scale components to text objects attributes: x, y, fontSize.
            // However, in practice, this has lead to almost no file size difference in SVG and PDF.
            val group = GroupObject.Transform(textTag.identifier, transform)
            textObjects = group.objects
            mutableListOf(group)
        }

        // Parse text spans
        for (span in textTag.spans) {
            textObjects.add(createTextSpanObject(span) ?: continue)
        }

        // Add text bounds rectangle if enabled and at least one text object was created.
        if (config.drawTextBounds && textObjects.isNotEmpty()) {
            objects += createTextBoundsObject(textTag)
        }

        return objects
    }

    /**
     * Get font scale used by the [textTag]. Also make sure font scale parameters are all the same
     * (i.e DefineFont2 and DefineFont3 aren't mixed) because the same transform group is used for all spans.
     * Returns `null` if no text spans in the tag have a font set, or tag has no text spans.
     */
    private fun getTextScale(): FontScale? {
        var lastScale: FontScale? = null
        for (span in textTag.spans) {
            if (span.identifier != null) {
                val fontId = createFontId(span.identifier)
                val font = fontsMap[fontId] ?: error("Unknown font ID")
                val scale = font.metrics.scale
                conversionError(lastScale == null || scale == lastScale, context) {
                    "Font tags with different scale in same text tag"
                }
                lastScale = scale
            }
        }
        return lastScale
    }

    /**
     * Creates the text transform used to draw the text.
     */
    private fun getTextTransform(scale: FontScale): AffineTransform {
        val tagTr = textTag.transform.toAffineTransformOrIdentity()
        val usx = scale.unscaleX.toDouble()
        val usy = scale.unscaleY.toDouble() * yAxisMultiplier
        // Scale the transform except for translation components.
        // (this is a pre-scale transformation but AffineTransform doesn't have it)
        return AffineTransform(
            tagTr.scaleX * usx, tagTr.shearY * usx,
            tagTr.shearX * usy, tagTr.scaleY * usy,
            tagTr.translateX, tagTr.translateY)
    }

    private fun createTextSpanObject(span: TextSpan): TextObject? {
        updateStyleFromTextSpan(span)

        val font = this.font!!
        val fontSize = this.fontSize!!
        val fontScale = font.metrics.scale

        if (fontSize == 0f || span.characters.isEmpty()) {
            // No height or no chars, ignore it.
            return null
        }

        val glyphIndices = span.characters.toMutableList()

        // SWF spec talks about the offset being relative to the glyph "reference point", i.e
        // the starting point of the path. It doesn't seem necessary to take it into account.
        var xPos = offsetX
        val yPos = offsetY

        // Although not very clear from SWF specification, drawing text actually changes the
        // X offset. So change the X offset by the sum of all glyph advances.
        offsetX += glyphIndices.sumBy { it.advance } / fontScale.unscaleX

        // Fold and trim whitespace in text
        foldRepeatedWhitespaceInGlyphIndices(glyphIndices)
        xPos += trimWhitespaceInGlyphIndices(glyphIndices)

        if (glyphIndices.isEmpty()) {
            // Text span was only made of whitespaces, ignore it.
            return null
        }

        val glyphOffsets = getGlyphOffsetsFromGlyphIndices(glyphIndices)

        val text = String(CharArray(glyphIndices.size) { font.getGlyph(glyphIndices[it]).char })

        return TextObject(textTag.identifier, xPos, yPos, fontSize,
            color!!, font, text, glyphOffsets)
    }

    /**
     * Fold repeated whitespace to single spaces in [glyphIndices].
     * This is a problem for SVG output for example, because repeated whitespace are ignored
     * in XML and interpreted as a single space. It also allows a slight size optimization.
     */
    private fun foldRepeatedWhitespaceInGlyphIndices(glyphIndices: MutableList<GlyphIndex>) {
        val font = this.font!!
        for (i in glyphIndices.lastIndex - 1 downTo 0) {
            val currGlyphIndex = glyphIndices[i]
            val nextGlyphIndex = glyphIndices[i + 1]
            val currGlyph = font.getGlyph(currGlyphIndex)
            val nextGlyph = font.getGlyph(nextGlyphIndex)
            if (currGlyph.isWhitespace && nextGlyph.isWhitespace) {
                // Fold the two whitespace into a single one, using char of the first.
                glyphIndices[i] = GlyphIndex(currGlyphIndex.glyphIndex,
                    currGlyphIndex.advance + nextGlyphIndex.advance)
                glyphIndices.removeAt(i + 1)
            }
        }
    }

    /**
     * Trim leading and trailling whitespace in [glyphIndices].
     * A single space can be removed to either end. The offset by which the text span should
     * be shifted to the right is returned.
     */
    private fun trimWhitespaceInGlyphIndices(glyphIndices: MutableList<GlyphIndex>): Float {
        val font = this.font!!
        val offset = if (font.getGlyph(glyphIndices.first()).isWhitespace) {
            glyphIndices.removeAt(0).advance / font.metrics.scale.unscaleX
        } else {
            0f
        }
        if (glyphIndices.isNotEmpty() && font.getGlyph(glyphIndices.last()).isWhitespace) {
            glyphIndices.removeAt(glyphIndices.lastIndex)
        }
        return offset
    }

    /**
     * Create a list of glyph offsets from [glyphIndices]. See [TextObject.glyphOffsets] for
     * more information.
     */
    private fun getGlyphOffsetsFromGlyphIndices(glyphIndices: List<GlyphIndex>): List<Float> {
        val font = this.font!!
        val fontSize = this.fontSize!!
        val fontScale = font.metrics.scale

        // Add text span glyphs to list
        val glyphOffsets = mutableListOf<Float>()
        var advanceDeviation = 0f
        for (i in 0 until glyphIndices.lastIndex) {
            val glyphIndex = glyphIndices[i]
            val glyph = font.getGlyph(glyphIndex)

            // Find difference with default advance width, in glyph space units.
            val defaultAdvance = glyph.data.advanceWidth
            val actualAdvance = glyphIndex.advance / (fontScale.unscaleX * fontSize) * GlyphData.EM_SQUARE_SIZE
            val diff = actualAdvance - defaultAdvance

            if (diff <= config.ignoreGlyphOffsetsThreshold) {
                // Ignore below threshold difference, use zero offset. But accumulate deviation from
                // expected total span advance to avoid adding up error due to ignored offsets over time.
                glyphOffsets += 0f
                advanceDeviation += diff
                if (advanceDeviation > config.ignoreGlyphOffsetsThreshold) {
                    // Accumulated advance deviation is above threshold, reset it and add offset.
                    advanceDeviation = 0f
                    glyphOffsets += advanceDeviation
                }
            } else {
                glyphOffsets += diff
            }
        }

        // Remove trailing zero offsets.
        while (glyphOffsets.isNotEmpty() && glyphOffsets.last() == 0f) {
            glyphOffsets.removeAt(glyphOffsets.lastIndex)
        }

        return glyphOffsets
    }

    private fun Font.getGlyph(index: GlyphIndex) = this.glyphs[index.glyphIndex]

    private fun updateStyleFromTextSpan(span: TextSpan) {
        updateFontFromTextSpan(span)
        updateFontSizeFromTextSpan(span)
        updateTextColorFromTextSpan(span)
        updateOffsetsFromTextSpan(span)
    }

    private fun resetStyles() {
        font = null
        fontSize = null
        color = null
        offsetX = 0f
        offsetY = 0f
    }

    private fun updateFontFromTextSpan(span: TextSpan) {
        if (span.identifier != null) {
            val fontId = createFontId(span.identifier)
            this.font = fontsMap[fontId]
                ?: conversionError(context, "Unknown font ID ${span.identifier}")
        }
        val font = this.font
        conversionError(font != null, context) { "No font specified" }
    }

    private fun updateFontSizeFromTextSpan(span: TextSpan) {
        if (span.height != null) {
            fontSize = span.height.toFloat()
        }
        conversionError(fontSize != null, context) { "No font size specified" }
    }

    private fun updateTextColorFromTextSpan(span: TextSpan) {
        if (span.color != null) {
            color = colorTransform.transform(span.color.toColor())
        }
        conversionError(color != null, context) { "No text color specified" }
    }

    private fun updateOffsetsFromTextSpan(span: TextSpan) {
        // Although SWF specification says that unspecified offset is like setting it to 0,
        // this is not true, offset is only set when specified. Also, offsets are not additive.
        val fontScale = font!!.metrics.scale
        if (span.offsetX != null) {
            offsetX = span.offsetX.toFloat() / fontScale.unscaleX
        }
        if (span.offsetY != null) {
            offsetY = span.offsetY.toFloat() / fontScale.unscaleY * yAxisMultiplier
        }
    }

    /**
     * Create a debug shape matching the defined SWF bounds of a [textTag].
     */
    private fun createTextBoundsObject(textTag: StaticTextTag): FrameObject {
        val bounds = textTag.bounds
        val boundsRect = Path(listOf(PathElement.Rectangle(
            bounds.minX.toFloat(), bounds.minY.toFloat(),
            bounds.width.toFloat(), bounds.height.toFloat())),
            lineStyle = config.debugLineStyle)
        return ShapeObject(textTag.identifier, listOf(boundsRect))
    }

    /**
     * Create a font ID to uniquely identify a font across a SWF file collection.
     * [context] could've been used directly but it's too tied to SWF to be part of IR.
     */
    private fun createFontId(id: Int) = FontId((context.parent as SwfFileContext).fileIndex, id)

}
