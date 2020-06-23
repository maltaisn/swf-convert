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

package com.maltaisn.swfconvert.render.ir.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class IrPath(
    val data: String,
    val fillStyle: IrPathFillStyle? = null,
    val lineStyle: IrPathLineStyle? = null
)

@Serializable
internal sealed class IrPathFillStyle {

    @Serializable
    @SerialName("solid")
    data class Solid(val color: String) : IrPathFillStyle()

    @Serializable
    @SerialName("image")
    data class Image(
        val id: Int,
        val transform: String,
        val image: IrImageData
    ) : IrPathFillStyle()

    @Serializable
    @SerialName("gradient")
    data class Gradient(
        val colors: List<IrGradientColor>,
        val transform: String
    ) : IrPathFillStyle()
}

@Serializable
internal data class IrImageData(
    val dataFile: String,
    val alphaDataFile: String? = null
)

@Serializable
internal data class IrGradientColor(
    val color: String,
    val ratio: Float
)

@Serializable
internal data class IrPathLineStyle(
    val color: String,
    val width: Float,
    val cap: Int,
    val join: Int,
    val miterLimit: Float
)
