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

import com.maltaisn.swfconvert.core.frame.data.FrameGroup
import com.maltaisn.swfconvert.core.image.data.ImageData
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.image.BufferedImage
import java.io.File


/**
 * Interface for a class used to rasterize [FrameGroup] to an image.
 * Implementation must be thread-safe.
 */
internal interface FrameRasterizer {

    /**
     * Convert a [frameGroup] to a [BufferedImage]. Returns `null` if rasterization fails.
     * @param tempDir Can be used as the temp directory during rasterization process.
     */
    fun rasterizeFrame(frameGroup: FrameGroup, tempDir: File,
                       pdfImages: MutableMap<ImageData, PDImageXObject>,
                       pdfFonts: Map<File, PDFont>): BufferedImage?


    companion object {

        private val rasterizers = mutableMapOf<String?, FrameRasterizer>()

        fun register(name: String?, rasterizer: FrameRasterizer) {
            rasterizers[name] = rasterizer
        }

        fun getByName(name: String?) = rasterizers[name]
    }

}
