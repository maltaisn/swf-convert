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
