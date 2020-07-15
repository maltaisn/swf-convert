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
import com.maltaisn.swfconvert.render.ir.IrConfiguration
import java.io.File

@Parameters(commandDescription = "Intermediate representation output format")
internal class RenderIrParams : RenderParams<IrConfiguration> {

    @ParametersDelegate
    override var params = CoreParams(false) { "json" }

    @Parameter(
        names = ["-y-direction"],
        description = "Y axis direction: up | down.",
        order = 1000)
    var yAxisDirectionName = "up"

    @Parameter(
        names = ["-p", "--pretty"],
        description = "Pretty print output JSON.",
        order = 1010)
    var prettyPrint: Boolean = false

    @Parameter(
        names = ["--indent-size"],
        description = "Indent size if pretty printing",
        order = 1020)
    var indentSize: Int = 2

    override val yAxisDirection by lazy {
        when (yAxisDirectionName.toLowerCase()) {
            "up" -> YAxisDirection.UP
            "down" -> YAxisDirection.DOWN
            else -> configError("Invalid Y axis direction '$yAxisDirectionName'.")
        }
    }

    override fun createConfigurations(inputs: List<List<File>>): List<IrConfiguration> {
        configError(indentSize >= 0) { "Indent size must be positive." }
        return inputs.mapIndexed { i, input ->
            val tempDir = params.getTempDirForInput(input)
            IrConfiguration(
                params.outputFiles[i],
                tempDir,
                prettyPrint,
                indentSize,
                params.parallelFrameRendering)
        }
    }

    override fun print() {
        println("""
            |Y axis direction: ${yAxisDirection.name.toLowerCase()}
            |Pretty print JSON: $prettyPrint
        """.trimMargin())
    }

}
