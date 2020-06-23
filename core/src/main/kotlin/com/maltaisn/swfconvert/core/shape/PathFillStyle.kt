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
