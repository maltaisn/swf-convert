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

package com.maltaisn.swfconvert.core.shape.path

import com.maltaisn.swfconvert.core.image.data.Color
import com.maltaisn.swfconvert.core.image.data.ImageData
import com.maltaisn.swfconvert.core.shape.data.GradientColor
import java.awt.geom.AffineTransform


sealed class PathFillStyle {

    data class Solid(val color: Color) : PathFillStyle()

    data class Image(val id: Int, val transform: AffineTransform, var imageData: ImageData) : PathFillStyle()

    data class Gradient(val colors: List<GradientColor>, val transform: AffineTransform) : PathFillStyle()

}
