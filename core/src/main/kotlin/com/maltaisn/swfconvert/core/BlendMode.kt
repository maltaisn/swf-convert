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

package com.maltaisn.swfconvert.core

/**
 * Blend modes in the intermediate representation, mostly similar to the ones defined by SWF.
 *
 * These blend modes should be easily applied for most output formats, with the exception of:
 * - `ALPHA`: this blend mode acts like a mask which is supported with [GroupObject.Masked].
 * - `LAYER`: sets the opacity of the layer to 100% before blending, limited support.
 * - `INVERT`: inverts color, not really a blend mode. If further support is intended, this shouldn't be a blend mode.
 *
 * An output format should log an error if unsupported blend modes are used.
 */
enum class BlendMode {
    NULL,
    NORMAL,
    LAYER,
    MULTIPLY,
    SCREEN,
    LIGHTEN,
    DARKEN,
    ADD,
    SUBTRACT,
    DIFFERENCE,
    INVERT,
    ERASE,
    OVERLAY,
    HARDLIGHT
}
