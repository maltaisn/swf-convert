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

package com.maltaisn.swfconvert.render.pdf.di

import com.maltaisn.swfconvert.render.core.FramesRenderer
import com.maltaisn.swfconvert.render.core.RenderConfiguration
import com.maltaisn.swfconvert.render.core.di.FramesRendererKey
import com.maltaisn.swfconvert.render.pdf.PdfConfiguration
import com.maltaisn.swfconvert.render.pdf.PdfFramesRenderer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap


@Module
abstract class RenderPdfModule {

    @Binds
    @IntoMap
    @FramesRendererKey(PdfConfiguration::class)
    abstract fun bindsPdfFramesRenderer(framesRenderer: PdfFramesRenderer): FramesRenderer

    @Module
    companion object {
        @Provides
        @JvmStatic
        fun providesRenderConfiguration(configuration: RenderConfiguration) =
                configuration as PdfConfiguration
    }
}