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
import java.text.DecimalFormat
import kotlin.system.measureTimeMillis


fun main(args: Array<String>) {
    // Parse program arguments into configurations.
    // This also prints configuration values.
    val configs = ParamsParser().parse(args)

    // Do each conversion
    try {
        for ((i, config) in configs.withIndex()) {
            println("Converting collection ${i + 1} / ${configs.size}")
            val duration = measureTimeMillis {
                SwfConvert(config).convert()
            }
            println("Done in ${DURATION_FMT.format(duration / 1000.0)} s\n")
        }
    } catch (e: ConversionError) {
        // TODO
        println()
    }
}

private val DURATION_FMT = DecimalFormat("0.00")
