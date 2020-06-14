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

package com.maltaisn.swfconvert.render.svg.writer

import com.maltaisn.swfconvert.core.image.Color
import com.maltaisn.swfconvert.render.svg.writer.data.*
import java.awt.geom.Rectangle2D
import java.io.*
import java.util.*
import kotlin.math.roundToInt

/**
 * A class to write SVG to an [outputStream]. SVG 2.0 is generated.
 * Only the small subset of SVG needed to render the intermediate representation is supported.
 *
 * @param prettyPrint Whether to pretty print SVG or not.
 */
internal class SvgStreamWriter(outputStream: OutputStream,
                               private val prettyPrint: Boolean = false) : Closeable, Flushable {

    private val xml = XmlStreamWriter(outputStream,
            mapOf(null to "http://www.w3.org/2000/svg"), prettyPrint)

    private var state = State.UNINITIALIZED

    private val defStream = ByteArrayOutputStream()
    private val defXml = XmlStreamWriter(defStream)

    private val currentXml: XmlStreamWriter
        get() = if (writingDefs) defXml else xml

    private val grStateStack = LinkedList<SvgGraphicsState>()
    private val defGrStateStack = LinkedList<SvgGraphicsState>()

    private val currentGrStateStack: LinkedList<SvgGraphicsState>
        get() = if (writingDefs) defGrStateStack else grStateStack

    private var writingDefs = false
    private var defId: String? = null


    init {
        // Push default graphics state correspond to the default values for attributes.
        grStateStack.push(DEFAULT_GRAPHICS_STATE)
    }

    override fun flush() {
        xml.flush()
    }

    override fun close() {
        flush()
        defXml.close()
        defStream.close()
        xml.close()
    }

    fun start(width: Float, height: Float,
              viewBox: Rectangle2D? = null,
              grState: SvgGraphicsState = NULL_GRAPHICS_STATE) {
        check(state == State.UNINITIALIZED) { "SVG has already started" }
        state = State.STARTED

        val viewBoxStr = viewBox?.let { rect ->
            buildString {
                appendValuesList(rect.x.toFloat(), rect.y.toFloat(),
                        rect.width.toFloat(), rect.height.toFloat())
            }
        }

        grStateStack.push(grState)

        xml.prolog(ATTR_VERSION to "1.1", ATTR_ENCODING to "UTF-8")
        xml.start(TAG_SVG,
                ATTR_VERSION to "2.0",
                ATTR_WIDTH to width,
                ATTR_HEIGHT to height,
                ATTR_VIEWBOX to viewBoxStr,
                *getNewGraphicsStateAttrs())
    }

    /**
     * End SVG document and closes this writer. Nothing should be done after that.
     */
    fun end() {
        check(state == State.STARTED) { "SVG has already ended" }
        check(!writingDefs) { "Cannot end SVG while writing defs" }
        check(xml.currentLevel == 1) { "Cannot end SVG with unclosed tags" }

        state == State.ENDED

        // Write defs to output stream if there are any.
        if (!defXml.isRoot) {
            endDefs()
        }

        xml.end()
        xml.close()
    }

    private fun startDefs() {
        defXml.start(TAG_DEFS)
        defGrStateStack.push(DEFAULT_GRAPHICS_STATE)
    }

    private fun endDefs() {
        check(defXml.currentLevel == 1) { "Cannot end defs with unclosed tags" }
        defXml.end()
        defXml.close()
        xml.raw(defStream.toString())
        defStream.close()
    }

    fun writeDef(id: String, block: SvgStreamWriter.() -> Unit) {
        if (defXml.isRoot) {
            startDefs()
        }

        writingDefs = true
        defId = id
        this.block()
        check(defId == null) { "No def was written" }
        writingDefs = false
    }

    fun startGroup(grState: SvgGraphicsState = NULL_GRAPHICS_STATE) {
        currentGrStateStack.push(grState)
        currentXml.start(TAG_GROUP,
                ATTR_ID to defId,
                *getNewGraphicsStateAttrs())
        onElementStarted()
    }

    fun endGroup() {
        check(currentXml.end() == TAG_GROUP)
        currentGrStateStack.pop()
    }

    inline fun group(grState: SvgGraphicsState = NULL_GRAPHICS_STATE, build: () -> Unit = {}) {
        startGroup(grState)
        build()
        endGroup()
    }

    fun startClipPath() {
        currentXml.start(TAG_CLIP_PATH, ATTR_ID to defId)
        onElementStarted()
    }

    fun endClipPath() {
        check(currentXml.end() == TAG_CLIP_PATH)
    }

    inline fun clipPath(build: () -> Unit) {
        startClipPath()
        build()
        endClipPath()
    }

    fun startMask() {
        currentXml.start(TAG_MASK, ATTR_ID to defId)
        onElementStarted()
    }

    fun endMask() {
        check(currentXml.end() == TAG_MASK)
    }

    inline fun mask(build: () -> Unit) {
        startMask()
        build()
        endMask()
    }

    fun path(data: String, grState: SvgGraphicsState = NULL_GRAPHICS_STATE) {
        currentGrStateStack.push(grState)
        currentXml {
            TAG_PATH(
                    ATTR_ID to defId,
                    ATTR_DATA to data,
                    *getNewGraphicsStateAttrs())
        }
        currentGrStateStack.pop()
        onElementStarted()
    }

    inline fun path(grState: SvgGraphicsState, precision: Int, write: SvgPathWriter.() -> Unit) {
        val data = SvgPathWriter(precision).apply(write).toString()
        path(data, grState)
    }

    fun image(href: String,
              x: Float, y: Float,
              width: Float? = null, height: Float? = null,
              grState: SvgGraphicsState = NULL_GRAPHICS_STATE) {
        require((width == null || width > 0f) && (height == null || height > 0f)) {
            "Image dimensions must be greater than zero"
        }
        currentGrStateStack.push(grState)
        currentXml {
            TAG_IMAGE(
                    ATTR_ID to defId,
                    ATTR_X to x,
                    ATTR_Y to y,
                    ATTR_WIDTH to width,
                    ATTR_HEIGHT to height,
                    ATTR_HREF to href,
                    *getNewGraphicsStateAttrs())
        }
        currentGrStateStack.pop()
        onElementStarted()
    }

    fun linearGradient(stops: List<SvgGradientStop>,
                       transforms: List<SvgTransform>? = null,
                       x1: Float = 0f, y1: Float = 0f, x2: Float = 1f, y2: Float = 0f) {
        stops.validateGradientStops()
        require(x1 in 0f..1f && y1 in 0f..1f && x2 in 0f..1f && y2 in 0f..1f) {
            "Gradient vector value out of bounds, must be between 0 and 1"
        }
        currentXml {
            TAG_LINEAR_GRADIENT(
                    ATTR_ID to defId,
                    ATTR_X1 to x1.takeIf { it != 0f },
                    ATTR_Y1 to y1.takeIf { it != 0f },
                    ATTR_X2 to x2.takeIf { it != 1f },
                    ATTR_Y2 to y2.takeIf { it != 0f },
                    ATTR_GRADIENT_TRANSFORM to transforms?.toSvgTransformList()) {
                for (stop in stops) {
                    TAG_STOP(ATTR_OFFSET to stop.offset,
                            ATTR_STOP_COLOR to stop.color.withAlpha(255),
                            ATTR_STOP_OPACITY to stop.color.a.takeIf { it != 255 }?.opacityToFloatPercent())
                }
            }
        }
        onElementStarted()
    }

    fun font(name: String, file: File) {
        currentXml {
            TAG_STYLE(ATTR_TYPE to "text/css") {
                text("@$CSS_FONT_FACE{$CSS_FONT_FAMILY:$name;" +
                        "$CSS_SRC:url('${file.path}');}")
            }
        }
        onElementStarted()
    }

    fun text(x: Float, y: Float, dx: FloatArray = floatArrayOf(), fontId: String? = null, text: String) {
        currentXml {
            TAG_TEXT(
                    ATTR_ID to defId,
                    ATTR_X to x,
                    ATTR_Y to y,
                    ATTR_DX to buildString { appendValuesList(*dx) },
                    ATTR_FONT_FAMILY to fontId) {
                text(text)
            }
        }
    }

    private fun onElementStarted() {
        if (writingDefs) {
            check(defXml.currentLevel > 1 || defId == null) { "Def already written" }
            defId = null
        }
    }

    /**
     * Get a list of SVG attributes corresponding to the properties changed in the last
     * pushed [SvgGraphicsState] compared to the previous graphics state.
     */
    private fun getNewGraphicsStateAttrs() = arrayOf(
            ATTR_FILL to getNewGraphicsStateProperty { fillColor?.withAlpha(0xFF)?.toStringNoAlpha() },
            ATTR_FILL_OPACITY to getNewGraphicsStateProperty { fillColor?.a?.opacityToFloatPercent() },
            ATTR_STROKE to getNewGraphicsStateProperty { strokeColor?.withAlpha(0xFF)?.toStringNoAlpha() },
            ATTR_STROKE_OPACITY to getNewGraphicsStateProperty { strokeColor?.a?.opacityToFloatPercent() },
            ATTR_STROKE_WIDTH to getNewGraphicsStateProperty { strokeWidth },
            ATTR_STROKE_LINE_JOIN to getNewGraphicsStateProperty { strokeLineJoin }?.svgName,
            ATTR_STROKE_LINE_CAP to getNewGraphicsStateProperty { strokeLineCap }?.svgName,
            ATTR_CLIP_PATH to getNewGraphicsStateProperty { "url(#$clipPathId)" },
            ATTR_CLIP_RULE to getNewGraphicsStateProperty { clipPathRule }?.svgName,
            ATTR_MASK to getNewGraphicsStateProperty { "url(#$maskId)" },
            ATTR_TRANSFORM to getNewGraphicsStateProperty { transforms }?.toSvgTransformList(),
            ATTR_PRESERVE_ASPECT_RATIO to getNewGraphicsStateProperty { preserveAspectRatio }?.toSvg(),
            ATTR_STYLE to getGraphicsStateCssStyle()
    )

    /**
     * Get the new value for a [property], or `null` if value hasn't changed since last state.
     */
    private inline fun <T> getNewGraphicsStateProperty(property: SvgGraphicsState.() -> T?): T? {
        val head = currentGrStateStack.peek()
        val newValue = head.property() ?: return null
        for (grState in currentGrStateStack.listIterator(1)) {
            val oldValue = grState.property() ?: continue
            // Found previously defined value for that property, check if different.
            return newValue.takeIf { it != oldValue }
        }
        // No previously defined value for that property, return new value.
        return newValue
    }

    /**
     * Get the new value of the CSS `style` attribute, or `null` if value hasn't changed since last state.
     */
    private fun getGraphicsStateCssStyle() = getNewGraphicsStateProperty {
        buildString {
            if (mixBlendMode != null) {
                append("$CSS_MIX_BLEND_MODE:${mixBlendMode.svgName};")
                append("$CSS_ISOLATION:$CSS_ISOLATION_ISOLATE;")
            }
        }.takeIf { it.isNotEmpty() }
    }

    private fun Int?.opacityToFloatPercent() = if (this == null) {
        null
    } else {
        (this / 255f * 100f).roundToInt() / 100f
    }

    private enum class State {
        UNINITIALIZED,
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

        private const val ATTR_CLIP_PATH = "clip-path"
        private const val ATTR_CLIP_RULE = "clip-rule"
        private const val ATTR_DATA = "d"
        private const val ATTR_DX = "dx"
        private const val ATTR_ENCODING = "encoding"
        private const val ATTR_FILL = "fill"
        private const val ATTR_FILL_OPACITY = "fill-opacity"
        private const val ATTR_FONT_FAMILY = "font-family"
        private const val ATTR_GRADIENT_TRANSFORM = "gradientTransform"
        private const val ATTR_HEIGHT = "height"
        private const val ATTR_HREF = "href"
        private const val ATTR_ID = "id"
        private const val ATTR_MASK = "mask"
        private const val ATTR_OFFSET = "offset"
        private const val ATTR_PRESERVE_ASPECT_RATIO = "preserveAspectRatio"
        private const val ATTR_STOP_COLOR = "stop-color"
        private const val ATTR_STOP_OPACITY = "stop-opacity"
        private const val ATTR_STROKE = "stroke"
        private const val ATTR_STROKE_LINE_CAP = "stroke-linecap"
        private const val ATTR_STROKE_LINE_JOIN = "stroke-linejoin"
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
        private const val ATTR_Y = "y"
        private const val ATTR_Y1 = "y2"
        private const val ATTR_Y2 = "y2"

        private const val CSS_FONT_FACE = "font-face"
        private const val CSS_FONT_FAMILY = "font-family"
        private const val CSS_ISOLATION = "isolation"
        private const val CSS_ISOLATION_ISOLATE = "isolate"
        private const val CSS_MIX_BLEND_MODE = "mix-blend-mode"
        private const val CSS_SRC = "src"


        private val DEFAULT_GRAPHICS_STATE = SvgGraphicsState(
                fillColor = Color.BLACK,
                strokeColor = Color.BLACK,
                strokeWidth = 0f,
                strokeLineJoin = SvgStrokeLineJoin.Miter,
                strokeLineCap = SvgStrokeLineCap.BUTT,
                clipPathId = null,
                clipPathRule = SvgClipPathRule.NON_ZERO,
                maskId = null,
                transforms = null,
                preserveAspectRatio = SvgPreserveAspectRatio(SvgPreserveAspectRatio.Align.X_MID_Y_MID),
                mixBlendMode = SvgMixBlendMode.NORMAL)

        val NULL_GRAPHICS_STATE = SvgGraphicsState()


    }

}
