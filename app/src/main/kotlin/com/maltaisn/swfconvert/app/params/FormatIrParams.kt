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
import com.maltaisn.swfconvert.render.ir.IrConfiguration


@Parameters(commandDescription = "Intermediate representation output format")
class FormatIrParams : FormatParams<IrConfiguration> {

    @ParametersDelegate
    override var params = BaseParams(false, "json").apply {
        // Keep fonts and images by default for IR output.
        this.params[BaseParams.OPT_KEEP_FONTS] = true.toString()
        this.params[BaseParams.OPT_KEEP_IMAGES] = true.toString()
    }

    @Parameter(names = ["--pretty"], description = "Whether to pretty print JSON or not.", order = 1000)
    var prettyPrint: Boolean = false


    override fun createConfigurations(count: Int): List<IrConfiguration> {
        val config = IrConfiguration(prettyPrint)
        return List(count) { config }
    }

    override fun print() {
        println("Pretty print JSON: $prettyPrint")
    }

}
