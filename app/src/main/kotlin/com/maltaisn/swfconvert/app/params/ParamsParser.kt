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

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import com.maltaisn.swfconvert.app.ConfigException
import com.maltaisn.swfconvert.app.Configuration
import com.maltaisn.swfconvert.app.configError
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import kotlin.system.exitProcess

internal class ParamsParser {

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

            setConsoleLogLevel()

            val command = commands[jc.parsedCommand] ?: return emptyList()

            if (!mainParams.silent) {
                // Print configuration
                command.params.print()
                command.print()
                println()
            }

            // Create configuration
            val mainConfigs = command.params.createConfigurations(command.yAxisDirection)
            val formatConfigs = command.createConfigurations(mainConfigs.map { it.input })
            return mainConfigs.zip(formatConfigs) { convert, render ->
                Configuration(convert, render, mainParams.silent)
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

    private fun setConsoleLogLevel() {
        val context = LogManager.getContext(false) as LoggerContext
        val config = context.configuration
        config.rootLogger.let {
            it.removeAppender("stdout")
            it.addAppender(config.getAppender("stdout"), mainParams.logLevel, null)
        }
    }
}
