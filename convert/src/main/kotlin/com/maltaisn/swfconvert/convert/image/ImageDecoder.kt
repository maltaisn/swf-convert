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

package com.maltaisn.swfconvert.convert.image

import com.flagstone.transform.image.DefineImage
import com.flagstone.transform.image.DefineImage2
import com.flagstone.transform.image.DefineJPEGImage2
import com.flagstone.transform.image.DefineJPEGImage3
import com.flagstone.transform.image.DefineJPEGImage4
import com.flagstone.transform.image.ImageTag
import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.convert.context.ConvertContext
import com.maltaisn.swfconvert.convert.conversionError
import com.maltaisn.swfconvert.convert.wrapper.WDefineImage
import com.maltaisn.swfconvert.convert.zlibDecompress
import com.maltaisn.swfconvert.core.Disposable
import com.maltaisn.swfconvert.core.YAxisDirection
import com.maltaisn.swfconvert.core.image.Color
import com.maltaisn.swfconvert.core.image.ImageData
import com.maltaisn.swfconvert.core.image.ImageDataCreator
import com.maltaisn.swfconvert.core.image.ImageFormat
import com.maltaisn.swfconvert.core.image.flippedVertically
import com.mortennobel.imagescaling.ResampleFilter
import com.mortennobel.imagescaling.ResampleOp
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Converts SWF image tags to binary image data, optionally applying a color transform.
 * Doesn't support [DefineJPEGImage4] for now. (deblocking filter)
 * See [https://www.adobe.com/content/dam/acom/en/devnet/pdf/swf-file-format-spec.pdf].
 */
internal class ImageDecoder @Inject constructor(
    private val config: ConvertConfiguration,
    private val imageDataCreator: ImageDataCreator
) : Disposable {

    override fun dispose() {
        imageDataCreator.dispose()
    }

    fun convertImage(
        context: ConvertContext,
        image: ImageTag,
        colorTransform: CompositeColorTransform,
        density: Float
    ) =
        when (image) {
            is DefineImage -> convertDefineImage(context, WDefineImage(image), colorTransform, density)
            is DefineImage2 -> convertDefineImage2(context, WDefineImage(image), colorTransform, density)
            is DefineJPEGImage2 -> convertJpegImage2(image, colorTransform, density)
            is DefineJPEGImage3 -> convertJpegImage3(image, colorTransform, density)
            else -> conversionError(context, "Unsupported image type ${image.javaClass.simpleName}")
        }

    // DEFINE BITS LOSSLESS DECODING

    /**
     * DefineBitsLossless is either:
     * - An indexed bitmap image with 256 24-bit colors table. (RGB)
     * - RGB555 (16 bits) encoded bitmap.
     * - RGB888 (24 bits) encoded bitmap.
     */
    private fun convertDefineImage(
        context: ConvertContext,
        image: WDefineImage,
        colorTransform: CompositeColorTransform,
        density: Float
    ): ImageData {
        // Create buffered image. RGB channels only, no alpha.
        val buffImage = when (image.bits) {
            INDEXED_RGB_BITS -> convertIndexedImage(image, BufferedImage.TYPE_INT_RGB,
                INDEXED_RGB_BYTES, ByteArray::rgbBytesToColor)
            RAW_PIX15_BITS -> convertRawImage(image, BufferedImage.TYPE_INT_RGB,
                RAW_PIX15_BYTES, ByteArray::pix15BytesToColor)
            RAW_PIX24_BITS -> convertRawImage(image, BufferedImage.TYPE_INT_RGB,
                RAW_PIX24_BYTES, ByteArray::pix24BytesToColor)
            else -> conversionError(context, "Invalid number of image bits ${image.bits}")
        }
        return createTransformedImageData(buffImage, colorTransform, density, ImageFormat.PNG)
    }

    /**
     * DefineBitsLossless2 is either:
     * - An indexed bitmap image with 256 32-bit colors table. (RGBA)
     * - ARGB8888 (32 bits) encoded bitmap.
     */
    private fun convertDefineImage2(
        context: ConvertContext,
        image: WDefineImage,
        colorTransform: CompositeColorTransform,
        density: Float
    ): ImageData {
        // Create buffered image. RGB channels + alpha channel.
        val buffImage = when (image.bits) {
            INDEXED_RGBA_BITS -> convertIndexedImage(image, BufferedImage.TYPE_INT_ARGB,
                INDEXED_RGBA_BYTES, ByteArray::rgbaBytesToColor)
            RAW_ARGB_BITS -> convertRawImage(image, BufferedImage.TYPE_INT_ARGB,
                RAW_ARGB_BYTES, ByteArray::argbBytesToColor)
            else -> conversionError(context, "Invalid number of image bits ${image.bits}")
        }
        return createTransformedImageData(buffImage, colorTransform, density, ImageFormat.PNG)
    }

    /**
     * Converts a DefineBitsLossless image tag that uses a color table
     * to a [BufferedImage]. Colors in table occupy a certain number of [bytes]
     * and are decoded to ARGB values using [bitsConverter].
     */
    private fun convertIndexedImage(
        image: WDefineImage,
        type: Int,
        bytes: Int,
        bitsConverter: ByteArray.(Int) -> Color
    ): BufferedImage {
        // Image data is color table then pixel data as indices in color table.
        // Color table colors is either RGB or RGBA.
        val colors = IntArray(image.tableSize) {
            image.data.bitsConverter(it * bytes).value
        }
        val buffImage = BufferedImage(image.width, image.height, type)
        var pos = image.tableSize * bytes // Current pos in image data including color table
        var i = 0 // Current pos in image data, excluding color table
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                buffImage.setRGB(x, y, colors[image.data[pos].toUByte().toInt()])
                pos++
                i++
            }
            while (i % IMAGE_DATA_PAD_BYTES != 0) {
                // Account for padding. Note that padding is relative to the start of the
                // color table, not the start of the image data.
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
    private fun convertRawImage(
        image: WDefineImage,
        type: Int,
        bytes: Int,
        bitsConverter: (ByteArray, Int) -> Color
    ): BufferedImage {
        // Image data only. Data can be PIX15, PIX24 or ARGB.
        val buffImage = BufferedImage(image.width, image.height, type)
        var i = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                buffImage.setRGB(x, y, bitsConverter(image.data, i).value)
                i += bytes
            }
            while (i % IMAGE_DATA_PAD_BYTES != 0) {
                i++ // Account for padding by skipping data bytes
            }
        }
        return buffImage
    }

    // DEFINE JPEG DECODING

    /**
     * DefineBitsJPEG2 tag is just plain JPEG data, without alpha channel.
     */
    private fun convertJpegImage2(
        image: DefineJPEGImage2,
        colorTransform: CompositeColorTransform,
        density: Float
    ): ImageData {
        val buffImage = ImageIO.read(ByteArrayInputStream(image.image))
        return createTransformedImageData(buffImage, colorTransform, density, ImageFormat.JPG)
    }

    /**
     * DefineBitsJPEG3 tag is JPEG data with a ZLIB compressed alpha channel.
     */
    private fun convertJpegImage3(
        image: DefineJPEGImage3,
        colorTransform: CompositeColorTransform,
        density: Float
    ): ImageData {
        // JPEG/PNG/GIF image where alpha channel is stored separatedly.
        val w = image.width
        val h = image.height
        val buffImage = ImageIO.read(ByteArrayInputStream(image.image))

        // Read alpha channel which is compressed with ZLIB.
        // Each byte is the alpha value for a pixel, the array being (width * height) long.
        val alphaBytes = image.alpha.zlibDecompress()
        if (alphaBytes.isEmpty()) {
            // For PNG and GIF data, no alpha can be specified, use image as-is.
            return createTransformedImageData(buffImage, colorTransform, density, ImageFormat.JPG)
        }
        assert(alphaBytes.size == w * h)

        // Create new image and copy each pixel, setting alpha value
        // Also premultiply alpha values.
        val newImage = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        var i = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                var color = Color(buffImage.getRGB(x, y)) // Get pixel color without alpha
                color = color.withAlpha(alphaBytes[i].toUByte().toInt()) // Set alpha value on color
                color = color.divideAlpha()
                newImage.setRGB(x, y, color.value)
                i++
            }
        }

        return createTransformedImageData(newImage, colorTransform, density, ImageFormat.JPG)
    }

    // BUFFERED IMAGE UTILS

    /**
     * Create image data for an [buffImage]. Created data uses [ConvertConfiguration.imageFormat],
     * or [defaultFormat] if no format is forced. Also applies color transform, vertical flip,
     * and downsampling to [buffImage].
     *
     * @param density Density of [buffImage] in DPI. Use `null` to disable downsampling.
     * @param colorTransform Color transform to apply on [buffImage]. Use `null` for none.
     */
    private fun createTransformedImageData(
        buffImage: BufferedImage,
        colorTransform: CompositeColorTransform?,
        density: Float?,
        defaultFormat: ImageFormat
    ): ImageData {
        // Transform image
        var image = buffImage
        colorTransform?.transform(image)
        if (density != null) {
            image = image.downsampled(density, config.maxDpi)
        }
        if (config.yAxisDirection == YAxisDirection.UP) {
            image = image.flippedVertically()
        }

        // Create image data
        val format = config.imageFormat ?: defaultFormat
        return imageDataCreator.createImageData(image, format, config.jpegQuality)
    }

    /**
     * If [ConvertConfiguration.downsampleImages] is `true`, this will downsample [this] image
     * if its [currentDensity] is over [maxDensity]. New density will be [maxDensity].
     * If density is already below or downsampling is disabled, the same image is returned.
     */
    private fun BufferedImage.downsampled(
        currentDensity: Float,
        maxDensity: Float
    ): BufferedImage {
        val min = config.downsampleMinSize.toFloat()
        val isTooSmall = this.width < min || this.height < min
        if (!config.downsampleImages || currentDensity < maxDensity || isTooSmall) {
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

        return this.downsampleWithFilter(config.downsampleFilter, w.roundToInt(), h.roundToInt())
    }

    /**
     * Downsample [this] image with a [filter] to a [width] and [height].
     * If [filter] is `null`, use native Java API to resize.
     */
    private fun BufferedImage.downsampleWithFilter(filter: ResampleFilter?, width: Int, height: Int): BufferedImage {
        val result = BufferedImage(width, height, this.type)
        if (filter == null) {
            // Resize with Graphics2D
            val graphics = result.createGraphics()
            graphics.drawImage(this, 0, 0, width, height, null)
            graphics.dispose()

        } else {
            // Resize with third party lib
            val resizeOp = ResampleOp(width, height)
            resizeOp.filter = filter
            resizeOp.filter(this, result)
        }
        return result
    }

    companion object {
        // Constants defined in DefineImage and DefineImage2 for `image.tableSize`.
        private const val INDEXED_RGB_BITS = 8
        private const val INDEXED_RGBA_BITS = 8

        // Constants defined in DefineImage and DefineImage2 for `image.pixelSize`.
        private const val RAW_PIX15_BITS = 16
        private const val RAW_PIX24_BITS = 24
        private const val RAW_ARGB_BITS = 32

        // Number of bytes occupied by colors in indexed image formats
        private const val INDEXED_RGB_BYTES = 3
        private const val INDEXED_RGBA_BYTES = 4

        // Number of bytes occupied by colors in raw image formats
        private const val RAW_PIX15_BYTES = 2
        private const val RAW_PIX24_BYTES = 4
        private const val RAW_ARGB_BYTES = 4

        // Image data records pad each image line data to 32-bit relative to start of data.
        // See the note on the COLORMAPDATA and ALPHACOLORMAPDATA records in SWF reference.
        private const val IMAGE_DATA_PAD_BYTES = 4
    }

}
