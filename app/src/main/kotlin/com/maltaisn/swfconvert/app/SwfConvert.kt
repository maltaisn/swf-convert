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


class SwfConvert(private val config: Configuration) {

    private val logger = logger()

    @Inject lateinit var converterProvider: Provider<SwfCollectionConverter>
    @Inject lateinit var framesRenderers: Map<Class<out RenderConfiguration>,
            @JvmSuppressWildcards Provider<FramesRenderer>>

    private val progressCb = ProgressPrinter()

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
