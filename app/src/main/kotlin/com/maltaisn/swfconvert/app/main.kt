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
