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

package com.maltaisn.swfconvert.convert.shape

import com.flagstone.transform.MovieTag
import com.flagstone.transform.fillstyle.*
import com.flagstone.transform.image.ImageTag
import com.flagstone.transform.linestyle.*
import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.convert.conversionError
import com.maltaisn.swfconvert.convert.image.CompositeColorTransform
import com.maltaisn.swfconvert.convert.image.ImageDecoder
import com.maltaisn.swfconvert.convert.toAffineTransform
import com.maltaisn.swfconvert.convert.toColor
import com.maltaisn.swfconvert.convert.wrapper.WLineStyle
import com.maltaisn.swfconvert.core.Disposable
import com.maltaisn.swfconvert.core.Units
import com.maltaisn.swfconvert.core.shape.GradientColor
import com.maltaisn.swfconvert.core.shape.PathFillStyle
import com.maltaisn.swfconvert.core.shape.PathLineStyle
import java.awt.BasicStroke
import java.awt.geom.AffineTransform
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.sqrt


/**
 * Extends the functionality of [ShapeConverter] by allowing fill and line styles.
 */
internal class StyledShapeConverter @Inject constructor(
        private val config: ConvertConfiguration,
        private val imageDecoder: ImageDecoder
) : ShapeConverter(), Disposable {

    private lateinit var objectsMap: Map<Int, MovieTag>
    private lateinit var colorTransform: CompositeColorTransform

    /**
     * Initialize this shape converter with resources.
     * @param objectsMap Used to get image tags from SWF.
     * @param colorTransform Color transform applied on fill and line styles.
     */
    fun initialize(objectsMap: Map<Int, MovieTag>,
                   colorTransform: CompositeColorTransform) {
        this.objectsMap = objectsMap
        this.colorTransform = colorTransform
    }

    override fun convertFillStyle(fillStyle: FillStyle) = when (fillStyle) {
        is SolidFill -> {
            PathFillStyle.Solid(colorTransform.transform(fillStyle.color.toColor()))
        }
        is BitmapFill -> {
            // Only bitmap fill of type 0x41 is supported, i.e, clipped non-smoothed.
            conversionError(fillStyle.isTiled && !fillStyle.isSmoothed, context) {
                "Unsupported bitmap fill type"
            }

            val image = objectsMap[fillStyle.identifier] as? ImageTag
                    ?: conversionError(context, "Invalid image ID ${fillStyle.identifier}")

            val tr = fillStyle.transform.toAffineTransform(config.yAxisDirection)
            tr.scale(image.width.toDouble(), image.height.toDouble())

            // Create image data
            val density = findImageDensity(image, tr)
            val imageData = imageDecoder.convertImage(context, image, colorTransform, density)

            PathFillStyle.Image(image.identifier, tr, imageData, !config.disableClipping)
        }
        is GradientFill -> {
            conversionError(fillStyle.spread == Spread.PAD, context) {
                "Unsupported gradient spread mode ${fillStyle.spread}"
            }
            conversionError(fillStyle.interpolation == Interpolation.NORMAL, context) {
                "Unsupported gradient interpolation mode ${fillStyle.interpolation}"
            }

            // SWF gradients have a size of 32768, offset of 16384 and a transform.
            val transform = fillStyle.transform.toAffineTransform(config.yAxisDirection)
            transform.concatenate(AffineTransform(32768f, 0f, 0f, 32768f, -16384f, -16384f))

            val colors = fillStyle.gradients.map {
                val color = colorTransform.transform(it.color.toColor())
                val ratio = it.ratio / 255f
                GradientColor(color, ratio)
            }

            PathFillStyle.Gradient(colors, transform)
        }
        else -> {
            conversionError(context, "Unsupported shape fill style ${fillStyle.javaClass.simpleName}")
        }
    }

    override fun dispose() {
        imageDecoder.dispose()
    }

    private fun findImageDensity(image: ImageTag, imageTransform: AffineTransform): Float {
        // Find total transform (current + image transforms)
        val tr = AffineTransform(currentTransform)
        tr.preConcatenate(imageTransform)

        // Find length of vectors [0 1] and [1 0], in other words, image width and height (in inches).
        val width = sqrt(tr.scaleX * tr.scaleX + tr.shearY * tr.shearY) / Units.INCH_TO_POINTS
        val height = sqrt(tr.scaleY * tr.scaleY + tr.shearX * tr.shearX) / Units.INCH_TO_POINTS

        // Find minimum density. Not sure if these are always equal.
        val xDensity = image.width / width
        val yDensity = image.height / height

        return min(xDensity, yDensity).toFloat()
    }

    override fun convertLineStyle(lineStyle: LineStyle): PathLineStyle {
        val wstyle = when (lineStyle) {
            is LineStyle1 -> WLineStyle(lineStyle)
            is LineStyle2 -> {
                conversionError(lineStyle.fillStyle == null, context) {
                    "Unsupported line fill style"
                }
                conversionError(lineStyle.startCap == lineStyle.endCap, context) {
                    "Unsupported different start and end caps on line style"
                }
                WLineStyle(lineStyle)
            }
            else -> conversionError(context, "Unknown line style ${lineStyle.javaClass.simpleName}")
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

}
