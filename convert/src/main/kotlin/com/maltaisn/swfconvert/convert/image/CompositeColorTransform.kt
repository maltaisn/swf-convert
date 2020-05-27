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

package com.maltaisn.swfconvert.convert.image

import com.flagstone.transform.datatype.ColorTransform
import com.maltaisn.swfconvert.core.image.Color
import java.awt.image.BufferedImage
import java.util.*
import kotlin.math.roundToInt


data class CompositeColorTransform(val transforms: LinkedList<ColorTransform> = LinkedList()) {

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
            a = (a * tr.multiplyAlpha + tr.addAlpha).roundToInt().coerceAtMost(0xFF)
            r = (r * tr.multiplyRed + tr.addRed).roundToInt().coerceAtMost(0xFF)
            g = (g * tr.multiplyGreen + tr.addGreen).roundToInt().coerceAtMost(0xFF)
            b = (b * tr.multiplyBlue + tr.addBlue).roundToInt().coerceAtMost(0xFF)
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
