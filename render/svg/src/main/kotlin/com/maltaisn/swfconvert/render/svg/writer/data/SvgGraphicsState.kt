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

package com.maltaisn.swfconvert.render.svg.writer.data

import com.maltaisn.swfconvert.core.image.Color

/**
 * Represents the "graphics state" at a position in a SVG document.
 * Undefined attributes inherit the value from the ancestor graphics state, except a for few non-inheritable attributes.
 */
@Suppress("UNCHECKED_CAST")
internal data class SvgGraphicsState(val attrs: Map<Attribute, Any>) {

    val clipPathId get() = attrs[Attribute.CLIP_PATH] as String?
    val clipRule get() = attrs[Attribute.CLIP_RULE] as SvgFillRule?
    val fill get() = attrs[Attribute.FILL] as SvgFill?
    val fillOpacity get() = attrs[Attribute.FILL_OPACITY] as Float?
    val fillRule get() = attrs[Attribute.FILL_RULE] as SvgFillRule?
    val fontFamily get() = attrs[Attribute.FONT_FAMILY] as String?
    val fontSize get() = attrs[Attribute.FONT_SIZE] as Float?
    val maskId get() = attrs[Attribute.MASK] as String?
    val mixBlendMode get() = attrs[Attribute.MIX_BLEND_MODE] as SvgMixBlendMode?
    val preserveAspectRatio get() = attrs[Attribute.PRESERVE_ASPECT_RATIO] as SvgPreserveAspectRatio?
    val stroke get() = attrs[Attribute.STROKE] as SvgFill?
    val strokeOpacity get() = attrs[Attribute.STROKE_OPACITY] as Float?
    val strokeWidth get() = attrs[Attribute.STROKE_WIDTH] as Float?
    val strokeLineJoin get() = attrs[Attribute.STROKE_LINE_JOIN] as SvgStrokeLineJoin?
    val strokeLineCap get() = attrs[Attribute.STROKE_LINE_CAP] as SvgStrokeLineCap?
    val strokeMiterLimit get() = attrs[Attribute.STROKE_MITER_LIMIT] as Float?
    val transform get() = attrs[Attribute.TRANSFORM] as List<SvgTransform>?
    val x get() = attrs[Attribute.X] as SvgNumber?
    val y get() = attrs[Attribute.Y] as SvgNumber?

    /**
     * Return a "cleaned" copy of this graphics state, making attributes that use the same value
     * as the one previously defined in [ancestors] inherit the value instead of duplicating it.
     * Ancestors must be ordered from the newest to the oldest.
     * Non-inheritable attribute values are always kept.
     */
    fun cleanedForAncestors(ancestors: List<SvgGraphicsState>): SvgGraphicsState {
        val newAttrs = attrs.toMutableMap()
        for ((attr, value) in attrs) {
            if (!attr.inheritable) continue
            for (ancestor in ancestors) {
                val oldValue = ancestor.attrs[attr] ?: continue
                // Found previously defined value for that attribute, check if different.
                if (value == oldValue) {
                    // Same as previous value, remove it.
                    newAttrs -= attr
                }
                break
            }
            // No previously defined value for that property, keep new value.
        }
        return SvgGraphicsState(newAttrs)
    }

    enum class Attribute(val inheritable: Boolean) {
        CLIP_PATH(true),
        CLIP_RULE(true),
        FILL(true),
        FILL_OPACITY(true),
        FILL_RULE(true),
        FONT_FAMILY(true),
        FONT_SIZE(true),
        MASK(true),
        MIX_BLEND_MODE(true),
        PRESERVE_ASPECT_RATIO(true),
        STROKE(true),
        STROKE_LINE_CAP(true),
        STROKE_LINE_JOIN(true),
        STROKE_MITER_LIMIT(true),
        STROKE_OPACITY(true),
        STROKE_WIDTH(true),
        TRANSFORM(false),
        X(false),
        Y(false),
    }

