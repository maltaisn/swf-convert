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

import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.render.core.RenderConfiguration

/**
 * Configuration used for converting a SWF file collection to the output format.
 */
internal data class Configuration(
    val convert: ConvertConfiguration,
    val render: RenderConfiguration,
    val silent: Boolean
)
