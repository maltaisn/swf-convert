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

package com.maltaisn.swfconvert.render.svg.writer

import com.maltaisn.swfconvert.render.svg.writer.format.appendValuesList
import com.maltaisn.swfconvert.render.svg.writer.format.appendValuesListOptimized
import com.maltaisn.swfconvert.render.svg.writer.format.createNumberFormat
import com.maltaisn.swfconvert.render.svg.writer.format.formatOptimized
import com.maltaisn.swfconvert.render.svg.writer.format.requireSvgPrecision
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Class used to write SVG paths with numbers using a certain [precision].
 * Path strings are also optimized to reduce size.
 */
internal class SvgPathWriter(
    private val precision: Int,
    private val optimize: Boolean = true
) {

    init {
        requireSvgPrecision(precision)
    }

    private val numberFmt = createNumberFormat(precision)

    private val precisionMult = 10f.pow(precision)

    private val sb = StringBuilder()

    private val absoluteSb = StringBuilder()
    private val relativeSb = StringBuilder()

    private var lastCommand = '_'
    private var lastNumberStr = ""

    private var moveToX = 0f
    private var moveToY = 0f
    private var currentX = 0f
    private var currentY = 0f

    fun moveTo(x: Float, y: Float) {
        moveToX = x.roundToPrecision()
        moveToY = y.roundToPrecision()
        appendShortestCommand('M', x, y,
            getFormattedValues(x, y),
            getFormattedValues(x - currentX, y - currentY))
    }

    fun lineTo(x: Float, y: Float) {
        val absVals = getFormattedValues(x, y)
        val relVals = getFormattedValues(x - currentX, y - currentY)

        val rxStr = relVals.first()
        val ryStr = relVals.last()
        val rxIsZero = rxStr == "0"
        val ryIsZero = ryStr == "0"

        when {
            rxIsZero && ryIsZero -> {
                // Invisible line, don't write it.
                return
            }
            rxIsZero -> {
                // X didn't change from current position
                appendShortestCommand('V', x, y,
                    arrayOf(absVals.last()), arrayOf(relVals.last()))
            }
            ryIsZero -> {
                // Y didn't change from current position
                appendShortestCommand('H', x, y,
                    arrayOf(absVals.first()), arrayOf(relVals.first()))
            }
            else -> appendShortestCommand('L', x, y, absVals, relVals)
        }
    }

    fun quadTo(c1x: Float, c1y: Float, x: Float, y: Float) {
        val relVals = getFormattedValues(c1x - currentX, c1y - currentY,
            x - currentX, y - currentY)
        @Suppress("MagicNumber")
        if (relVals[2] == "0" && relVals[3] == "0") {
            // Invisible quad curve, don't write it.
            return
        }
        appendShortestCommand('Q', x, y,
            getFormattedValues(c1x, c1y, x, y),
            relVals)
    }

    fun cubicTo(c1x: Float, c1y: Float, c2x: Float, c2y: Float, x: Float, y: Float) {
        val relVals = getFormattedValues(c1x - currentX, c1y - currentY,
            c2x - currentX, c2y - currentY,
            x - currentX, y - currentY)
        @Suppress("MagicNumber")
        if (relVals[4] == "0" && relVals[5] == "0") {
            // Invisible cubic curve, don't write it.
            return
        }
        appendShortestCommand('C', x, y,
            getFormattedValues(c1x, c1y, c2x, c2y, x, y),
            relVals)
    }

    fun closePath() {
        if (!optimize && sb.isNotEmpty()) {
            sb.append(' ')
        }
        sb.append('Z')
        lastCommand = 'Z'
        currentX = moveToX
        currentY = moveToY
    }

    private fun getFormattedValues(vararg values: Float) =
        Array(values.size) { values[it].formatOptimized(numberFmt) }

    private fun appendShortestCommand(
        command: Char,
        x: Float,
        y: Float,
        absoluteValues: Array<String>,
        relativeValues: Array<String>
    ) {
        if (!optimize) {
            sb.appendCommand(command, absoluteValues)
            lastNumberStr = absoluteValues.last()
            currentX = x.roundToPrecision()
            currentY = y.roundToPrecision()
            return
        }

        val absCommand = command.uppercaseChar()
        val relCommand = command.lowercaseChar()

        absoluteSb.clear()
        absoluteSb.appendOptimizedCommand(absCommand, absoluteValues)

        relativeSb.clear()
        relativeSb.appendOptimizedCommand(relCommand, relativeValues)

        lastCommand = if (relativeSb.length < absoluteSb.length) {
            sb.append(relativeSb)
            lastNumberStr = relativeValues.last()
            currentX += (x - currentX).roundToPrecision()
            currentY += (y - currentY).roundToPrecision()
            relCommand
        } else {
            sb.append(absoluteSb)
            lastNumberStr = absoluteValues.last()
            currentX = x.roundToPrecision()
            currentY = y.roundToPrecision()
            absCommand
        }
    }

    private fun Float.roundToPrecision() = (this * precisionMult).roundToInt() / precisionMult

    private fun StringBuilder.appendCommand(command: Char, values: Array<String>) {
        if (this.isNotEmpty()) {
            append(' ')
        }
        append(command)
        append(' ')
        appendValuesList(values)
    }

    private fun StringBuilder.appendOptimizedCommand(command: Char, values: Array<String>) {
        var lastNbStr = lastNumberStr

        val isSameCommandExceptMoveTo = lastCommand == command && command != 'M' && command != 'm'
        val hasImplicitLineToAbs = lastCommand == 'M' && command == 'L'
        val hasImplicitLineToRel = lastCommand == 'm' && command == 'l'

        if (!isSameCommandExceptMoveTo && !hasImplicitLineToAbs && !hasImplicitLineToRel) {
            // Command cannot be omitted.
            append(command)
            lastNbStr = ""
        }

        // Append all values for that command
        appendValuesListOptimized(lastNbStr, values)
    }

    override fun toString() = sb.toString()

}
