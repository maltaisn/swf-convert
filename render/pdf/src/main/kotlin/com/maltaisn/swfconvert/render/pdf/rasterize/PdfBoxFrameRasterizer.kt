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
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage
import java.io.File


/**
 * Frame rasterizer that uses PDFBox to rasterize frames.
 */
internal class PdfBoxFrameRasterizer(private val config: Configuration) : FrameRasterizer {

    private val pdfConfig: PdfConfiguration
        get() = config.format as PdfConfiguration


    override fun rasterizeFrame(frameGroup: FrameGroup, tempDir: File,
                                pdfImages: MutableMap<ImageData, PDImageXObject>,
                                pdfFonts: Map<File, PDFont>): BufferedImage {
        val pdfDoc = PDDocument()
        val pdfRenderer = PDFRenderer(pdfDoc)
        PdfFrameRenderer(config).renderFrame(pdfDoc, frameGroup, pdfImages, pdfFonts)
        val pdfImage = pdfRenderer.renderImageWithDPI(0, pdfConfig.rasterizationDpi, ImageType.RGB)
        pdfDoc.close()
        return pdfImage
    }

    companion object {
        const val NAME = "pdfbox"
    }

}
