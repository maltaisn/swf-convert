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
import com.maltaisn.swfconvert.core.SwfCollectionConverter
import com.maltaisn.swfconvert.core.di.DaggerCoreComponent
import com.maltaisn.swfconvert.render.core.FramesRenderer
import com.maltaisn.swfconvert.render.core.RenderConfiguration
import com.maltaisn.swfconvert.render.core.di.DaggerRenderCoreComponent
import com.maltaisn.swfconvert.render.ir.di.DaggerRenderIrComponent
import com.maltaisn.swfconvert.render.pdf.di.DaggerRenderPdfComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Provider


class SwfConvert(private val config: Configuration) {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    @Inject lateinit var converterProvider: Provider<SwfCollectionConverter>
    @Inject lateinit var framesRenderers: Map<Class<out RenderConfiguration>,
            @JvmSuppressWildcards Provider<FramesRenderer>>

    init {
        // Create components
        val coreComponent = DaggerCoreComponent.factory()
                .create(coroutineScope, config.core)
        val renderCoreComponent = DaggerRenderCoreComponent.factory()
                .create(coroutineScope, config.render)
        DaggerRenderIrComponent.builder()
                .renderCoreComponent(renderCoreComponent)
                .build()
        DaggerRenderPdfComponent.builder()
                .renderCoreComponent(renderCoreComponent)
                .build()

        val appComponent = DaggerAppComponent.builder()
                .coreComponent(coreComponent)
                .renderCoreComponent(renderCoreComponent)
                .build()
        appComponent.inject(this)
    }

    fun convert() {
        // Convert SWF to intermediate representation.
        val converter = converterProvider.get()
        val frameGroups = converter.convert()

        // Conver intermediate representation to output format.
        val framesRenderer = (framesRenderers.entries.find { (key, _) ->
            key.isAssignableFrom(config.render::class.java)
        } ?: error("No frame renderer registered")).value.get()
        framesRenderer.renderFrames(frameGroups)

        // Remove temp files
        converter.cleanup()
    }

}
