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

/**
 * Represents the "graphics state" at a position in a SVG document.
 * These attributes inherit their values from the parent tags. Here `null` means inherit.
 * These are called the "presentation attributes" in the reference:
 * [https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/Presentation]
 */
internal data class SvgGraphicsState(
    val fill: SvgFill? = null,
    val fillOpacity: Float? = null,
    val fillRule: SvgFillRule? = null,
    val stroke: SvgFill? = null,
    val strokeOpacity: Float? = null,
    val strokeWidth: Float? = null,
    val strokeLineJoin: SvgStrokeLineJoin? = null,
    val strokeLineCap: SvgStrokeLineCap? = null,
    val strokeMiterLimit: Float? = null,
    val clipPathId: String? = null,
    val clipPathRule: SvgFillRule? = null,
    val maskId: String? = null,
    val transforms: List<SvgTransform>? = null,
    val preserveAspectRatio: SvgPreserveAspectRatio? = null,
    val mixBlendMode: SvgMixBlendMode? = null
)
