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

package com.maltaisn.swfconvert.render.svg

enum class SvgFontsMode(val embedded: Boolean) {

    /** Fonts are stored as external TTF files. */
    EXTERNAL(false),

    /** Fonts are stored as base64 encoded data URLs (TTF format). */
    BASE64(true),

    /** Fonts are not used, text is drawn using paths. */
    NONE(true),
}
