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
