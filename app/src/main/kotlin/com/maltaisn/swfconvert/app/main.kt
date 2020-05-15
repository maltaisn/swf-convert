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

package com.maltaisn.swfconvert.app

import com.beust.jcommander.JCommander
import com.maltaisn.swfconvert.core.SwfsConverter
import java.text.DecimalFormat
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis


fun main(rawArgs: Array<String>) {
    val args = Args()
    val commander = JCommander.newBuilder().addObject(args).build()
    commander.programName = "swf-convert"

    try {
        // Parse arguments
        try {
            commander.parse(*rawArgs)
        } catch (e: com.beust.jcommander.ParameterException) {
            argError(e.message)
        }

        if (args.version) {
            // Get version from resources and print it.
            val versionRes = Args::class.java.classLoader.getResourceAsStream("version.txt")!!
            val version = String(versionRes.readBytes())
            println("swf-convert v$version\n")
        }

        if (args.help) {
            // Show help message
            commander.usage()
            exitProcess(0)
        }

        // Create configurations
        val configs = args.createConfigurations()

        // Display options
        val options = configs.first()
        println("""
            |Output format: ${options.outputFormat.name}
            |OCR detect glyphs: ${options.ocrDetectGlyphs}
            |Group fonts: ${options.groupFonts}
            |Remove duplicate images: ${options.removeDuplicateImages}
            |Downsample images: ${options.downsampleImages}
            """.trimMargin())
        if (options.downsampleImages) {
            println("""
                |Downsample filter: ${args.downsampleFilterName}
                |Downsample min size: ${options.downsampleMinSize}
            """.trimMargin())
        }
        println("""
            |Max DPI: ${NUMBER_FMT.format(options.maxDpi)}
            |JPEG quality: ${args.jpegQuality} %
            |Image format: ${args.imageFormatName}
            |Rasterization enabled: ${options.rasterizationEnabled}
        """.trimMargin())
        if (options.rasterizationEnabled) {
            println("""
            |Rasterization threshold: ${NUMBER_FMT.format(options.rasterizationThreshold)}
            |Rasterization DPI: ${NUMBER_FMT.format(options.rasterizationDpi)}
            |Rasterizer: ${options.rasterizer}
            """.trimMargin())
        }
        println()

        val converter = SwfsConverter()
        for ((i, config) in configs.withIndex()) {
            println("Converting collection ${i + 1} / ${configs.size}")
            val duration = measureTimeMillis {
                converter.convertSwfs(config)
            }
            println("Done in ${DURATION_FMT.format(duration / 1000.0)} s\n")
        }

        exitProcess(0)

    } catch (e: ArgumentException) {
        println("ERROR: ${e.message}\n")
        exitProcess(1)
    }
}

private val DURATION_FMT = DecimalFormat("0.00")
private val NUMBER_FMT = DecimalFormat()
