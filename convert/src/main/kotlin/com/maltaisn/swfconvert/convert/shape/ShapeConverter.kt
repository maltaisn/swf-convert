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

package com.maltaisn.swfconvert.convert.shape

import com.flagstone.transform.fillstyle.FillStyle
import com.flagstone.transform.linestyle.LineStyle
import com.flagstone.transform.shape.Curve
import com.flagstone.transform.shape.Line
import com.flagstone.transform.shape.Shape
import com.flagstone.transform.shape.ShapeStyle
import com.flagstone.transform.shape.ShapeStyle2
import com.maltaisn.swfconvert.convert.context.ConvertContext
import com.maltaisn.swfconvert.convert.shape.ShapeConverter.Edge.CurveEdge
import com.maltaisn.swfconvert.convert.shape.ShapeConverter.Edge.LineEdge
import com.maltaisn.swfconvert.convert.wrapper.WShapeStyle
import com.maltaisn.swfconvert.core.image.Color
import com.maltaisn.swfconvert.core.shape.Path
import com.maltaisn.swfconvert.core.shape.PathElement
import com.maltaisn.swfconvert.core.shape.PathElement.LineTo
import com.maltaisn.swfconvert.core.shape.PathElement.MoveTo
import com.maltaisn.swfconvert.core.shape.PathElement.QuadTo
import com.maltaisn.swfconvert.core.shape.PathElement.Rectangle
import com.maltaisn.swfconvert.core.shape.PathFillStyle
import com.maltaisn.swfconvert.core.shape.PathLineStyle
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import javax.inject.Inject

/**
 * Converts SWF shapes to the [Path] intermediate format.
 * Most shape parsing logic was taken from:
 * [https://github.com/claus/as3swf/blob/master/src/com/codeazur/as3swf/data/SWFShape.as].
 */
internal open class ShapeConverter @Inject constructor() {

    protected lateinit var context: ConvertContext

    private lateinit var shape: Shape
    private lateinit var transform: AffineTransform
    private var ignoreStyles = false
    private var allowRectangles = false

    protected lateinit var currentTransform: AffineTransform

    private val fillStyles = mutableListOf<PathFillStyle>()
    private val lineStyles = mutableListOf<PathLineStyle>()
    private val fillEdgeMaps = mutableListOf<MutableMap<Int, MutableList<Edge>>>()
    private val lineEdgeMaps = mutableListOf<MutableMap<Int, MutableList<Edge>>>()
    private lateinit var currFillEdgeMap: MutableMap<Int, MutableList<Edge>>
    private lateinit var currLineEdgeMap: MutableMap<Int, MutableList<Edge>>
    private var coordMap = mutableMapOf<Point, MutableList<Edge>>()
    private var numGroups = 0

    private val paths = mutableListOf<Path>()

    fun parseShape(
        context: ConvertContext,
        shape: Shape,
        fillStyles: List<FillStyle>,
        lineStyles: List<LineStyle>,
        transform: AffineTransform,
        currentTransform: AffineTransform,
        ignoreStyles: Boolean,
        allowRectangles: Boolean
    ): List<Path> {
        this.context = context
        this.shape = shape

        this.fillStyles.clear()
        this.lineStyles.clear()
        addFillStyles(fillStyles)
        addLineStyles(lineStyles)

        this.transform = transform
        this.ignoreStyles = ignoreStyles
        this.allowRectangles = allowRectangles

        this.currentTransform = currentTransform

        fillEdgeMaps.clear()
        lineEdgeMaps.clear()
        currFillEdgeMap = mutableMapOf()
        currLineEdgeMap = mutableMapOf()
        numGroups = 0
        createEdgeMaps()

        paths.clear()
        if (ignoreStyles) {
            for (groupIndex in 0 until numGroups) {
                createFillPaths(groupIndex)
            }

        } else {
            for (groupIndex in 0 until numGroups) {
                createFillPaths(groupIndex)
                createLinePaths(groupIndex)
            }
        }

        return paths.toList()
    }

