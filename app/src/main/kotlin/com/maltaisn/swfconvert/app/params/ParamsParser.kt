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

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import com.maltaisn.swfconvert.app.ConfigException
import com.maltaisn.swfconvert.app.Configuration
import com.maltaisn.swfconvert.app.configError
import org.apache.logging.log4j.core.config.Configurator
import kotlin.system.exitProcess

class ParamsParser {

    private val mainParams = MainParams()

    private val irParams = RenderIrParams()
    private val pdfParams = RenderPdfParams()
    private val svgParams = RenderSvgParams()

    private val commands = mapOf(
        "ir" to irParams,
        "pdf" to pdfParams,
        "svg" to svgParams
    )

    private val jc = JCommander.newBuilder().run {
        programName("swf-convert")
        addObject(mainParams)
        for ((name, command) in commands) {
            addCommand(name, command)
        }

        build()
    }

    fun parse(args: Array<String>): List<Configuration> {
        try {
            // Parse arguments
            try {
                jc.parse(*args)
            } catch (e: ParameterException) {
                // Failed to parse arguments
                configError(e.message ?: "")
            }

            showVersionIfNeeded()
            showHelpIfNeeded()
            Configurator.setRootLevel(mainParams.logLevel)

            val command = commands[jc.parsedCommand] ?: return emptyList()

            // Print configuration
            command.params.print()
            command.print()
            println()

            // Create configuration
            val mainConfigs = command.params.createConfigurations(command.yAxisDirection)
            val formatConfigs = command.createConfigurations(mainConfigs.map { it.input })
            return mainConfigs.zip(formatConfigs) { convert, render ->
                Configuration(convert, render)
            }

        } catch (e: ConfigException) {
            // Configuration error
            println("ERROR: ${e.message}")
            e.cause?.printStackTrace()
            exitProcess(1)
        }
    }

    private fun showVersionIfNeeded() {
        if (mainParams.version) {
            // Get version from resources and print it.
            val versionRes = MainParams::class.java.classLoader.getResourceAsStream("version.txt")!!
            val version = String(versionRes.readBytes())
            println("swf-convert v$version\n")
        }
    }

    private fun showHelpIfNeeded() {
        if (mainParams.help) {
            jc.usage()
            exitProcess(0)
        }

        val commandHelp = commands.entries.find { (_, cmd) -> cmd.params.help }?.key
        if (commandHelp != null) {
            jc.findCommandByAlias(commandHelp).usage()
            exitProcess(0)
        }
    }

}
