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

package com.maltaisn.swfconvert.app.di

import com.maltaisn.swfconvert.convert.di.ConvertModule
import com.maltaisn.swfconvert.render.core.FramesRenderer
import com.maltaisn.swfconvert.render.core.RenderConfiguration
import com.maltaisn.swfconvert.render.ir.di.RenderIrModule
import com.maltaisn.swfconvert.render.pdf.di.RenderPdfModule
import com.maltaisn.swfconvert.render.svg.di.RenderSvgModule
import dagger.Module
import dagger.multibindings.Multibinds

@Module(includes = [
    ConvertModule::class,
    RenderIrModule::class,
    RenderPdfModule::class,
    RenderSvgModule::class
])
internal interface AppModule {

    @get:Multibinds
    val providesFrameRenderersMap: Map<Class<out RenderConfiguration>,
            @JvmSuppressWildcards FramesRenderer>

}
