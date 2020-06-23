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
        val objects: List<IrObject>
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
