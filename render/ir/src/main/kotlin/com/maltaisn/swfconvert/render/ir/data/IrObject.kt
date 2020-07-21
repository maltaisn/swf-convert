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

import com.maltaisn.swfconvert.core.BlendMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class IrObject {

    @Serializable
    @SerialName("frame_group")
    data class FrameGroup(
        val id: Int,
        val width: Float,
        val height: Float,
        val actualWidth: Float,
        val actualHeight: Float,
        val padding: Float,
        val transform: String,
        val objects: List<IrObject>
    ) : IrObject()

    @Serializable
    @SerialName("simple_group")
    data class SimpleGroup(
        val id: Int,
        val objects: List<IrObject>
    ) : IrObject()

    @Serializable
    @SerialName("transform_group")
    data class TransformGroup(
        val id: Int,
        val transform: String,
        val objects: List<IrObject>
    ) : IrObject()

    @Serializable
    @SerialName("blend_group")
    data class BlendGroup(
        val id: Int,
        val blendMode: BlendMode,
        val objects: List<IrObject>
    ) : IrObject()

    @Serializable
    @SerialName("clip_group")
    data class ClipGroup(
        val id: Int,
        val clips: List<IrPath>,
        val objects: List<IrObject>
    ) : IrObject()

    @Serializable
    @SerialName("masked_group")
    data class MaskedGroup(
        val id: Int,
        val bounds: IrRectangle,
        val objects: List<IrObject>,
        val mask: IrObject
    ) : IrObject()

    @Serializable
    @SerialName("shape")
    data class Shape(
        val id: Int,
        val paths: List<IrPath>
    ) : IrObject()

    @Serializable
    @SerialName("text")
    data class Text(
        val id: Int,
        val x: Float,
        val y: Float,
        val fontSize: Float,
        val color: String,
        val font: String?,
        val text: String,
        val glyphIndices: List<Int>,
        val glyphOffsets: List<Float>
    ) : IrObject()

}

@Serializable
internal data class IrRectangle(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
)
