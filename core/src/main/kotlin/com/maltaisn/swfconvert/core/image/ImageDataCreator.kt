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

import com.maltaisn.swfconvert.core.Disposable
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.inject.Inject


/**
 * Utility class for creating [ImageData] from [BufferedImage].
 */
class ImageDataCreator @Inject constructor() : Disposable {

    private val jpgWriter = ImageIO.getImageWritersByFormatName(ImageFormat.JPG.extension).next()
    private val jpgWriteParam = jpgWriter.defaultWriteParam.apply {
        compressionMode = ImageWriteParam.MODE_EXPLICIT
    }

    /**
     * Create image data for an [image] in the specified image [format].
     * Image will be flipped compared to original.
     * @param jpegQuality Quality to use for JPG format, between 0 (worst) and 1 (best).
     */
    fun createImageData(image: BufferedImage, format: ImageFormat, jpegQuality: Float): ImageData {
        if (format == ImageFormat.JPG) {
            jpgWriteParam.compressionQuality = jpegQuality
        }

        // Flip image. TODO is this pdf specific?
        val flipped = image.flippedVertically()

        val data: ByteArray
        val alphaData: ByteArray
        if (format == ImageFormat.PNG || !flipped.colorModel.hasAlpha()) {
            // No alpha channel, alpha data is never used, or PNG, which supports alpha channel
            data = flipped.toByteArray(format)
            alphaData = ByteArray(0)

        } else {
            // JPG doesn't support alpha channel, so RGB and alpha must be encoded separatedly.
            data = flipped.removeAlphaChannel().toByteArray(format)
            alphaData = flipped.isolateAlphaChannel().toByteArray(format)
        }

        return ImageData(data, alphaData, format, image.width, image.height)
    }

    /**
     * Return a vertically flipped copy of [this] image.
     */
    private fun BufferedImage.flippedVertically(): BufferedImage {
        val transform = AffineTransform.getScaleInstance(1.0, -1.0)
        transform.translate(0.0, -this.height.toDouble())
        val transformOp = AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
        return transformOp.filter(this, null)
    }

    /**
     * Returns a copy of [this] image, but with alpha channel removed.
     */
    private fun BufferedImage.removeAlphaChannel(): BufferedImage {
        assert(this.type == BufferedImage.TYPE_INT_ARGB)
        val w = this.width
        val h = this.height
        val rgbImage = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val rgbArray = this.getRGB(0, 0, w, h, null, 0, w)
        for ((i, argb) in rgbArray.withIndex()) {
            rgbArray[i] = argb or 0xFF000000.toInt()
        }
        rgbImage.setRGB(0, 0, w, h, rgbArray, 0, w)
        return rgbImage
    }

    /**
     * Returns a copy of the alpha channel of [this] image, as a single channel image.
     */
    private fun BufferedImage.isolateAlphaChannel(): BufferedImage {
        assert(this.type == BufferedImage.TYPE_INT_ARGB)
        val w = this.width
        val h = this.height
        val alphaImage = BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY)
        val alphaArray = this.getRGB(0, 0, w, h, null, 0, w)
        for ((i, argb) in alphaArray.withIndex()) {
            val alpha = argb ushr 24
            alphaArray[i] = Color.gray(alpha).value
        }
        alphaImage.raster.setPixels(0, 0, w, h, alphaArray)
        return alphaImage
    }

    /**
     * Convert [this] image to its byte array representation in a [format].
     */
    private fun BufferedImage.toByteArray(format: ImageFormat): ByteArray {
        val output = ByteArrayOutputStream()
        val imageOutput = ImageIO.createImageOutputStream(output)

        when (format) {
            ImageFormat.JPG -> {
                jpgWriter.output = imageOutput
                val iioImage = IIOImage(this, null, null)
                jpgWriter.write(null, iioImage, jpgWriteParam)
            }
            else -> {
                ImageIO.write(this, format.extension, imageOutput)
            }
        }

        val arr = output.toByteArray()
        output.close()
        imageOutput.close()

        return arr
    }

    override fun dispose() {
        jpgWriter.dispose()
    }

}
