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
import com.maltaisn.swfconvert.core.YAxisDirection
import com.maltaisn.swfconvert.render.svg.SvgConfiguration
import java.io.File


@Parameters(commandDescription = "SVG output format")
class RenderSvgParams : RenderParams<SvgConfiguration> {

    @ParametersDelegate
    override var params = CoreParams(false, "svg")

    @Parameter(names = ["--pretty"], description = "Whether to pretty print SVG or not.", order = 1000)
    var prettyPrint: Boolean = false

    override val yAxisDirection: YAxisDirection
        get() = YAxisDirection.DOWN


    override fun createConfigurations(inputs: List<List<File>>) = inputs.mapIndexed { i, input ->
        val tempDir = params.getTempDirForInput(input)
        SvgConfiguration(
                params.outputFiles[i],
                tempDir,
                prettyPrint,
                params.parallelFrameRendering)
    }

    override fun print() {
        println("""
            |Pretty print SVG: $prettyPrint
        """.trimMargin())
    }

}