    private fun createFillPaths(groupIndex: Int) {
        val path = createPathFromEdgeMap(fillEdgeMaps[groupIndex])
        var pos = NO_POINT
        var fillStyleIdx = NO_STYLE_INDEX

        var fillStyle: PathFillStyle? = null
        val elements = mutableListOf<PathElement>()

        for (e in path) {
            if (fillStyleIdx != e.fillStyleIdx) {
                if (fillStyleIdx != NO_STYLE_INDEX) {
                    createPath(elements, fillStyle = fillStyle)
                }
                fillStyleIdx = e.fillStyleIdx
                fillStyle = fillStyles.getOrNull(fillStyleIdx - 1) ?: SOLID_BLACK_FILL
                pos = NO_POINT
            }
            addEdgeToPath(elements, pos, e)
            pos = e.end
        }
        if (fillStyleIdx != NO_STYLE_INDEX) {
            createPath(elements, fillStyle = fillStyle)
        }
    }

    private fun createLinePaths(groupIndex: Int) {
        val path = createPathFromEdgeMap(lineEdgeMaps[groupIndex])
        var pos: Point = NO_POINT
        var lineStyleIdx = NO_STYLE_INDEX

        var lineStyle: PathLineStyle? = null
        val elements = mutableListOf<PathElement>()

        for (e in path) {
            if (lineStyleIdx != e.lineStyleIdx) {
                if (lineStyleIdx != NO_STYLE_INDEX) {
                    createPath(elements, lineStyle = lineStyle)
                }
                lineStyleIdx = e.lineStyleIdx
                pos = NO_POINT
                lineStyle = lineStyles.getOrNull(lineStyleIdx - 1)
            }
            addEdgeToPath(elements, pos, e)
            pos = e.end
        }
        if (lineStyleIdx != NO_STYLE_INDEX) {
            createPath(elements, lineStyle = lineStyle)
        }
    }

    private fun addEdgeToPath(elements: MutableList<PathElement>, pos: Point, e: Edge) {
        if (pos != e.start) {
            // Edge isn't a continuation of the previous subpath. Move current pos.
            val point = e.start.transform(transform)
            elements += MoveTo(point.x.toFloat(), point.y.toFloat())
        }

        val end = e.end.transform(transform)
        elements += when (e) {
            is CurveEdge -> {
                val control = e.control.transform(transform)
                QuadTo(control.x.toFloat(), control.y.toFloat(),
                    end.x.toFloat(), end.y.toFloat())
            }
            is LineEdge -> {
                LineTo(end.x.toFloat(), end.y.toFloat())
            }
        }
    }

    private fun createPath(
        elements: MutableList<PathElement>,
        fillStyle: PathFillStyle? = null,
        lineStyle: PathLineStyle? = null
    ) {
        if (elements.isNotEmpty() && (fillStyle != null || lineStyle != null)) {
            // FIXME rectangle optimization not working correctly, see test class
//            val rect = if (allowRectangles) {
//                convertPathToRectangle(elements)
//            } else {
//                null
//            }
//            paths += Path(if (rect != null) listOf(rect) else elements.toList(),
//                fillStyle.takeUnless { ignoreStyles }, lineStyle.takeUnless { ignoreStyles })
            paths += Path(elements.toList(),
                fillStyle.takeUnless { ignoreStyles }, lineStyle.takeUnless { ignoreStyles })
        }
        elements.clear()
    }

