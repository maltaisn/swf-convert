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

package com.maltaisn.swfconvert.app

import com.maltaisn.swfconvert.app.params.ParamsParser
import com.maltaisn.swfconvert.convert.ConversionError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.logger
import java.text.DecimalFormat
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    // Parse program arguments into configurations.
    // This also prints configuration values.
    val configs = ParamsParser().parse(args)

    // Do each conversion
    try {
        for ((i, config) in configs.withIndex()) {
            println("Converting collection ${i + 1} / ${configs.size}")
            logger.info { "Started conversion of collection ${i + 1}, config: $config" }
            val duration = measureTimeMillis {
                runBlocking(Dispatchers.Default) {
                    SwfConvert(config).convert(SwfCollectionContext(i))
                }
            }
            println("Done in ${DURATION_FMT.format(duration.toDouble() / MILLIS_IN_SECOND)} s\n")
            logger.info { "Finished conversion of collection ${i + 1}" }
        }
        exitProcess(0)

    } catch (e: ConversionError) {
        logger.fatal("Conversion error", e)
        println("Conversion error: ${e.message}")

    } catch (e: Exception) {
        logger.fatal("Unknown error", e)
        println("Unknown error occurred")
        e.printStackTrace()
    }
    exitProcess(1)
}

private val logger = logger(object {}.javaClass.name)

private val DURATION_FMT = DecimalFormat("0.00")
private const val MILLIS_IN_SECOND = 1000
