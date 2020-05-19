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

import com.flagstone.transform.image.*
import com.maltaisn.swfconvert.core.config.Configuration
import com.maltaisn.swfconvert.core.conversionError
import com.maltaisn.swfconvert.core.image.data.Color
import com.maltaisn.swfconvert.core.image.data.ImageData
import com.maltaisn.swfconvert.core.image.data.WDefineImage
import com.maltaisn.swfconvert.core.zlibDecompress
import com.mortennobel.imagescaling.ResampleOp
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.roundToInt


/**
 * Converts SWF image tags to binary image data, optionally applying a color transform.
 * Doesn't support [DefineJPEGImage4] for now. (deblocking filter)
 * See [https://www.adobe.com/content/dam/acom/en/devnet/pdf/swf-file-format-spec.pdf].
 */
class ImageDecoder(private val config: Configuration) {

    private val jpgWriter = ImageIO.getImageWritersByFormatName(ImageFormat.JPG.extension).next()
    private val jpgWriteParam = jpgWriter.defaultWriteParam.apply {
        compressionMode = ImageWriteParam.MODE_EXPLICIT
        compressionQuality = config.main.jpegQuality
    }

    fun dispose() {
        jpgWriter.dispose()
    }

    fun convertImage(image: ImageTag, colorTransform: CompositeColorTransform,
                     density: Float) = when (image) {
        is DefineImage -> convertDefineImage(WDefineImage(image), colorTransform, density)
        is DefineImage2 -> convertDefineImage2(WDefineImage(image), colorTransform, density)
        is DefineJPEGImage2 -> convertJpegImage2(image, colorTransform, density)
        is DefineJPEGImage3 -> convertJpegImage3(image, colorTransform, density)
        else -> {
            conversionError("Unsupported image type")
        }
    }

    // DEFINE BITS LOSSLESS DECODING

    /**
     * DefineBitsLossless is either:
     * - An indexed bitmap image with 256 24-bit colors table. (RGB)
     * - RGB555 (16 bits) encoded bitmap.
     * - RGB888 (24 bits) encoded bitmap.
     */
    private fun convertDefineImage(image: WDefineImage, colorTransform: CompositeColorTransform,
                                   density: Float): ImageData {
        // Create buffered image. RGB channels only, no alpha.
        val buffImage = when (image.bits) {
            8 -> convertIndexedImage(image, BufferedImage.TYPE_INT_RGB, 3, (Color)::fromRgbBytes)
            16 -> convertRawImage(image, BufferedImage.TYPE_INT_RGB, 2, (Color)::fromPix15Bytes)
            24 -> convertRawImage(image, BufferedImage.TYPE_INT_RGB, 4, (Color)::fromPix24Bytes)
            else -> conversionError("Invalid number of image bits")
        }
        return createImageData(buffImage, colorTransform, density, ImageFormat.PNG)
    }

    /**
     * DefineBitsLossless2 is either:
     * - An indexed bitmap image with 256 32-bit colors table. (RGBA)
     * - ARGB8888 (32 bits) encoded bitmap.
     */
    private fun convertDefineImage2(image: WDefineImage, colorTransform: CompositeColorTransform,
                                    density: Float): ImageData {
        // Create buffered image. RGB channels + alpha channel.
        val buffImage = when (image.bits) {
            8 -> convertIndexedImage(image, BufferedImage.TYPE_INT_ARGB, 4, (Color)::fromRgbaBytes)
            32 -> convertRawImage(image, BufferedImage.TYPE_INT_ARGB, 4, (Color)::fromArgbBytes)
            else -> conversionError("Invalid number of image bits")
        }
        return createImageData(buffImage, colorTransform, density, ImageFormat.PNG)
    }

