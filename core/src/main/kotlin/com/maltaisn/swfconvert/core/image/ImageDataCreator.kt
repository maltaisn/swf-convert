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
