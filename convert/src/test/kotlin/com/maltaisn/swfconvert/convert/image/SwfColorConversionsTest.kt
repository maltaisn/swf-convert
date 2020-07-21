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

package com.maltaisn.swfconvert.convert.image

import com.maltaisn.swfconvert.core.image.Color
import org.junit.Test
import kotlin.test.assertEquals

class SwfColorConversionsTest {

    @Test
    fun `should create color from pix15 bytes`() {
        val bytes = byteArrayOf(0x33, 0x95.toByte())
        assertEquals(Color(0xFF60E0A8.toInt()), bytes.pix15BytesToColor(0))
    }

    @Test
    fun `should create color from pix24 bytes`() {
        val bytes = byteArrayOf(0x00, 0x01, 0x02, 0x03)
        assertEquals(Color(0xFF010203.toInt()), bytes.pix24BytesToColor(0))
    }

    @Test
    fun `should create color from rgb bytes`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03)
        assertEquals(Color(0xFF010203.toInt()), bytes.rgbBytesToColor(0))
    }

    @Test
    fun `should create color from rgba bytes`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        assertEquals(Color(0x04010203), bytes.rgbaBytesToColor(0))
    }

    @Test
    fun `should create color from argb bytes`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        assertEquals(Color(0x01020304), bytes.argbBytesToColor(0))
    }

}
