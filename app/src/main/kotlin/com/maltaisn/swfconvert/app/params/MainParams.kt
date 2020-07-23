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

    @Parameter(
        names = ["-s", "--silent"],
        description = "Don't display conversion progress",
        order = 20)
    var silent: Boolean = false

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
