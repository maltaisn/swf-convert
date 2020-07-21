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
