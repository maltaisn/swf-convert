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

package com.maltaisn.swfconvert.render.pdf.rasterize

import com.maltaisn.swfconvert.core.config.Configuration
import com.maltaisn.swfconvert.core.frame.data.FrameGroup
import com.maltaisn.swfconvert.core.image.data.ImageData
import com.maltaisn.swfconvert.render.pdf.PdfConfiguration
import com.maltaisn.swfconvert.render.pdf.PdfFrameRenderer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import javax.imageio.ImageIO
import kotlin.math.roundToInt


class ExternalFrameRasterizer(private val config: Configuration) : FrameRasterizer {

    private val pdfConfig: PdfConfiguration
        get() = config.format as PdfConfiguration


    override fun rasterizeFrame(frameGroup: FrameGroup, tempDir: File,
                                pdfImages: MutableMap<ImageData, PDImageXObject>,
                                pdfFonts: Map<File, PDFont>): BufferedImage? {
        val pdfDoc = PDDocument()
        PdfFrameRenderer(config).renderFrame(pdfDoc, frameGroup, pdfImages, pdfFonts)

        val logFile = File(tempDir, "frame.log")
        return try {
            // Export PDF to rasterize
            val input = File(tempDir, "frame.pdf")
            val output = File(tempDir, "frame.png")
            pdfDoc.save(input)

            // Use external rasterizer
            val command = pdfConfig.rasterizerArgs.format(
                    "\"${input.absolutePath}\"",
                    pdfConfig.rasterizationDpi.roundToInt().toString(),
                    "\"${output.absolutePath}\"")
            val pb = ProcessBuilder(pdfConfig.rasterizer, command)
            pb.redirectErrorStream()
            pb.redirectOutput()
            pb.start().waitFor()

            // Adjust image format to RGB if needed
            var pdfImage = ImageIO.read(output)
            if (pdfImage.type != BufferedImage.TYPE_INT_RGB) {
                val pdfImageRgb = BufferedImage(pdfImage.width, pdfImage.height, BufferedImage.TYPE_INT_RGB)
                val gr = pdfImageRgb.graphics
                gr.drawImage(pdfImage, 0, 0, null)
                gr.dispose()
                pdfImage = pdfImageRgb
            }

            pdfImage

        } catch (e: IOException) {
            // External rasterizer failed, log error.
            FileWriter(logFile).use {
                e.printStackTrace(PrintWriter(it))
            }
            println("ERROR: Failed to rasterize page with external rasterizer.")
            return null
        }
    }

}
