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