    private fun createEdgeMaps() {
        var xPos = 0
        var yPos = 0
        var fillStyleIdxOffset = 0
        var lineStyleIdxOffset = 0
        var currFillStyleIdx0 = 0
        var currFillStyleIdx1 = 0
        var currLineStyleIdx = 0
        val subPath = mutableListOf<Edge>()

        for (shapeRecord in shape.objects) {
            val shapeStyle = when (shapeRecord) {
                is ShapeStyle -> WShapeStyle(shapeRecord)
                is ShapeStyle2 -> WShapeStyle(shapeRecord)
                else -> null
            }
            when {
                shapeStyle != null -> {
                    if (shapeStyle.lineStyle != null ||
                        shapeStyle.fillStyle0 != null ||
                        shapeStyle.fillStyle1 != null
                    ) {
                        processSubPath(subPath, currLineStyleIdx, currFillStyleIdx0, currFillStyleIdx1)
                        subPath.clear()
                    }
                    if (shapeStyle.lineStyles.isNotEmpty()) {
                        lineStyleIdxOffset = lineStyles.size
                        addLineStyles(shapeStyle.lineStyles)
                    }
                    if (shapeStyle.fillStyles.isNotEmpty()) {
                        fillStyleIdxOffset = fillStyles.size
                        addFillStyles(shapeStyle.fillStyles)
                    }
                    // Check if all styles are reset to 0.
                    // This (probably) means that a new group starts with the next record
                    if (shapeStyle.lineStyle == 0 && shapeStyle.fillStyle0 == 0 && shapeStyle.fillStyle1 == 0) {
                        cleanEdgeMap(currFillEdgeMap)
                        cleanEdgeMap(currLineEdgeMap)
                        fillEdgeMaps += currFillEdgeMap
                        lineEdgeMaps += currLineEdgeMap
                        currFillEdgeMap = mutableMapOf()
                        currLineEdgeMap = mutableMapOf()
                        currLineStyleIdx = 0
                        currFillStyleIdx0 = 0
                        currFillStyleIdx1 = 0
                        numGroups++
                    } else {
                        if (shapeStyle.lineStyle != null) {
                            currLineStyleIdx = shapeStyle.lineStyle
                            if (currLineStyleIdx > 0) {
                                currLineStyleIdx += lineStyleIdxOffset
                            }
                        }
                        if (shapeStyle.fillStyle0 != null) {
                            currFillStyleIdx0 = shapeStyle.fillStyle0
                            if (currFillStyleIdx0 > 0) {
                                currFillStyleIdx0 += fillStyleIdxOffset
                            }
                        }
                        if (shapeStyle.fillStyle1 != null) {
                            currFillStyleIdx1 = shapeStyle.fillStyle1
                            if (currFillStyleIdx1 > 0) {
                                currFillStyleIdx1 += fillStyleIdxOffset
                            }
                        }
                    }
                    if (shapeStyle.moveX != null && shapeStyle.moveY != null) {
                        xPos = shapeStyle.moveX
                        yPos = shapeStyle.moveY
                    }
                }
                shapeRecord is Line -> {
                    val from = Point(xPos, yPos)
                    xPos += shapeRecord.x
                    yPos += shapeRecord.y
                    val to = Point(xPos, yPos)
                    subPath += LineEdge(from, to, currLineStyleIdx, currFillStyleIdx1)
                }
                shapeRecord is Curve -> {
                    val from = Point(xPos, yPos)
                    val control = Point(xPos + shapeRecord.controlX, yPos + shapeRecord.controlY)
                    xPos = control.x + shapeRecord.anchorX
                    yPos = control.y + shapeRecord.anchorY
                    val to = Point(xPos, yPos)
                    subPath += CurveEdge(from, control, to, currLineStyleIdx, currFillStyleIdx1)
                }
            }
        }

        // We're done. Process the last subpath, if any
        processSubPath(subPath, currLineStyleIdx, currFillStyleIdx0, currFillStyleIdx1)
        cleanEdgeMap(currFillEdgeMap)
        cleanEdgeMap(currLineEdgeMap)
        fillEdgeMaps += currFillEdgeMap
        lineEdgeMaps += currLineEdgeMap
        numGroups++
    }

    private fun addFillStyles(fillStyles: List<FillStyle>) {
        for (fillStyle in fillStyles) {
            this.fillStyles += convertFillStyle(fillStyle)
        }
    }

    private fun addLineStyles(lineStyles: List<LineStyle>) {
        for (lineStyle in lineStyles) {
            this.lineStyles += convertLineStyle(lineStyle)
        }
    }

    private fun processSubPath(
        subPath: List<Edge>,
        lineStyleIdx: Int,
        fillStyleIdx0: Int,
        fillStyleIdx1: Int
    ) {
        if (fillStyleIdx0 != 0) {
            // Wrong side is being filled. Reverse the entire path to correct this.
            val path = currFillEdgeMap.getOrPut(fillStyleIdx0) { mutableListOf() }
            for (edge in subPath.asReversed()) {
                path += edge.reverseWithNewFillStyle(fillStyleIdx0)
            }
        }
        if (fillStyleIdx1 != 0) {
            currFillEdgeMap.getOrPut(fillStyleIdx1) { mutableListOf() } += subPath
        }
        if (lineStyleIdx != 0) {
            currLineEdgeMap.getOrPut(lineStyleIdx) { mutableListOf() } += subPath
        }
    }

    private fun createPathFromEdgeMap(edgeMap: Map<Int, MutableList<Edge>>): List<Edge> {
        // Create single list of edges from all lists in the edge map.
        val newPath = mutableListOf<Edge>()
        for (styleIdx in edgeMap.keys.sorted()) {
            newPath += edgeMap[styleIdx] ?: continue
        }
        return newPath
    }

