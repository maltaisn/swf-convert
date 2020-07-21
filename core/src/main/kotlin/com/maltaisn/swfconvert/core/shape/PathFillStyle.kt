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

package com.maltaisn.swfconvert.core.shape

import com.maltaisn.swfconvert.core.image.Color
import com.maltaisn.swfconvert.core.image.ImageData
import com.maltaisn.swfconvert.core.shape.PathFillStyle.Gradient.Companion.OFFSET
import com.maltaisn.swfconvert.core.shape.PathFillStyle.Gradient.Companion.SIZE
import java.awt.geom.AffineTransform

sealed class PathFillStyle {

    /**
     * Path solid fill [color].
     */
    data class Solid(val color: Color) : PathFillStyle()

    /**
     * Path image fill. The image [transform] should map image space (a 1x1 square)
     * to the user space where it's drawn.
     *
     * @param imageData Data of the image to use.
     * @param clip Whether to clip image to the path or not.
     */
    data class Image(
        val id: Int,
        val transform: AffineTransform,
        var imageData: ImageData,
        val clip: Boolean
    ) : PathFillStyle()

    /**
     * Path gradient fill. A gradient has a size of [SIZE] and an offset of [OFFSET].
     * The [transform] should map this space to the user space where it's drawn. Gradients are defined
     * exactly like this in the SWF specification.
     * There should be at least 2 [colors] in the gradient.
     */
    data class Gradient(
        val colors: List<GradientColor>,
        val transform: AffineTransform
    ) : PathFillStyle() {

        init {
            require(colors.size >= 2) { "Gradient fill must have at least 2 colors." }
        }

        companion object {
            const val SIZE = 32768f
            const val OFFSET = -16384f
        }
    }

}
