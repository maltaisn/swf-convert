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

import com.maltaisn.swfconvert.core.image.Color
import org.junit.Test
import kotlin.test.assertEquals

class SvgFillTest {

    @Test
    fun `should return hex color`() {
        assertEquals("#7f1f3e", SvgFillColor(Color(0x7f, 0x1f, 0x3e)).toSvg())
    }

    @Test
    fun `should return hex color shorthand`() {
        repeat(16) {
            val color = Color(it * 17, it * 17, it * 17)
            val hexStr = "#" + HEX_CHARS[it].toString().repeat(3)
            assertEquals(hexStr, SvgFillColor(color).toSvg())
        }
    }

    companion object {
        private const val HEX_CHARS = "0123456789abcdef"
    }

}
