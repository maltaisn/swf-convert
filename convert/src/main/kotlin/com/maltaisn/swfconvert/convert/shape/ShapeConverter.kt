/*
 * Copyright (C) 2020 Nicolas Maltais
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 */

// The code was mostly copied from some other place and it works relatively well,
// I have no intention of refactoring it to make it less complex. So let's ignore those.
@file:Suppress("NestedBlockDepth", "ComplexMethod", "LongMethod")

package com.maltaisn.swfconvert.convert.shape

import com.flagstone.transform.fillstyle.FillStyle
import com.flagstone.transform.linestyle.LineStyle
import com.flagstone.transform.shape.Curve
import com.flagstone.transform.shape.Line
import com.flagstone.transform.shape.Shape
import com.maltaisn.swfconvert.convert.context.ConvertContext
import com.maltaisn.swfconvert.convert.shape.ShapeConverter.Edge.CurveEdge
import com.maltaisn.swfconvert.convert.shape.ShapeConverter.Edge.LineEdge
import com.maltaisn.swfconvert.convert.wrapper.toShapeStyleWrapperOrNull
import com.maltaisn.swfconvert.core.image.Color
import com.maltaisn.swfconvert.core.shape.Path
import com.maltaisn.swfconvert.core.shape.PathElement
import com.maltaisn.swfconvert.core.shape.PathElement.ClosePath
import com.maltaisn.swfconvert.core.shape.PathElement.LineTo
import com.maltaisn.swfconvert.core.shape.PathElement.MoveTo
import com.maltaisn.swfconvert.core.shape.PathElement.QuadTo
import com.maltaisn.swfconvert.core.shape.PathFillStyle
import com.maltaisn.swfconvert.core.shape.PathLineStyle
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import javax.inject.Inject

/**
 * Converts SWF shapes to the [Path] intermediate format.
 * Most shape parsing logic was taken from:
 * [https://github.com/claus/as3swf/blob/master/src/com/codeazur/as3swf/data/SWFShape.as].
 *
 * Note that [ClosePath] is never added to the path, but most often than not, the most ends
 * on the same point as it started so the effect is the same.
 */
internal open class ShapeConverter @Inject constructor() {

    protected lateinit var context: ConvertContext

    private lateinit var shape: Shape
    private lateinit var transform: AffineTransform
    private var ignoreStyles = false

    // Used to find image density in subclass.
    protected lateinit var currentTransform: AffineTransform

    private val fillStyles = mutableListOf<PathFillStyle>()
    private val lineStyles = mutableListOf<PathLineStyle>()
    private val fillEdgeMaps = mutableListOf<MutableMap<Int, MutableList<Edge>>>()
    private val lineEdgeMaps = mutableListOf<MutableMap<Int, MutableList<Edge>>>()
    private lateinit var currFillEdgeMap: MutableMap<Int, MutableList<Edge>>
    private lateinit var currLineEdgeMap: MutableMap<Int, MutableList<Edge>>
    private val coordMap = mutableMapOf<Point, MutableList<Edge>>()
    private val reverseCoordMap = mutableMapOf<Point, MutableList<Edge>>()
    private var numGroups = 0

    private val paths = mutableListOf<Path>()

    fun parseShape(
        context: ConvertContext,
        shape: Shape,
        fillStyles: List<FillStyle> = emptyList(),
        lineStyles: List<LineStyle> = emptyList(),
        transform: AffineTransform = IDENTITY_TRANSFORM,
        currentTransform: AffineTransform = IDENTITY_TRANSFORM,
        ignoreStyles: Boolean = false
    ): List<Path> {
        this.context = context
        this.shape = shape

        this.transform = transform
        this.ignoreStyles = ignoreStyles
        this.currentTransform = currentTransform

        this.fillStyles.clear()
        this.lineStyles.clear()
        addFillStyles(fillStyles)
        addLineStyles(lineStyles)

        fillEdgeMaps.clear()
        lineEdgeMaps.clear()
        currFillEdgeMap = mutableMapOf()
        currLineEdgeMap = mutableMapOf()
        numGroups = 0
        createEdgeMaps()

        paths.clear()

        for (groupIndex in 0 until numGroups) {
            createFillPaths(groupIndex)
            if (!ignoreStyles) {
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
            val shapeStyle = shapeRecord.toShapeStyleWrapperOrNull()
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
                createReverseCoordMap(subPath)
                while (subPath.size > 0) {
                    i = 0
                    while (i < subPath.size) {
                        if (prevEdge == null || prevEdge.end == subPath[i].start) {
                            val edge = subPath.removeAt(i)
                            tmpPath += edge
                            removeEdgeFromCoordMap(edge)
                            removeEdgeFromReverseCoordMap(edge)
                            prevEdge = edge
                        } else {
                            val edge = coordMap[prevEdge.end]?.firstOrNull()
                            if (edge != null) {
                                i = subPath.indexOf(edge)
                            } else {
                                val revEdge = reverseCoordMap[prevEdge.end]?.firstOrNull()
                                if (revEdge != null) {
                                    i = subPath.indexOf(revEdge)
                                    val r = revEdge.reverseWithNewFillStyle(revEdge.fillStyleIdx)
                                    coordMap[revEdge.start]?.remove(revEdge)
                                    coordMap.getOrPut(r.start) { mutableListOf() } += r
                                    reverseCoordMap[revEdge.end]?.remove(revEdge)
                                    reverseCoordMap.getOrPut(r.end) { mutableListOf() } += r
                                    subPath[i] = r
                                } else {
                                    i = 0
                                    prevEdge = null
                                }
                            }
                        }
                    }
                }
                edgeMap[styleIdx] = tmpPath
            }
        }
    }

    private fun createCoordMap(path: MutableList<Edge>) {
        coordMap.clear()
        for (edge in path) {
            coordMap.getOrPut(edge.start) { mutableListOf() } += edge
        }
    }

    private fun createReverseCoordMap(path: MutableList<Edge>) {
        reverseCoordMap.clear()
        for (edge in path) {
            reverseCoordMap.getOrPut(edge.end) { mutableListOf() } += edge
        }
    }

    private fun removeEdgeFromCoordMap(edge: Edge) {
        val coordMapList = coordMap[edge.start] ?: return
        if (coordMapList.size == 1) {
            coordMap -= edge.start
        } else {
            coordMapList -= edge
        }
    }

    private fun removeEdgeFromReverseCoordMap(edge: Edge) {
        val coordMapList = reverseCoordMap[edge.end] ?: return
        if (coordMapList.size == 1) {
            reverseCoordMap -= edge.end
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
        private val IDENTITY_TRANSFORM = AffineTransform()

        private const val NO_STYLE_INDEX = Int.MAX_VALUE
        private val NO_POINT = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        private val SOLID_BLACK_FILL = PathFillStyle.Solid(Color.BLACK)
    }

}
