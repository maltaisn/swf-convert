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

package com.maltaisn.swfconvert.convert

import com.maltaisn.swfconvert.convert.font.FontConverter
import com.maltaisn.swfconvert.convert.frame.SwfsConverter
import com.maltaisn.swfconvert.convert.image.ImageCreator
import com.maltaisn.swfconvert.core.FrameGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


/**
 * Converts a collection of SWF files to the intermediate representation.
 */
class SwfCollectionConverter @Inject internal constructor(
        private val config: ConvertConfiguration,
        private val swfsDecoder: SwfsDecoder,
        private val swfsConverter: SwfsConverter,
        private val fontConverter: FontConverter,
        private val imageCreator: ImageCreator
) {

    suspend fun convert(): List<FrameGroup> {
        config.imagesDir.deleteRecursively()
        config.fontsDir.deleteRecursively()
        config.imagesDir.mkdirs()
        config.fontsDir.mkdirs()

        // Decode SWFs files
        val swfs = swfsDecoder.decodeFiles(config.input)

        // Create font files
        val fontGroups = fontConverter.createFontGroups(swfs)

        // Create font files and ungroup them.
        withContext(Dispatchers.IO) {
            fontConverter.createFontFiles(fontGroups)
        }
        val fontsMap = fontConverter.ungroupFonts(fontGroups)

        // Convert SWF to intermediate representation.
        val frameGroups = swfsConverter.createFrameGroups(swfs, fontsMap)

        // Create images and remove duplicates if needed.
        imageCreator.createAndOptimizeImages(frameGroups)

        return frameGroups
    }

    fun cleanup() {
        // Remove temporary files if needed
        if (!config.keepImages) {
            config.imagesDir.deleteRecursively()
        }
        if (!config.keepFonts) {
            config.fontsDir.deleteRecursively()
        }
    }

}

