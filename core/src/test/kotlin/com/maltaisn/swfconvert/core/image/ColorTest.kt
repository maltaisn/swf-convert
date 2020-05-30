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
        assertEquals("#5E3F09AD", color.toString())
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

    @Test
    fun `should color from pix15 bytes`() {
        val bytes = byteArrayOf(0x33, 0x95.toByte())
        assertEquals(Color(0xFF60E0A8.toInt()), Color.fromPix15Bytes(bytes, 0))
    }

    @Test
    fun `should color from pix24 bytes`() {
        val bytes = byteArrayOf(0x00, 0x01, 0x02, 0x03)
        assertEquals(Color(0xFF010203.toInt()), Color.fromPix24Bytes(bytes, 0))
    }

    @Test
    fun `should color from rgb bytes`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03)
        assertEquals(Color(0xFF010203.toInt()), Color.fromRgbBytes(bytes, 0))
    }

    @Test
    fun `should color from rgba bytes`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        assertEquals(Color(0x04010203), Color.fromRgbaBytes(bytes, 0))
    }

    @Test
    fun `should color from argb bytes`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        assertEquals(Color(0x01020304), Color.fromArgbBytes(bytes, 0))
    }

}