    /**
     * Converts a DefineBitsLossless image tag that uses a color table
     * to a [BufferedImage]. Colors in table occupy a certain number of [bytes]
     * and are decoded to ARGB values using [bitsConverter].
     */
    private fun convertIndexedImage(image: WDefineImage, type: Int, bytes: Int,
                                    bitsConverter: (ByteArray, Int) -> Color): BufferedImage {
        // Image data is color table then pixel data as indices in color table.
        // Color table colors is either RGB or RGBA.
        val colors = IntArray(image.tableSize) {
            bitsConverter(image.data, it * bytes).value
        }
        val buffImage = BufferedImage(image.width, image.height, type)
        var pos = image.tableSize * bytes
        var i = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                buffImage.setRGB(x, y, colors[image.data[pos].toInt() and 0xFF])
                pos++
                i++
            }
            while (i % 4 != 0) {
                // Pad to 32-bits (relative to the start of the pixel data!)
                pos++
                i++
            }
        }

        return buffImage
    }

    /**
     * Converts a DefineBitsLossless image tag encoding a bitmap to
     * a [BufferedImage]. Colors in the bitmap occupy a certain number of [bytes],
     * and are decoded to ARGB values using [bitsConverter]
     */
    private fun convertRawImage(image: WDefineImage, type: Int, bytes: Int,
                                bitsConverter: (ByteArray, Int) -> Color): BufferedImage {
        // Image data only. Data can be PIX15, PIX24 or ARGB.
        val buffImage = BufferedImage(image.width, image.height, type)
        var i = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                buffImage.setRGB(x, y, bitsConverter(image.data, i).value)
                i += bytes
            }
            while (i % 4 != 0) {
                i++  // Pad to 32-bits
            }
        }
        return buffImage
    }


    // DEFINE JPEG DECODING

    /**
     * DefineBitsJPEG2 tag is just plain JPEG data, without alpha channel.
     */
    private fun convertJpegImage2(image: DefineJPEGImage2, colorTransform: CompositeColorTransform,
                                  density: Float): ImageData {
        val buffImage = ImageIO.read(ByteArrayInputStream(image.image))
        return createImageData(buffImage, colorTransform, density, ImageFormat.JPG)
    }

    /**
     * DefineBitsJPEG3 tag is JPEG data with a ZLIB compressed alpha channel.
     */
    private fun convertJpegImage3(image: DefineJPEGImage3, colorTransform: CompositeColorTransform,
                                  density: Float): ImageData {
        // JPEG/PNG/GIF image where alpha channel is stored separatedly.
        val w = image.width
        val h = image.height
        val buffImage = ImageIO.read(ByteArrayInputStream(image.image))

        // Read alpha channel which is compressed with ZLIB.
        // Each byte is the alpha value for a pixel, the array being (width * height) long.
        val alphaBytes = image.alpha.zlibDecompress()
        if (alphaBytes.isEmpty()) {
            // For PNG and GIF data, no alpha can be specified, use image as-is.
            return createImageData(buffImage, colorTransform, density, ImageFormat.JPG)
        }
        assert(alphaBytes.size == w * h)

        // Create new image and copy each pixel, setting alpha value
        // Also premultiply alpha values.
        val newImage = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        var i = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                var color = Color(buffImage.getRGB(x, y))  // Get pixel color without alpha
                color = color.withAlpha(alphaBytes[i].toInt() and 0xFF)  // Set alpha value on color
                color = color.divideAlpha()
                newImage.setRGB(x, y, color.value)
                i++
            }
        }

        return createImageData(newImage, colorTransform, density, ImageFormat.JPG)
    }

    // BUFFERED IMAGE UTILS

    /**
     * Create image data for an [buffImage]. Created data uses [Config.FORCE_IMAGE_FORMAT],
     * or [defaultFormat] if no format is forced. Also applies color transform, vertical flip,
     * and downsampling to [buffImage].
     *
     * @param density Density of [buffImage] in DPI. Use `null` to disable downsampling.
     * @param colorTransform Color transform to apply on [buffImage]. Use `null` for none.
     */
    fun createImageData(buffImage: BufferedImage,
                        colorTransform: CompositeColorTransform?,
                        density: Float?,
                        defaultFormat: ImageFormat): ImageData {
        // Transform image
        var image = buffImage
        image = image.flippedVertically()
        colorTransform?.transform(image)
        if (density != null) {
            image = image.downsampled(density, config.main.maxDpi)
        }

        // Create image data
        val format = config.main.imageFormat ?: defaultFormat
        val data: ByteArray
        val alphaData: ByteArray
        when (image.type) {
            BufferedImage.TYPE_INT_RGB, BufferedImage.TYPE_3BYTE_BGR -> {
                // No alpha channel, alpha data is never used.
                data = image.toByteArray(format)
                alphaData = ByteArray(0)
            }
            BufferedImage.TYPE_INT_ARGB -> {
                if (format == ImageFormat.JPG) {
                    // JPG doesn't support alpha channel, so RGB and alpha must be encoded separatedly.
                    if (image.type == BufferedImage.TYPE_INT_ARGB) {
                        data = image.removeAlphaChannel().toByteArray(format)
                        alphaData = image.isolateAlphaChannel().toByteArray(format)
                    } else {
                        data = image.toByteArray(format)
                        alphaData = ByteArray(0)
                    }

                } else {
                    // PNG supports alpha channel, alpha data is not used.
                    data = image.toByteArray(format)
                    alphaData = ByteArray(0)
                }
            }
            else -> error("Unsupported image type")
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
     * If [Config.DOWNSAMPLE_IMAGES] is `true`, this will downsample [this] image
     * if its [currentDensity] is over [maxDensity]. New density will be [maxDensity].
     * If density is already below or downsampling is disabled, the same image is returned.
     */
    private fun BufferedImage.downsampled(currentDensity: Float,
                                maxDensity: Float): BufferedImage {
        val min = config.main.downsampleMinSize.toFloat()
        if (!config.main.downsampleImages || currentDensity < maxDensity ||
                this.width < min || this.height < min) {
            // Downsampling disabled, or density is below maximum, or size is already very small.
            return this
        }

        val scale = maxDensity / currentDensity
        var w = this.width * scale
        var h = this.height * scale

        // Make sure we're not downsampling to below minimum size.
        if (w < min) {
            h *= min / w
            w = min
        }
        if (h < min) {
            w *= min / h
            h = min
        }

        val iw = w.roundToInt()
        val ih = h.roundToInt()
        val resizeOp = ResampleOp(iw, ih)
        resizeOp.filter = config.main.downsampleFilter!!
        // TODO implement "fast" filter
        val destImage = BufferedImage(iw, ih, this.type)
        return resizeOp.filter(this, destImage)
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
     * Convert a [BufferedImage] its byte array representation in a [format].
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
        check(arr.isNotEmpty()) {
            "Could not write image to byte array."
        }

        return arr
    }

}