    private fun cleanEdgeMap(edgeMap: MutableMap<Int, MutableList<Edge>>) {
        // This seems to connect edges together? 
        for ((styleIdx, subPath) in edgeMap) {
            if (subPath.isNotEmpty()) {
                var i: Int
                var prevEdge: Edge? = null
                val tmpPath = mutableListOf<Edge>()
                createCoordMap(subPath)
                while (subPath.size > 0) {
                    i = 0
                    while (i < subPath.size) {
                        if (prevEdge == null || prevEdge.end == subPath[i].start) {
                            val edge = subPath.removeAt(i)
                            tmpPath += edge
                            removeEdgeFromCoordMap(edge)
                            prevEdge = edge
                        } else {
                            val edge = coordMap[prevEdge.end]?.firstOrNull()
                            if (edge != null) {
                                i = subPath.indexOf(edge)
                            } else {
                                i = 0
                                prevEdge = null
                            }
                        }
                    }
                }
                edgeMap[styleIdx] = tmpPath
            }
        }
    }

    private fun createCoordMap(path: MutableList<Edge>) {
        coordMap = mutableMapOf()
        for (edge in path) {
            coordMap.getOrPut(edge.start) { mutableListOf() } += edge
        }
    }

    private fun removeEdgeFromCoordMap(edge: Edge) {
        val coordMapList = coordMap[edge.start] ?: return
        if (coordMapList.size == 1) {
            coordMap.remove(edge.start)
        } else {
            coordMapList -= edge
        }
    }

    protected open fun convertFillStyle(fillStyle: FillStyle): PathFillStyle {
        throw UnsupportedOperationException("Can't convert fill style")
    }

    protected open fun convertLineStyle(lineStyle: LineStyle): PathLineStyle {
        throw UnsupportedOperationException("Can't convert fill style")
    }

    private data class Point(val x: Int, val y: Int) {
        fun transform(transform: AffineTransform): Point2D {
            val src = java.awt.Point(x, y)
            return transform.transform(src, null)
        }
    }

    private sealed class Edge {
        abstract val start: Point
        abstract val end: Point
        abstract val lineStyleIdx: Int
        abstract val fillStyleIdx: Int

        abstract fun reverseWithNewFillStyle(newFillStyleIdx: Int): Edge

        data class LineEdge(
            override val start: Point,
            override val end: Point,
            override val lineStyleIdx: Int,
            override val fillStyleIdx: Int
        ) : Edge() {

            override fun reverseWithNewFillStyle(newFillStyleIdx: Int) =
                LineEdge(end, start, lineStyleIdx, newFillStyleIdx)
        }

        data class CurveEdge(
            override val start: Point,
            val control: Point,
            override val end: Point,
            override val lineStyleIdx: Int,
            override val fillStyleIdx: Int
        ) : Edge() {

            override fun reverseWithNewFillStyle(newFillStyleIdx: Int) =
                CurveEdge(end, control, start, lineStyleIdx, newFillStyleIdx)
        }
    }

    companion object {
        private const val NO_STYLE_INDEX = Int.MAX_VALUE
        private val NO_POINT = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        private val SOLID_BLACK_FILL = PathFillStyle.Solid(Color.BLACK)

        internal fun convertPathToRectangle(elements: List<PathElement>): Rectangle? {
            TODO()
//            if (elements.size != 5) {
//                return null
//            }
//
//            val first = elements[0]
//            var last: PathElement = first
//            var lastWasHorizontal: Boolean? = null
//            var width = 0f
//            var height = 0f
//            for (i in 1 until elements.size) {
//                val e = elements[i]
//                if (e !is LineTo) return null
//                val dx = e.x - last.x
//                val dy = e.y - last.y
//                when {
//                    dy == 0f -> {
//                        if (lastWasHorizontal == true) return null
//                        if (width == 0f) {
//                            width = dx
//                        } else if (width != -dx) {
//                            return null
//                        }
//                        lastWasHorizontal = true
//                    }
//                    dx == 0f -> {
//                        if (lastWasHorizontal == false) return null
//                        if (height == 0f) {
//                            height = dy
//                        } else if (height != -dy) {
//                            return null
//                        }
//                        lastWasHorizontal = false
//                    }
//                    else -> return null
//                }
//                last = e
//            }
//
//            return Rectangle(first.x, first.y, width, height)
        }
    }

}
