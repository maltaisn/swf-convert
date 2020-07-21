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

package com.maltaisn.swfconvert.render.svg.writer.format

import org.junit.Test
import kotlin.test.assertEquals

class ValueFormatTest {

    @Test
    fun `should format value precision=0`() {
        assertEquals("1", 1.123456f.format(0))
    }

    @Test
    fun `should format value precision=2`() {
        assertEquals("1.12", 1.123456f.format(2))
    }

    @Test
    fun `should format value leading zero`() {
        assertEquals("0.1", 0.1f.format(1))
    }

    @Test
    fun `should format value negative`() {
        assertEquals("-1.3", (-1.3f).format(2))
    }

    @Test
    fun `should format value leading zero and negative`() {
        assertEquals("-0.3", (-0.3f).format(2))
    }

    @Test
    fun `should format value minus zero`() {
        assertEquals("-0", (-0f).format(1))
    }

    @Test
    fun `should format value optimized precision=2`() {
        assertEquals("1.12", 1.123456f.format(2))
    }

    @Test
    fun `should format value optimized leading zero`() {
        assertEquals(".1", 0.1f.formatOptimized(1))
    }

    @Test
    fun `should format value optimized minus zero`() {
        assertEquals("0", (-0f).formatOptimized(1))
    }

    @Test
    fun `should format value optimized negative`() {
        assertEquals("-1.3", (-1.3f).formatOptimized(2))
    }

    @Test
    fun `should format value optimized leading zero and negative`() {
        assertEquals("-.3", (-0.3f).formatOptimized(2))
    }

}
