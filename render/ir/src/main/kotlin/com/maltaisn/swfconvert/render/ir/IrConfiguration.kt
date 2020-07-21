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

package com.maltaisn.swfconvert.render.ir

import com.maltaisn.swfconvert.render.core.RenderConfiguration
import java.io.File

/**
 * Configuration for the intermediate representation output format.
 */
data class IrConfiguration(

    override val output: List<File>,
    override val tempDir: File,

    /** Whether to pretty print JSON or not. */
    val prettyPrint: Boolean,

    /** Indent size (number of spaces) if [prettyPrint] is `true`. */
    val indentSize: Int,

    // DEBUG OPTIONS

    override val parallelFrameRendering: Boolean

) : RenderConfiguration
