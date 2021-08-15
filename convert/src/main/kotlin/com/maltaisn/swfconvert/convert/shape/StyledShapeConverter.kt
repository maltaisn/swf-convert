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

package com.maltaisn.swfconvert.convert.shape

import com.flagstone.transform.fillstyle.BitmapFill
import com.flagstone.transform.fillstyle.FillStyle
import com.flagstone.transform.fillstyle.GradientFill
import com.flagstone.transform.fillstyle.Interpolation
import com.flagstone.transform.fillstyle.SolidFill
import com.flagstone.transform.fillstyle.Spread
import com.flagstone.transform.image.ImageTag
import com.flagstone.transform.linestyle.CapStyle
import com.flagstone.transform.linestyle.JoinStyle
import com.flagstone.transform.linestyle.LineStyle
import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.convert.conversionError
import com.maltaisn.swfconvert.convert.frame.data.SwfDictionary
import com.maltaisn.swfconvert.convert.image.CompositeColorTransform
import com.maltaisn.swfconvert.convert.image.ImageDecoder
import com.maltaisn.swfconvert.convert.toAffineTransform
import com.maltaisn.swfconvert.convert.toColor
import com.maltaisn.swfconvert.convert.wrapper.toLineStyleWrapperOrNull
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

    private lateinit var swfDictionary: SwfDictionary
    private lateinit var colorTransform: CompositeColorTransform

    /**
     * Initialize this shape converter with resources.
     * @param swfDictionary Used to get image tags from SWF.
     * @param colorTransform Color transform applied on fill and line styles.
     */
    fun initialize(swfDictionary: SwfDictionary, colorTransform: CompositeColorTransform) {
        this.swfDictionary = swfDictionary
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

            val image = swfDictionary[fillStyle.identifier] as? ImageTag
                ?: conversionError(context, "Invalid image ID ${fillStyle.identifier}")

            val tr = fillStyle.transform.toAffineTransform()
            tr.scale(image.width.toDouble(), image.height.toDouble())
            tr.preConcatenate(config.bitmapMatrixTransform)

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

            val colors = fillStyle.gradients.map {
                val color = colorTransform.transform(it.color.toColor())
                val ratio = it.ratio.toFloat() / GRADIENT_MAX_RATIO
                GradientColor(color, ratio)
            }

            PathFillStyle.Gradient(colors, fillStyle.transform.toAffineTransform())
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
        val wstyle = lineStyle.toLineStyleWrapperOrNull(context)!!
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
        private const val GRADIENT_MAX_RATIO = 255
    }
}
