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
     * @param jpegQuality Quality to use for JPG format, between 0 (worst) and 1 (best).
     */
    fun createImageData(image: BufferedImage, format: ImageFormat, jpegQuality: Float): ImageData {
        if (format == ImageFormat.JPG) {
            jpgWriteParam.compressionQuality = jpegQuality
        }

        val data: ByteArray
        val alphaData: ByteArray
        if (format == ImageFormat.PNG || !image.colorModel.hasAlpha()) {
            // No alpha channel, alpha data is never used, or PNG, which supports alpha channel
            data = image.toByteArray(format)
            alphaData = ByteArray(0)

        } else {
            // JPG doesn't support alpha channel, so RGB and alpha must be encoded separatedly.
            data = image.removeAlphaChannel().toByteArray(format)
            alphaData = image.isolateAlphaChannel().toByteArray(format)
        }

        return ImageData(data, alphaData, format, image.width, image.height)
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
