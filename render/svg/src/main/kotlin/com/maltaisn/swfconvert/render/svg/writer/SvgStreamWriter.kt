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

@file:Suppress("TooManyFunctions")

package com.maltaisn.swfconvert.render.svg.writer

import com.maltaisn.swfconvert.render.svg.writer.data.SvgFillColor
import com.maltaisn.swfconvert.render.svg.writer.data.SvgGradientStop
import com.maltaisn.swfconvert.render.svg.writer.data.SvgGradientUnits
import com.maltaisn.swfconvert.render.svg.writer.data.SvgGraphicsState
import com.maltaisn.swfconvert.render.svg.writer.data.SvgNumber
import com.maltaisn.swfconvert.render.svg.writer.data.SvgTransform
import com.maltaisn.swfconvert.render.svg.writer.data.toSvgTransformList
import com.maltaisn.swfconvert.render.svg.writer.data.validateGradientStops
import com.maltaisn.swfconvert.render.svg.writer.format.appendValuesList
import com.maltaisn.swfconvert.render.svg.writer.format.format
import com.maltaisn.swfconvert.render.svg.writer.format.formatOptimized
import com.maltaisn.swfconvert.render.svg.writer.format.requireSvgPrecision
import java.awt.geom.Rectangle2D
import java.io.Closeable
import java.io.Flushable
import java.io.OutputStream

/**
 * A class to write SVG to an [outputStream]. SVG 2.0 is generated.
 * Only the small subset of SVG needed to render the intermediate representation is supported.
 *
 * @param prettyPrint Whether to pretty print SVG or not.
 */
