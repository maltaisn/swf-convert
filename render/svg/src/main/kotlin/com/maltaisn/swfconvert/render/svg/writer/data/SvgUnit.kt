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

package com.maltaisn.swfconvert.render.svg.writer.data

/**
 * Units that can be used by [SvgNumber].
 * [https://www.w3.org/TR/SVG2/coords.html#Units] and [https://www.w3.org/TR/css-values/#lengths].
 * Most units are provided for completeness but only are actually used.
 */
internal enum class SvgUnit(val symbol: String) {
    USER(""), // User units, as defined by the viewport.
    PX("px"),
    CM("cm"),
    MM("mm"),
    PT("pt"),
    IN("in"),
    EM("em"),
    PERCENT("%")
}
