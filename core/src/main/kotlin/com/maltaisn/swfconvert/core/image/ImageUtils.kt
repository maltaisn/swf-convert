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
