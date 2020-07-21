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

package com.maltaisn.swfconvert.render.svg

import com.maltaisn.swfconvert.render.core.RenderConfiguration
import java.io.File

/**
 * Configuration for the SVG output format.
 */
data class SvgConfiguration(

    override val output: List<File>,
    override val tempDir: File,

    /**
     * Whether to pretty print SVG or not. Pretty printing will also make
     * paths and values list more readable by optimizing them less.
     */
    val prettyPrint: Boolean,

    /** Whether to use the SVGZ format or not (gzip compression). */
    val compress: Boolean,

    /** General number precision used for path, position and dimension values. */
    val precision: Int,

    /** Number precision used for scale and shear componenets of transforms. */
    val transformPrecision: Int,

    /** Number precision used for percentage values (gradient stops and opacity). */
    val percentPrecision: Int,

    /** Whether to write XML prolog in SVG documents or not. */
    val writeProlog: Boolean,

    /** Controls how images are included in SVG. */
    val imagesMode: SvgImagesMode,

    /** Controls how fonts are included in SVG. */
    val fontsMode: SvgFontsMode,

    // DEBUG OPTIONS

    override val parallelFrameRendering: Boolean

) : RenderConfiguration
