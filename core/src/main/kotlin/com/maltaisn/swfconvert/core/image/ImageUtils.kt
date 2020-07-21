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

package com.maltaisn.swfconvert.core.image

import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage

/**
 * Return a vertically flipped copy of [this] image.
 */
fun BufferedImage.flippedVertically(): BufferedImage {
    val transform = AffineTransform.getScaleInstance(1.0, -1.0)
    transform.translate(0.0, -this.height.toDouble())
    val transformOp = AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
    return transformOp.filter(this, null)
}

/**
 * Returns a copy of [this] image, but with alpha channel removed.
 */
internal fun BufferedImage.removeAlphaChannel(): BufferedImage {
    assert(this.type == BufferedImage.TYPE_INT_ARGB)
    val w = this.width
    val h = this.height
    val rgbImage = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val rgbArray = this.getRGB(0, 0, w, h, null, 0, w)
    for ((i, argb) in rgbArray.withIndex()) {
        rgbArray[i] = Color(argb).opaque.value
    }
    rgbImage.setRGB(0, 0, w, h, rgbArray, 0, w)
    return rgbImage
}

/**
 * Returns a copy of the alpha channel of [this] image, as a single channel image.
 */
internal fun BufferedImage.isolateAlphaChannel(): BufferedImage {
    assert(this.type == BufferedImage.TYPE_INT_ARGB)
    val w = this.width
    val h = this.height
    val alphaImage = BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY)
    val alphaArray = this.getRGB(0, 0, w, h, null, 0, w)
    for ((i, argb) in alphaArray.withIndex()) {
        val alpha = Color(argb).a
        alphaArray[i] = Color.gray(alpha).value
    }
    alphaImage.raster.setPixels(0, 0, w, h, alphaArray)
    return alphaImage
}
