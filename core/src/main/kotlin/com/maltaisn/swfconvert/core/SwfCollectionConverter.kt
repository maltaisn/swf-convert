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

package com.maltaisn.swfconvert.core

import com.maltaisn.swfconvert.core.config.Configuration
import com.maltaisn.swfconvert.core.config.Debug
import com.maltaisn.swfconvert.core.font.FontConverter
import com.maltaisn.swfconvert.core.frame.FramesRenderer
import com.maltaisn.swfconvert.core.frame.SwfsConverter
import com.maltaisn.swfconvert.core.image.ImageCreator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File


/**
 * Converts a collection of SWF files to the output format.
 */
class SwfCollectionConverter {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)


    fun convertSwfCollection(config: Configuration) {
        val fontsDir = File(config.main.tempDir, "fonts")
        val imagesDir = File(config.main.tempDir, "images")

        // Decode SWFs files
        val swfs = SwfsDecoder(coroutineScope).decodeFiles(config.main.input)

        // Create font groups
        val fontConverter = FontConverter(fontsDir, config.main)
        val fontGroups = fontConverter.createFontGroups(swfs)

        // Create font files and ungroup them.
        fontConverter.createFontFiles(fontGroups)
        val fontsMap = fontConverter.ungroupFonts(fontGroups)

        // Convert SWF to intermediate representation.
        val swfsConverter = SwfsConverter(coroutineScope, fontsMap)
        val frameGroups = swfsConverter.createFrameGroups(swfs, config.main)

        // Create images and remove duplicates if needed.
        imagesDir.deleteRecursively()
        val imageCreator = ImageCreator(coroutineScope, config.main)
        imageCreator.createAndOptimizeImages(frameGroups, imagesDir)

        // Render frames (from intermediate format to output format)
        FramesRenderer(coroutineScope, config).renderFrames(frameGroups)

        // Remove files not to keep
        if (!Debug.keepImages) {
            imagesDir.deleteRecursively()
        }
        if (!Debug.keepFonts) {
            fontsDir.deleteRecursively()
        }
    }

}

