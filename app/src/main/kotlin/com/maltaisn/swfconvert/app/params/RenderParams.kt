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

import com.maltaisn.swfconvert.app.ConfigException
import com.maltaisn.swfconvert.core.YAxisDirection
import java.io.File

internal interface RenderParams<T> {

    val params: CoreParams

    val yAxisDirection: YAxisDirection

    /**
     * Validate and create the configurations associated to the parameters.
     *
     * @param inputs Input file collections. Any parameter related to it should have the same number
     * of values. This function is also expected to return a list containing as many configurations.
     *
     * @throws ConfigException Thrown on validation error.
     */
    fun createConfigurations(inputs: List<List<File>>): List<T>

    /**
     * Print the values of the parameters to the standard output.
     */
    fun print()

}
