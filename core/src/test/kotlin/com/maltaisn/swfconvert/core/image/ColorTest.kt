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

package com.maltaisn.swfconvert.core.image

import org.junit.Test
import kotlin.test.assertEquals

class ColorTest {

    @Test
    fun `should give each component value`() {
        val color = Color(0x5E3F09AD)
        assertEquals(0x5E, color.a)
        assertEquals(0x3F, color.r)
        assertEquals(0x09, color.g)
        assertEquals(0xAD, color.b)
    }

    @Test
    fun `should give awt color no alpha`() {
        val color = Color(0xFF3F09AD.toInt())
        assertEquals(java.awt.Color(0x3F, 0x09, 0xAD), color.toAwtColor())
    }

    @Test
    fun `should give awt color with alpha`() {
        val color = Color(0x5E3F09AD)
        assertEquals(java.awt.Color(0x3F, 0x09, 0xAD, 0x5E), color.toAwtColor())
    }

    @Test
    fun `should give hex value`() {
        val color = Color(0x5E3F09AD)
        assertEquals("#5e3f09ad", color.toString())
    }

    @Test
    fun `should give hex value without alpha`() {
        val color = Color(0x5E3F09AD)
        assertEquals("#3f09ad", color.toStringNoAlpha())
    }

    @Test
    fun `should give color with different alpha`() {
        val color = Color(0x5E3F09AD)
        assertEquals(Color(0x013F09AD), color.withAlpha(0x01))
    }

    @Test
    fun `should give RGB color`() {
        assertEquals(Color(0xFF010203.toInt()), Color(0x01, 0x02, 0x03))
    }

    @Test
    fun `should give ARGB color`() {
        assertEquals(Color(0x04010203), Color(0x01, 0x02, 0x03, 0x04))
    }

    @Test
    fun `should give opaque gray`() {
        val color = Color.gray(0x56)
        assertEquals(Color(0xFF565656.toInt()), color)
    }
}
