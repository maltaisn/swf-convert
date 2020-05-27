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

package com.maltaisn.swfconvert.convert.font

import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.core.shape.PathElement.*
import com.maltaisn.swfconvert.core.text.GlyphData
import net.sourceforge.tess4j.ITessAPI
import net.sourceforge.tess4j.Tesseract
import java.awt.Color
import java.awt.geom.GeneralPath
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.inject.Inject


internal class GlyphOcr @Inject constructor(
        private val config: ConvertConfiguration,
        private val tesseract: Tesseract
) {

    init {
        tesseract.setTessVariable("user_defined_dpi", "300")
        tesseract.setTessVariable("debug_file", "/dev/null")
        tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_WORD)
    }

    fun recognizeGlyphData(data: GlyphData, tempDir: File): Char? {
        val image = renderGlyphData(data)

        // Recognize glyph from image with OCR
        val str = tesseract.doOCR(image).trim().toLowerCase()
        val char = when {
            str in OCR_UNICODE -> OCR_UNICODE[str]
            str.length == 1 -> str.first()
            else -> null
        }

        if (config.outputOcrGlyphs) {
            val sb = StringBuilder()
            sb.append(data.hashCode().toUInt())
            if (char != null) {
                sb.append('-')
                sb.append(char.toInt().toString(16))
                if (char !in ILLEGAL_FILE_CHARS) {
                    sb.append('-')
                    sb.append(char)
                }
            }
            sb.append(".jpg")

            tempDir.mkdirs()
            ImageIO.write(image, "png", File(tempDir, sb.toString()))
        }

        return char
    }

    private fun renderGlyphData(data: GlyphData): BufferedImage {
        val size = RENDERED_IMAGE_SIZE
        val pad = RENDERED_IMAGE_PADDING
        val image = BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY)

        val graphics = image.createGraphics()
        graphics.background = Color.WHITE
        graphics.color = Color.BLACK

        val path = convertGlyphDataToPath(data)

        // Draw the glyph, scaled to the image size, padded, and with Y axis inverted.
        val scale = (size - 2 * pad) / GlyphData.EM_SQUARE_SIZE.toDouble()
        graphics.clearRect(0, 0, size, size)
        graphics.translate(pad, size - pad)
        graphics.scale(scale, -scale)
        graphics.fill(path)

        return image
    }

    private fun convertGlyphDataToPath(glyphData: GlyphData): GeneralPath {
        val path = GeneralPath()
        for (contour in glyphData.contours) {
            for (e in contour.elements) {
                when (e) {
                    is MoveTo -> path.moveTo(e.x, e.y)
                    is LineTo -> path.lineTo(e.x, e.y)
                    is QuadTo -> path.quadTo(e.cx, e.cy, e.x, e.y)
                    is ClosePath -> path.closePath()
                    else -> error("Unexpected glyph path command")
                }
            }
        }
        return path
    }

    companion object {
        private const val RENDERED_IMAGE_SIZE = 256
        private const val RENDERED_IMAGE_PADDING = 32

        // Ligatures to unicode from U+FB00
        // Roman numerals to unicode from U+2160
        private val OCR_UNICODE = mapOf("ff" to 'ﬀ', "fi" to 'ﬁ', "fl" to 'ﬂ',
                "i" to 'Ⅰ', "v" to 'Ⅴ', "x" to 'Ⅹ')

        private const val ILLEGAL_FILE_CHARS = "/\n\r\t`?*\\<>|\":"

    }

}
