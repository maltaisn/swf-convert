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

package com.maltaisn.swfconvert.render.ir

import com.maltaisn.swfconvert.core.FrameGroup
import com.maltaisn.swfconvert.core.FrameObject
import com.maltaisn.swfconvert.core.GroupObject
import com.maltaisn.swfconvert.core.shape.Path
import com.maltaisn.swfconvert.core.shape.PathFillStyle
import com.maltaisn.swfconvert.core.shape.PathLineStyle
import com.maltaisn.swfconvert.core.shape.ShapeObject
import com.maltaisn.swfconvert.core.text.TextObject
import com.maltaisn.swfconvert.render.ir.data.IrGradientColor
import com.maltaisn.swfconvert.render.ir.data.IrImageData
import com.maltaisn.swfconvert.render.ir.data.IrObject
import com.maltaisn.swfconvert.render.ir.data.IrPath
import com.maltaisn.swfconvert.render.ir.data.IrPathFillStyle
import com.maltaisn.swfconvert.render.ir.data.IrPathLineStyle
import com.maltaisn.swfconvert.render.ir.data.IrRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import java.io.File
import javax.inject.Inject

internal class IrFrameRenderer @Inject constructor(
    private val config: IrConfiguration
) {

    suspend fun renderFrame(outputFile: File, frame: FrameGroup) {
        // Serialize the frame group to JSON.
        val json = Json(JsonConfiguration.Stable.copy(
            prettyPrint = config.prettyPrint,
            indent = if (config.prettyPrint) " ".repeat(config.indentSize) else "    ",
            encodeDefaults = false))
        val serializableFrame = frame.toSerializable()
        val frameJson = json.stringify(IrObject.serializer(), serializableFrame)

        // Save output to file.
        withContext(Dispatchers.IO) {
            outputFile.writeText(frameJson)
        }
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
            is GroupObject.Masked -> IrObject.MaskedGroup(id, bounds.toSerializable(),
                objects.dropLast(1), objects.last())
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
            font.fontFile?.nameWithoutExtension, text, glyphIndices, glyphOffsets)

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
