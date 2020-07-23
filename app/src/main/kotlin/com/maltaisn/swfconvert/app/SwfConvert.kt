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

package com.maltaisn.swfconvert.app

import com.maltaisn.swfconvert.app.di.DaggerAppComponent
import com.maltaisn.swfconvert.convert.SwfCollectionConverter
import com.maltaisn.swfconvert.convert.di.DaggerConvertComponent
import com.maltaisn.swfconvert.render.core.FramesRenderer
import com.maltaisn.swfconvert.render.core.RenderConfiguration
import com.maltaisn.swfconvert.render.core.di.DaggerRenderCoreComponent
import com.maltaisn.swfconvert.render.ir.di.DaggerRenderIrComponent
import com.maltaisn.swfconvert.render.pdf.di.DaggerRenderPdfComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.kotlin.logger
import javax.inject.Inject
import javax.inject.Provider

internal class SwfConvert(private val config: Configuration) {

    private val logger = logger()

    @Inject
    lateinit var converterProvider: Provider<SwfCollectionConverter>
    @Inject
    lateinit var framesRenderers: Map<Class<out RenderConfiguration>,
            @JvmSuppressWildcards Provider<FramesRenderer>>

    private val progressCb = ProgressPrinter(config.silent)

    init {
        // Create components
        val convertComponent = DaggerConvertComponent.factory()
            .create(config.convert, progressCb)
        val renderCoreComponent = DaggerRenderCoreComponent.factory()
            .create(config.render, progressCb)
        DaggerRenderIrComponent.builder()
            .renderCoreComponent(renderCoreComponent)
            .build()
        DaggerRenderPdfComponent.builder()
            .renderCoreComponent(renderCoreComponent)
            .build()

        val appComponent = DaggerAppComponent.builder()
            .convertComponent(convertComponent)
            .renderCoreComponent(renderCoreComponent)
            .build()
        appComponent.inject(this)
    }

    suspend fun convert(context: SwfCollectionContext) {
        // Convert SWF to intermediate representation.
        val converter = converterProvider.get()
        val frameGroups = converter.convert(context)

        // Conver intermediate representation to output format.
        val framesRenderer = (framesRenderers.entries.find { (key, _) ->
            key.isAssignableFrom(config.render::class.java)
        } ?: error("No frame renderer registered")).value.get()
        framesRenderer.renderFrames(frameGroups)

        // Remove temp files
        logger.info { "Cleaning up temp files" }
        withContext(Dispatchers.IO) {
            converter.cleanup()
        }
    }

}
