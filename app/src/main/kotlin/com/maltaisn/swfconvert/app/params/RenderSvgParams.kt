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

package com.maltaisn.swfconvert.app.params

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.beust.jcommander.ParametersDelegate
import com.maltaisn.swfconvert.app.configError
import com.maltaisn.swfconvert.core.YAxisDirection
import com.maltaisn.swfconvert.render.svg.SvgConfiguration
import com.maltaisn.swfconvert.render.svg.SvgFontsMode
import com.maltaisn.swfconvert.render.svg.SvgImagesMode
import com.maltaisn.swfconvert.render.svg.writer.format.requireSvgPrecision
import org.apache.logging.log4j.kotlin.logger
import java.io.File

@Parameters(commandDescription = "SVG output format")
internal class RenderSvgParams : RenderParams<SvgConfiguration> {

    private val logger = logger()

    @ParametersDelegate
    override var params = CoreParams(false) {
        if (compress) "svgz" else "svg"
    }

    @Parameter(
        names = ["-p", "--pretty"],
        description = "Pretty print output SVG.",
        order = 1000)
    var prettyPrint: Boolean = false

    @Parameter(
        names = ["--svgz"],
        description = "Use SVGZ format (gzip compression).",
        order = 1010)
    var compress: Boolean = false

    @Parameter(
        names = ["--precision"],
        description = "Precision of SVG path, position, and dimension values.",
        order = 1100)
    var precision: Int = 1

    @Parameter(
        names = ["--transform-precision"],
        description = "Precision of SVG transform values.",
        order = 1110)
    var transformPrecision: Int = 2

    @Parameter(
        names = ["--percent-precision"],
        description = "Precision of SVG percentage values.",
        order = 1120)
    var percentPrecision: Int = 2

    @Parameter(
        names = ["--no-prolog"],
        description = "Omit the XML prolog.",
        order = 1200)
    var noProlog: Boolean = false

    @Parameter(
        names = ["--images-mode"],
        description = "Controls how images are included in SVG: external | base64",
        order = 1300)
    var imagesModeName: String = MODE_EXTERNAL

    @Parameter(
        names = ["--fonts-mode"],
        description = "Controls how fonts are included in SVG: external | base64",
        order = 1310)
    var fontsModeName: String = MODE_EXTERNAL

    override val yAxisDirection: YAxisDirection
        get() = YAxisDirection.DOWN

    private val imagesMode: SvgImagesMode
        get() = when (imagesModeName.toLowerCase()) {
            MODE_EXTERNAL -> SvgImagesMode.EXTERNAL
            MODE_BASE64 -> SvgImagesMode.BASE64
            else -> configError("Unknown images mode '$imagesModeName'.")
        }

    private val fontsMode: SvgFontsMode
        get() = when (fontsModeName.toLowerCase()) {
            MODE_EXTERNAL -> SvgFontsMode.EXTERNAL
            MODE_BASE64 -> SvgFontsMode.BASE64
            MODE_NONE -> SvgFontsMode.NONE
            else -> configError("Unknown fonts mode '$fontsModeName'.")
        }

    override fun createConfigurations(inputs: List<List<File>>): List<SvgConfiguration> {
        try {
            requireSvgPrecision(precision)
            requireSvgPrecision(transformPrecision)
            requireSvgPrecision(percentPrecision)
        } catch (e: IllegalArgumentException) {
            configError(e.message!!)
        }

        if (prettyPrint && compress) {
            logger.warn { "Suboptimal configuration is used: pretty print with SVGZ compression." }
        }

        return inputs.mapIndexed { i, input ->
            val tempDir = params.getTempDirForInput(input)
            SvgConfiguration(
                params.outputFiles[i],
                tempDir,
                prettyPrint,
                compress,
                precision,
                transformPrecision,
                percentPrecision,
                !noProlog,
                imagesMode,
                fontsMode,
                params.parallelFrameRendering)
        }
    }

    override fun print() {
        println("""
            |Pretty print SVG: $prettyPrint
            |Use SVGZ compression: $compress
            |Precision: $precision
            |  Transform precision: $transformPrecision
            |  Percent precision: $percentPrecision
            |Output prolog: ${!noProlog}
            |Images mode: ${imagesModeName.toLowerCase()}
            |Fonts mode: ${fontsModeName.toLowerCase()}
        """.trimMargin())
    }

    companion object {
        private const val MODE_EXTERNAL = "external"
        private const val MODE_BASE64 = "base64"
        private const val MODE_NONE = "none"
    }
}
