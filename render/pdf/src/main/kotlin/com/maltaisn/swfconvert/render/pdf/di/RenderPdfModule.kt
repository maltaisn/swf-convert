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

    @get:[Binds IntoMap FramesRendererKey(PdfConfiguration::class)]
    internal abstract val PdfFramesRenderer.bindsPdfFramesRenderer: FramesRenderer

    companion object {
        @Provides
        fun providesRenderConfiguration(configuration: RenderConfiguration) =
            configuration as PdfConfiguration
    }
}
