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

package com.maltaisn.swfconvert.render.ir

import com.maltaisn.swfconvert.core.CoreConfiguration
import com.maltaisn.swfconvert.core.frame.data.*
import com.maltaisn.swfconvert.core.shape.path.Path
import com.maltaisn.swfconvert.core.shape.path.PathFillStyle
import com.maltaisn.swfconvert.core.shape.path.PathLineStyle
import com.maltaisn.swfconvert.render.ir.data.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import javax.inject.Inject


class IrFrameRenderer @Inject constructor(
        private val config: CoreConfiguration,
        private val irConfig: IrConfiguration
) {

    fun renderFrame(index: Int, frame: FrameGroup) {
        // Serialize the frame group to JSON.
        val json = Json(JsonConfiguration.Stable.copy(
                prettyPrint = irConfig.prettyPrint,
                encodeDefaults = false))
        val serializableFrame = frame.toSerializable()
        val frameJson = json.stringify(IrObject.serializer(), serializableFrame)

        // Save output to file.
        // TODO handle error
        config.output[index].writeText(frameJson)
    }

    private fun FrameObject.toSerializable(): IrObject = when (this) {
        is GroupObject -> toSerializable()
        is ShapeObject -> toSerializable()
        is TextObject -> toSerializable()
        else -> error("")
    }

    private fun GroupObject.toSerializable(): IrObject {
        val objects = this.objects.map { it.toSerializable() }
        return when (this) {
            is FrameGroup -> IrObject.FrameGroup(id, width, height, actualWidth, actualHeight, padding,
                    transform.toMatrixString(), objects)
            is GroupObject.Simple -> IrObject.SimpleGroup(id, objects)
            is GroupObject.Transform -> IrObject.TransformGroup(id, transform.toMatrixString(), objects)
            is GroupObject.Blend -> IrObject.BlendGroup(id, blendMode, objects)
            is GroupObject.Clip -> IrObject.ClipGroup(id, clips.map { it.toSerializable() }, objects)
            is GroupObject.Masked -> IrObject.MaskedGroup(id, bounds.toSerializable(), objects)
        }
    }

    private fun ShapeObject.toSerializable() = IrObject.Shape(id, paths.map { it.toSerializable() })

    private fun Path.toSerializable() = IrPath(toSvg(),
            fillStyle?.toSerializable(), lineStyle?.toSerializable())

    private fun PathFillStyle.toSerializable(): IrPathFillStyle = when (this) {
        is PathFillStyle.Solid -> IrPathFillStyle.Solid(color.toString())
        is PathFillStyle.Image -> IrPathFillStyle.Image(id, transform.toMatrixString(),
                IrImageData(imageData.dataFile!!.name, imageData.alphaDataFile?.name))
        is PathFillStyle.Gradient -> IrPathFillStyle.Gradient(
                colors.map { IrGradientColor(it.color.toString(), it.ratio) },
                transform.toMatrixString())
    }

    private fun PathLineStyle.toSerializable() =
            IrPathLineStyle(color.toString(), width, cap, join, miterLimit)

    private fun TextObject.toSerializable() =
            IrObject.Text(id, x, y, fontSize, color.toString(),
                    font.fontFile?.nameWithoutExtension, text, glyphOffsets)

    private fun Rectangle2D.toSerializable() = IrRectangle(x, y, width, height)

    private fun AffineTransform.toMatrixString() = buildString {
        // Components are converted to float because SWF [CoordTransform] was
        // originally using floats, so using double could lead to "0.300000000002" kind of results.
        append("[[")
        append(scaleX.toFloat())
        append(' ')
        append(shearX.toFloat())
        append(' ')
        append(translateX.toFloat())
        append("] [")
        append(shearY.toFloat())
        append(' ')
        append(scaleY.toFloat())
        append(' ')
        append(translateY.toFloat())
        append("]]")
    }

}
