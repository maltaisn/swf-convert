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

import com.maltaisn.swfconvert.convert.context.ConvertContext
import com.maltaisn.swfconvert.convert.font.FontConverter
import com.maltaisn.swfconvert.convert.frame.SwfsConverter
import com.maltaisn.swfconvert.convert.image.ImageCreator
import com.maltaisn.swfconvert.core.FrameGroup
import com.maltaisn.swfconvert.core.ProgressCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.kotlin.logger
import javax.inject.Inject

/**
 * Converts a collection of SWF files to the intermediate representation.
 */
class SwfCollectionConverter @Inject internal constructor(
    private val config: ConvertConfiguration,
    private val progressCb: ProgressCallback,
    private val swfsDecoder: SwfsDecoder,
    private val swfsConverter: SwfsConverter,
    private val fontConverter: FontConverter,
    private val imageCreator: ImageCreator
) {

    private val logger = logger()

    suspend fun convert(context: ConvertContext): List<List<FrameGroup>> {
        config.imagesDir.deleteRecursively()
        config.fontsDir.deleteRecursively()
        config.imagesDir.mkdirs()
        config.fontsDir.mkdirs()

        // Decode SWFs files
        val swfs = swfsDecoder.decodeFiles(context, config.input)

        // Create font files
        progressCb.beginStep("Creating fonts")
        val fontGroups = fontConverter.createFontGroups(context, swfs)

        // Create font files and ungroup them.
        withContext(Dispatchers.IO) {
            fontConverter.createFontFiles(fontGroups)
        }
        val fontsMap = fontConverter.ungroupFonts(fontGroups)
        progressCb.endStep()

        // Convert SWF to intermediate representation.
        val frameGroups = swfsConverter.createFrameGroups(context, swfs, fontsMap)

        // Create images and remove duplicates if needed.
        imageCreator.createAndOptimizeImages(frameGroups.flatten())

        return frameGroups
    }

    fun cleanup() {
        // Remove temporary files if needed
        if (!config.keepImages) {
            logger.info { "Deleting temp image files at ${config.imagesDir}" }
            if (!config.imagesDir.deleteRecursively()) {
                logger.error { "Failed to delete temp image files" }
            }
        }
        if (!config.keepFonts) {
            logger.info { "Deleting temp font files at ${config.fontsDir}" }
            if (!config.fontsDir.deleteRecursively()) {
                logger.error { "Failed to delete temp font files" }
            }
        }
    }

}
