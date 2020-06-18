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

package com.maltaisn.swfconvert.app.params

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.beust.jcommander.ParametersDelegate
import com.maltaisn.swfconvert.app.configError
import com.maltaisn.swfconvert.core.YAxisDirection
import com.maltaisn.swfconvert.render.svg.SvgConfiguration
import org.apache.logging.log4j.kotlin.logger
import java.io.File


@Parameters(commandDescription = "SVG output format")
class RenderSvgParams : RenderParams<SvgConfiguration> {

    private val logger = logger()

    @ParametersDelegate
    override var params = CoreParams(false) {
        if (compress) "svgz" else "svg"
    }

    @Parameter(names = ["--pretty"], description = "Whether to pretty print SVG or not.", order = 1000)
    var prettyPrint: Boolean = false

    @Parameter(names = ["--svgz"], description = "Whether to use SVGZ format (gzip compression).", order = 1010)
    var compress: Boolean = false

    @Parameter(names = ["--precision"], description = "Precision of SVG path, position, and dimension values.", order=1100)
    var precision: Int = 1

    @Parameter(names = ["--transform-precision"], description = "Precision of SVG transform values.", order=1110)
    var transformPrecision: Int = 2

    @Parameter(names = ["--percent-precision"], description = "Precision of SVG percentage values.", order=1120)
    var percentPrecision: Int = 2

    @Parameter(names = ["--no-prolog"], description = "Whether to omit the XML prolog or not.", order=1200)
    var noProlog: Boolean = false


    override val yAxisDirection: YAxisDirection
        get() = YAxisDirection.DOWN


    override fun createConfigurations(inputs: List<List<File>>): List<SvgConfiguration> {
        configError(precision in 0..5 &&
                transformPrecision in 0..5 &&
                percentPrecision in 0..5) {
            "Precision must be between 0 and 5."
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
                    params.parallelFrameRendering)
        }
    }

    override fun print() {
        println("""
            |Pretty print SVG: $prettyPrint
        """.trimMargin())
    }

}
