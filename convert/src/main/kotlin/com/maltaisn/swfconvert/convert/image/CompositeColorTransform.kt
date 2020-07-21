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

package com.maltaisn.swfconvert.convert.image

import com.flagstone.transform.datatype.ColorTransform
import com.maltaisn.swfconvert.core.image.Color
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

internal data class CompositeColorTransform(
    private val transforms: ArrayDeque<ColorTransform> = ArrayDeque()
) {

    fun clear() = transforms.clear()

    fun push(colorTransform: ColorTransform) {
        transforms += colorTransform
    }

    fun pop() = transforms.removeLast()

    /**
     * Return a [color] transformed with the color transforms.
     */
    fun transform(color: Color): Color {
        if (transforms.isEmpty()) {
            return color
        }
        var a = color.a
        var r = color.r
        var g = color.g
        var b = color.b
        for (tr in transforms) {
            a = (a * tr.multiplyAlpha + tr.addAlpha).roundToInt().coerceAtMost(Color.COMPONENT_MAX)
            r = (r * tr.multiplyRed + tr.addRed).roundToInt().coerceAtMost(Color.COMPONENT_MAX)
            g = (g * tr.multiplyGreen + tr.addGreen).roundToInt().coerceAtMost(Color.COMPONENT_MAX)
            b = (b * tr.multiplyBlue + tr.addBlue).roundToInt().coerceAtMost(Color.COMPONENT_MAX)
        }
        return Color(r, g, b, a)
    }

    /**
     * Transform an [image] in place with the color transforms.
     */
    fun transform(image: BufferedImage) {
        if (transforms.isEmpty()) {
            return
        }
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val color = Color(image.getRGB(x, y))
                val transformed = transform(color).value
                image.setRGB(x, y, transformed)
            }
        }
    }

}