    companion object {
        /**
         * The "null" graphics state, inherit all attributes from ancestor.
         */
        val NULL = SvgGraphicsState(emptyMap())

        /**
         * The default graphics state at the start of any SVG document. A few differences:
         * Note that default [fontSize] is `medium` which is relative to user so it's not used here.
         */
        val DEFAULT = SvgGraphicsState(
            clipRule = SvgFillRule.NON_ZERO,
            fill = SvgFillColor(Color.BLACK),
            fillOpacity = 1f,
            fillRule = SvgFillRule.NON_ZERO,
            mixBlendMode = SvgMixBlendMode.NORMAL,
            preserveAspectRatio = SvgPreserveAspectRatio(SvgPreserveAspectRatio.Align.X_MID_Y_MID),
            stroke = SvgFillNone,
            strokeOpacity = 1f,
            strokeWidth = 1f,
            strokeLineJoin = SvgStrokeLineJoin.MITER,
            strokeLineCap = SvgStrokeLineCap.BUTT,
            strokeMiterLimit = 4f,
            x = SvgNumber.ZERO,
            y = SvgNumber.ZERO
        )

        @Suppress("LongParameterList", "ComplexMethod")
        operator fun invoke(
            base: SvgGraphicsState? = null,
            clipPathId: String? = null,
            clipRule: SvgFillRule? = null,
            fill: SvgFill? = null,
            fillOpacity: Float? = null,
            fillRule: SvgFillRule? = null,
            fontFamily: String? = null,
            fontSize: Float? = null,
            maskId: String? = null,
            mixBlendMode: SvgMixBlendMode? = null,
            preserveAspectRatio: SvgPreserveAspectRatio? = null,
            stroke: SvgFill? = null,
            strokeOpacity: Float? = null,
            strokeWidth: Float? = null,
            strokeLineJoin: SvgStrokeLineJoin? = null,
            strokeLineCap: SvgStrokeLineCap? = null,
            strokeMiterLimit: Float? = null,
            transform: List<SvgTransform>? = null,
            x: SvgNumber? = null,
            y: SvgNumber? = null
        ): SvgGraphicsState {
            val attrs = base?.attrs?.toMutableMap() ?: mutableMapOf()
            if (clipPathId != null) attrs[Attribute.CLIP_PATH] = clipPathId
            if (clipRule != null) attrs[Attribute.CLIP_RULE] = clipRule
            if (fill != null) attrs[Attribute.FILL] = fill
            if (fillOpacity != null) attrs[Attribute.FILL_OPACITY] = fillOpacity
            if (fillRule != null) attrs[Attribute.FILL_RULE] = fillRule
            if (fontFamily != null) attrs[Attribute.FONT_FAMILY] = fontFamily
            if (fontSize != null) attrs[Attribute.FONT_SIZE] = fontSize
            if (maskId != null) attrs[Attribute.MASK] = maskId
            if (mixBlendMode != null) attrs[Attribute.MIX_BLEND_MODE] = mixBlendMode
            if (preserveAspectRatio != null) attrs[Attribute.PRESERVE_ASPECT_RATIO] = preserveAspectRatio
            if (stroke != null) attrs[Attribute.STROKE] = stroke
            if (strokeOpacity != null) attrs[Attribute.STROKE_OPACITY] = strokeOpacity
            if (strokeWidth != null) attrs[Attribute.STROKE_WIDTH] = strokeWidth
            if (strokeLineJoin != null) attrs[Attribute.STROKE_LINE_JOIN] = strokeLineJoin
            if (strokeLineCap != null) attrs[Attribute.STROKE_LINE_CAP] = strokeLineCap
            if (strokeMiterLimit != null) attrs[Attribute.STROKE_MITER_LIMIT] = strokeMiterLimit
            if (transform != null) attrs[Attribute.TRANSFORM] = transform
            if (x != null) attrs[Attribute.X] = x
            if (y != null) attrs[Attribute.Y] = y
            return SvgGraphicsState(attrs)
        }
    }
}
