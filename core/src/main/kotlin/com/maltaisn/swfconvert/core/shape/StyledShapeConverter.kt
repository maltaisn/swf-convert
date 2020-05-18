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

import com.flagstone.transform.MovieTag
import com.flagstone.transform.fillstyle.*
import com.flagstone.transform.image.ImageTag
import com.flagstone.transform.linestyle.*
import com.maltaisn.swfconvert.core.Configuration
import com.maltaisn.swfconvert.core.conversionError
import com.maltaisn.swfconvert.core.image.CompositeColorTransform
import com.maltaisn.swfconvert.core.image.ImageDecoder
import com.maltaisn.swfconvert.core.shape.data.GradientColor
import com.maltaisn.swfconvert.core.shape.data.WLineStyle
import com.maltaisn.swfconvert.core.shape.path.PathFillStyle
import com.maltaisn.swfconvert.core.shape.path.PathLineStyle
import com.maltaisn.swfconvert.core.toAffineTransform
import com.maltaisn.swfconvert.core.toColor
import java.awt.BasicStroke
import java.awt.geom.AffineTransform
import kotlin.math.min
import kotlin.math.sqrt


/**
 * Extends the functionality of [ShapeConverter] by allowing fill and line styles.
 */
class StyledShapeConverter(
        private val objectsMap: Map<Int, MovieTag>,
        private val colorTransform: CompositeColorTransform,
        private val config: Configuration
) : ShapeConverter() {

    private val imageDecoder = ImageDecoder(config)

    override fun convertFillStyle(fillStyle: FillStyle) = when (fillStyle) {
        is SolidFill -> {
            PathFillStyle.Solid(colorTransform.transform(fillStyle.color.toColor()))
        }
        is BitmapFill -> {
            // Only bitmap fill of type 0x41 is supported, i.e, clipped non-smoothed.
            conversionError(fillStyle.isTiled && !fillStyle.isSmoothed) {
                "Unsupported bitmap fill type"
            }

            val image = objectsMap[fillStyle.identifier] as? ImageTag ?: error("Invalid image ID")

            val tr = fillStyle.transform.toAffineTransform()
            tr.scale(image.width.toDouble(), image.height.toDouble())

            // Create image data
            val density = findImageDensity(image, tr)
            val imageData = imageDecoder.convertImage(image, colorTransform, density)

            PathFillStyle.Image(image.identifier, tr, imageData)
        }
        is GradientFill -> {
            conversionError(fillStyle.spread == Spread.PAD) { "Unsupported spread mode" }
            conversionError(fillStyle.interpolation == Interpolation.NORMAL) { "Unsupported interpolation mode" }

            // SWF gradients have a size of 32768, offset of 16384 and a transform.
            val transform = fillStyle.transform.toAffineTransform()
            transform.concatenate(AffineTransform(32768f, 0f, 0f, 32768f, -16384f, -16384f))

            val colors = fillStyle.gradients.map {
                val color = colorTransform.transform(it.color.toColor())
                val ratio = it.ratio / 255f
                GradientColor(color, ratio)
            }

            PathFillStyle.Gradient(colors, transform)
        }
        else -> {
            conversionError("Unsupported shape fill style")
        }
    }

    override fun dispose() {
        super.dispose()
        imageDecoder.dispose()
    }

    private fun findImageDensity(image: ImageTag, imageTransform: AffineTransform): Float {
        // Find total transform (current + image transforms)
        val tr = AffineTransform(currentTransform)
        tr.preConcatenate(imageTransform)

        // Find length of vectors [0 1] and [1 0], in other words, image width and height (in inches).
        val width = sqrt(tr.scaleX * tr.scaleX + tr.shearY * tr.shearY) / POINTS_PER_INCH
        val height = sqrt(tr.scaleY * tr.scaleY + tr.shearX * tr.shearX) / POINTS_PER_INCH

        // Find minimum density. Not sure if these are always equal.
        val xDensity = image.width / width
        val yDensity = image.height / height

        return min(xDensity, yDensity).toFloat()
    }

    override fun convertLineStyle(lineStyle: LineStyle): PathLineStyle {
        val wstyle = when (lineStyle) {
            is LineStyle1 -> WLineStyle(lineStyle)
            is LineStyle2 -> {
                conversionError(lineStyle.fillStyle == null) { "Unsupported line fill style" }
                conversionError(lineStyle.startCap == lineStyle.endCap) { "Unsupported different start and end caps" }
                WLineStyle(lineStyle)
            }
            else -> conversionError("Unknown line style")
        }
        return PathLineStyle(wstyle.color.toColor(), wstyle.width.toFloat(),
                wstyle.capStyle?.toBasicStrokeConstant() ?: BasicStroke.CAP_BUTT,
                wstyle.joinStyle?.toBasicStrokeConstant() ?: BasicStroke.JOIN_BEVEL,
                wstyle.miterLimit.toFloat())
    }

    private fun CapStyle.toBasicStrokeConstant() = when (this) {
        CapStyle.NONE -> BasicStroke.CAP_BUTT
        CapStyle.ROUND -> BasicStroke.CAP_ROUND
        CapStyle.SQUARE -> BasicStroke.CAP_SQUARE
    }

    private fun JoinStyle.toBasicStrokeConstant() = when (this) {
        JoinStyle.BEVEL -> BasicStroke.JOIN_BEVEL
        JoinStyle.ROUND -> BasicStroke.JOIN_ROUND
        JoinStyle.MITER -> BasicStroke.JOIN_MITER
    }

    companion object {
        private const val POINTS_PER_INCH = 72
    }

}
