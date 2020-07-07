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
import org.apache.logging.log4j.Level

/**
 * Params for the app itself.
 */
internal class MainParams {

    @Parameter(
        names = ["-h", "--help"],
        description = "Show help message.",
        help = true,
        order = 1000)
    var help = false

    @Parameter(
        names = ["-v", "--version"],
        description = "Show version.",
        order = 1010)
    var version = false

    @Parameter(
        names = ["--log"],
        description = "Log level from 0 (none) to 5 (all).",
        order = 1020)
    private var logLevelInt = LOG_LEVEL_WARN

    val logLevel: Level by lazy {
        when (logLevelInt) {
            LOG_LEVEL_OFF -> Level.OFF
            LOG_LEVEL_FATAL -> Level.FATAL
            LOG_LEVEL_ERROR -> Level.ERROR
            LOG_LEVEL_WARN -> Level.WARN
            LOG_LEVEL_INFO -> Level.INFO
            LOG_LEVEL_DEBUG -> Level.DEBUG
            else -> if (logLevelInt < 0) Level.OFF else Level.ALL
        }
    }

    companion object {
        private const val LOG_LEVEL_OFF = 0
        private const val LOG_LEVEL_FATAL = 1
        private const val LOG_LEVEL_ERROR = 2
        private const val LOG_LEVEL_WARN = 3
        private const val LOG_LEVEL_INFO = 4
        private const val LOG_LEVEL_DEBUG = 5
    }
}