internal class SvgStreamWriter(
    outputStream: OutputStream,
    private val precision: Int,
    private val transformPrecision: Int,
    private val percentPrecision: Int,
    private val prettyPrint: Boolean
) : Closeable, Flushable {

    private val xml = XmlStreamWriter(outputStream, mapOf(
        null to "http://www.w3.org/2000/svg",
        "xlink" to "http://www.w3.org/1999/xlink"
    ), prettyPrint)

    private var state = State.INITIAL

    private val grStateStack = ArrayDeque<SvgGraphicsState>()

    private var defStartLevel = NO_DEF_LEVEL
    private var defId: String? = null
    private var defWasWritten = false

    init {
        requireSvgPrecision(precision)
        requireSvgPrecision(transformPrecision)
        requireSvgPrecision(percentPrecision)

        // Push default graphics state correspond to the default values for attributes.
        grStateStack += SvgGraphicsState.DEFAULT
    }

    override fun flush() {
        xml.flush()
    }

    override fun close() {
        flush()
        xml.close()
    }

    fun start(
        width: SvgNumber,
        height: SvgNumber,
        viewBox: Rectangle2D? = null,
        writeProlog: Boolean = true,
        grState: SvgGraphicsState = SvgGraphicsState.NULL
    ) {
        check(state == State.INITIAL) { "SVG has already started or has ended" }
        state = State.STARTED

        grStateStack += grState

        if (writeProlog) {
            xml.prolog(arrayOf(ATTR_VERSION to "1.1", ATTR_ENCODING to "UTF-8"))
        }

        xml.start(TAG_SVG, arrayOf(
            ATTR_VERSION to "2.0",
            ATTR_WIDTH to width.toSvg(precision, !prettyPrint),
            ATTR_HEIGHT to height.toSvg(precision, !prettyPrint),
            ATTR_VIEWBOX to viewBox?.toSvgValuesList(),
            *getNewGraphicsStateAttrs()))
    }

    /**
     * End SVG document and closes this writer. Nothing should be done after that.
     */
    fun end() {
        checkIfStarted()
        check(xml.currentLevel == 1) { "Cannot end SVG with unclosed tags" }
        state = State.ENDED

        xml.end()
        xml.close()
    }

    private fun checkIfStarted() {
        check(state == State.STARTED) { "SVG has not started or has ended." }
    }

    fun startDefs() {
        xml.start(TAG_DEFS)
    }

    fun endDefs() {
        expectEndTag(TAG_DEFS)
    }

    inline fun writeDefs(build: () -> Unit = {}) {
        startDefs()
        build()
        endDefs()
    }

    fun <T : String?> writeDef(id: T, block: SvgStreamWriter.() -> Unit): T {
        check(defStartLevel == NO_DEF_LEVEL) { "Cannot write nested defs" }

        defStartLevel = xml.currentLevel
        defId = id
        defWasWritten = false

        this.block()

        check(defWasWritten) { "No def was written" }
        defStartLevel = NO_DEF_LEVEL
        return id
    }

    private fun consumeDefId(): String? {
        check(xml.currentLevel != defStartLevel || !defWasWritten) { "Def was already written" }
        val id = defId
        defId = null
        defWasWritten = true
        return id
    }

    /**
     * Start a new group element with a [grState]. Returns `true` if the group was started.
     * @param discardIfEmpty Whether to discard the group if it has no attributes (therefore being useless).
     */
    fun startGroup(grState: SvgGraphicsState = SvgGraphicsState.NULL, discardIfEmpty: Boolean = false): Boolean {
        checkIfStarted()
        grStateStack += grState
        val attrs = arrayOf(ATTR_ID to consumeDefId(), *getNewGraphicsStateAttrs())
        return if (attrs.any { it.second != null } || !discardIfEmpty) {
            xml.start(TAG_GROUP, attrs)
            true
        } else {
            false
        }
    }

    fun endGroup() {
        expectEndTag(TAG_GROUP)
        grStateStack.removeLast()
    }

    inline fun group(
        grState: SvgGraphicsState = SvgGraphicsState.NULL,
        discardIfEmpty: Boolean = false,
        build: () -> Unit = {}
    ) {
        val started = startGroup(grState, discardIfEmpty)
        build()
        if (started) {
            endGroup()
        }
    }

    fun startClipPath() {
        checkIfStarted()
        xml.start(TAG_CLIP_PATH, arrayOf(ATTR_ID to consumeDefId()))
    }

    fun endClipPath() {
        expectEndTag(TAG_CLIP_PATH)
    }

    inline fun clipPath(build: () -> Unit) {
        startClipPath()
        build()
        endClipPath()
    }

    fun clipPathData(data: String, grState: SvgGraphicsState = SvgGraphicsState.NULL) = clipPath {
        path(data, grState)
    }

    inline fun clipPathData(
        grState: SvgGraphicsState = SvgGraphicsState.NULL,
        write: SvgPathWriter.() -> Unit
    ) = clipPath {
        path(grState, write)
    }

    fun startMask() {
        checkIfStarted()
        xml.start(TAG_MASK, arrayOf(ATTR_ID to consumeDefId()))
    }

    fun endMask() {
        expectEndTag(TAG_MASK)
    }

    inline fun mask(build: () -> Unit) {
        startMask()
        build()
        endMask()
    }

    fun path(data: String, grState: SvgGraphicsState = SvgGraphicsState.NULL) {
        checkIfStarted()
        withGraphicsState(grState) {
            xml {
                TAG_PATH(arrayOf(
                    ATTR_ID to consumeDefId(),
                    ATTR_DATA to data,
                    *getNewGraphicsStateAttrs()
                ))
            }
        }
    }

    inline fun path(
        grState: SvgGraphicsState = SvgGraphicsState.NULL,
        write: SvgPathWriter.() -> Unit
    ) {
        val data = SvgPathWriter(precision, !prettyPrint).apply(write).toString()
        path(data, grState)
    }

    fun image(
        href: String,
        width: SvgNumber? = null,
        height: SvgNumber? = null,
        grState: SvgGraphicsState = SvgGraphicsState.NULL
    ) {
        require((width == null || width > SvgNumber.ZERO) &&
                (height == null || height > SvgNumber.ZERO)) {
            "Image dimensions must be greater than zero"
        }
        checkIfStarted()
        withGraphicsState(grState) {
            xml {
                TAG_IMAGE(arrayOf(
                    ATTR_ID to consumeDefId(),
                    ATTR_WIDTH to width,
                    ATTR_HEIGHT to height,
                    ATTR_XLINK_HREF to href,
                    *getNewGraphicsStateAttrs()
                ))
            }
        }
    }

    fun linearGradient(
        stops: List<SvgGradientStop>,
        units: SvgGradientUnits = SvgGradientUnits.OBJECT_BOUNDING_BOX,
        transforms: List<SvgTransform>? = null,
        x1: Float = 0f,
        y1: Float = 0f,
        x2: Float = 1f,
        y2: Float = 0f,
        grState: SvgGraphicsState = SvgGraphicsState.NULL
    ) {
        stops.validateGradientStops()
        checkIfStarted()
        withGraphicsState(grState) {
            xml {
                TAG_LINEAR_GRADIENT(arrayOf(
                    ATTR_ID to consumeDefId(),
                    ATTR_X1 to x1.takeIf { x1 != 0f }?.formatOptimizedOrNot(precision),
                    ATTR_Y1 to y1.takeIf { y1 != 0f }?.formatOptimizedOrNot(precision),
                    ATTR_X2 to x2.takeIf { x2 != 1f }?.formatOptimizedOrNot(precision),
                    ATTR_Y2 to y2.takeIf { y2 != 0f }?.formatOptimizedOrNot(precision),
                    ATTR_GRADIENT_UNITS to units.takeIf { it != SvgGradientUnits.OBJECT_BOUNDING_BOX }?.svgName,
                    ATTR_GRADIENT_TRANSFORM to transforms?.toSvgTransformList(transformPrecision, !prettyPrint),
                    *getNewGraphicsStateAttrs()
                )) {
                    for (stop in stops) {
                        TAG_STOP(arrayOf(
                            ATTR_OFFSET to stop.offset.format(percentPrecision),
                            ATTR_STOP_COLOR to SvgFillColor(stop.color).toSvg(),
                            ATTR_STOP_OPACITY to stop.opacity.takeIf { it != 1f }
                                ?.formatOptimizedOrNot(percentPrecision)
                        ))
                    }
                }
            }
        }
    }

    fun font(name: String, src: String) {
        checkIfStarted()
        consumeDefId() // Consume def ID so def is marked as written. Font doesn't actually use the ID attribute though.
        xml {
            TAG_STYLE(arrayOf(ATTR_TYPE to "text/css")) {
                text("@$CSS_FONT_FACE{$CSS_FONT_FAMILY:$name;" +
                        "$CSS_SRC:url($src);}")
            }
        }
    }

    fun text(
        text: String,
        dx: FloatArray = floatArrayOf(),
        grState: SvgGraphicsState = SvgGraphicsState.NULL
    ) {
        checkIfStarted()
        val dxValue = if (dx.all { it == 0f }) null else createSvgValuesList(precision, dx)
        withGraphicsState(grState) {
            xml {
                TAG_TEXT(arrayOf(
                    ATTR_ID to consumeDefId(),
                    *getNewGraphicsStateAttrs(),
                    ATTR_DX to dxValue
                )) {
                    text(text)
                }
            }
        }
    }

    fun use(id: String, grState: SvgGraphicsState = SvgGraphicsState.NULL) {
        checkIfStarted()
        withGraphicsState(grState) {
            xml {
                TAG_USE(arrayOf(ATTR_XLINK_HREF to "#$id", *getNewGraphicsStateAttrs()))
            }
        }
    }

    private inline fun withGraphicsState(grState: SvgGraphicsState, block: () -> Unit) {
        grStateStack += grState
        block()
        grStateStack.removeLast()
    }

    /**
     * Get a list of SVG attributes corresponding to the properties changed in the last
     * pushed [SvgGraphicsState] compared to the previous graphics state.
     */
    private fun getNewGraphicsStateAttrs(): AttributesArray {
        val newGrState = grStateStack.last().clean(
            grStateStack.subList(0, grStateStack.size - 1).asReversed())
        return listOfNotNull(
            ATTR_CLIP_PATH to newGrState.clipPathId?.toSvgUrlReference(),
            ATTR_CLIP_RULE to newGrState.clipRule?.svgName,
            ATTR_FILL to newGrState.fill?.toSvg(),
            ATTR_FILL_OPACITY to newGrState.fillOpacity?.formatOptimizedOrNot(percentPrecision),
            ATTR_FILL_RULE to newGrState.fillRule?.svgName,
            ATTR_FONT_FAMILY to newGrState.fontFamily,
            ATTR_FONT_SIZE to newGrState.fontSize?.formatOptimizedOrNot(precision),
            ATTR_MASK to newGrState.maskId?.toSvgUrlReference(),
            ATTR_PRESERVE_ASPECT_RATIO to newGrState.preserveAspectRatio?.toSvg(),
            ATTR_STROKE to newGrState.stroke?.toSvg(),
            ATTR_STROKE_OPACITY to newGrState.strokeOpacity?.formatOptimizedOrNot(percentPrecision),
            ATTR_STROKE_WIDTH to newGrState.strokeWidth?.formatOptimizedOrNot(precision),
            ATTR_STROKE_LINE_JOIN to newGrState.strokeLineJoin?.svgName,
            ATTR_STROKE_LINE_CAP to newGrState.strokeLineCap?.svgName,
            ATTR_STROKE_MITER_LIMIT to newGrState.strokeMiterLimit?.formatOptimizedOrNot(precision),
            ATTR_STYLE to newGrState.getCssStyleValueOrNull(),
            ATTR_TRANSFORM to newGrState.transform?.toSvgTransformList(transformPrecision, !prettyPrint),
            ATTR_X to newGrState.x?.toSvg(precision, !prettyPrint),
            ATTR_Y to newGrState.y?.toSvg(precision, !prettyPrint)
        ).toTypedArray()
    }

    /** Get the value of the CSS `style` attribute for a [SvgGraphicsState], or `null` if there's no value. */
    private fun SvgGraphicsState.getCssStyleValueOrNull() = buildString {
        val grState = this@getCssStyleValueOrNull
        val mixBlendMode = grState.mixBlendMode
        if (mixBlendMode != null) {
            append("$CSS_MIX_BLEND_MODE:${mixBlendMode.svgName};")
        }
        if (isNotEmpty()) {
            deleteCharAt(length - 1)
        }
    }.ifEmpty { null }

    private fun expectEndTag(tag: String) {
        checkIfStarted()
        check(xml.currentTag == tag) {
            "Cannot end tag <$tag>, current tag is <${xml.currentTag}>"
        }
        xml.end()
    }

    private fun Rectangle2D.toSvgValuesList() =
        createSvgValuesList(precision, floatArrayOf(this.x.toFloat(), this.y.toFloat(),
            this.width.toFloat(), this.height.toFloat()))

    private fun createSvgValuesList(precision: Int, values: FloatArray) = buildString {
        if (prettyPrint) {
            appendValuesList(precision, values)
        } else {
            // Some browsers don't support optimized values lists so it might be better not to use them.
            // It's also not part of the official syntax reference.
            // appendValuesListOptimized(precision, null, values)
            for (value in values) {
                append(value.formatOptimized(precision))
                append(' ')
            }
            if (values.isNotEmpty()) {
                deleteCharAt(length - 1)
            }
        }
    }

    private fun Float.formatOptimizedOrNot(precision: Int) = if (prettyPrint) {
        this.format(precision)
    } else {
        this.formatOptimized(precision)
    }

    enum class State {
        INITIAL,
        STARTED,
        ENDED
    }

    companion object {
        private const val TAG_CLIP_PATH = "clipPath"
        private const val TAG_DEFS = "defs"
        private const val TAG_GROUP = "g"
        private const val TAG_IMAGE = "image"
        private const val TAG_LINEAR_GRADIENT = "linearGradient"
        private const val TAG_MASK = "mask"
        private const val TAG_PATH = "path"
        private const val TAG_STOP = "stop"
        private const val TAG_STYLE = "style"
        private const val TAG_SVG = "svg"
        private const val TAG_TEXT = "text"
        private const val TAG_USE = "use"

        private const val ATTR_CLIP_PATH = "clip-path"
        private const val ATTR_CLIP_RULE = "clip-rule"
        private const val ATTR_DATA = "d"
        private const val ATTR_DX = "dx"
        private const val ATTR_ENCODING = "encoding"
        private const val ATTR_FILL = "fill"
        private const val ATTR_FILL_OPACITY = "fill-opacity"
        private const val ATTR_FILL_RULE = "fill-rule"
        private const val ATTR_FONT_FAMILY = "font-family"
        private const val ATTR_FONT_SIZE = "font-size"
        private const val ATTR_GRADIENT_TRANSFORM = "gradientTransform"
        private const val ATTR_GRADIENT_UNITS = "gradientUnits"
        private const val ATTR_HEIGHT = "height"
        private const val ATTR_ID = "id"
        private const val ATTR_MASK = "mask"
        private const val ATTR_OFFSET = "offset"
        private const val ATTR_PRESERVE_ASPECT_RATIO = "preserveAspectRatio"
        private const val ATTR_STOP_COLOR = "stop-color"
        private const val ATTR_STOP_OPACITY = "stop-opacity"
        private const val ATTR_STROKE = "stroke"
        private const val ATTR_STROKE_LINE_CAP = "stroke-linecap"
        private const val ATTR_STROKE_LINE_JOIN = "stroke-linejoin"
        private const val ATTR_STROKE_MITER_LIMIT = "stroke-miterlimit"
        private const val ATTR_STROKE_OPACITY = "stroke-opacity"
        private const val ATTR_STROKE_WIDTH = "stroke-width"
        private const val ATTR_STYLE = "style"
        private const val ATTR_TRANSFORM = "transform"
        private const val ATTR_TYPE = "type"
        private const val ATTR_VERSION = "version"
        private const val ATTR_VIEWBOX = "viewBox"
        private const val ATTR_WIDTH = "width"
        private const val ATTR_X = "x"
        private const val ATTR_X1 = "x1"
        private const val ATTR_X2 = "x2"
        private const val ATTR_XLINK_HREF = "xlink:href"
        private const val ATTR_Y = "y"
        private const val ATTR_Y1 = "y2"
        private const val ATTR_Y2 = "y2"

        private const val CSS_FONT_FACE = "font-face"
        private const val CSS_FONT_FAMILY = "font-family"
        private const val CSS_MIX_BLEND_MODE = "mix-blend-mode"
        private const val CSS_SRC = "src"

        private const val NO_DEF_LEVEL = 0
    }

}
