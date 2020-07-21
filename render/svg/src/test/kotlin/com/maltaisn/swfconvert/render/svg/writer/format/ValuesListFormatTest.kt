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

class ValuesListFormatTest {

    @Test
    fun `should append values list no values`() {
        assertEquals("", buildString {
            appendValuesList(emptyArray())
        })
    }

    @Test
    fun `should append values list single value`() {
        assertEquals("0.1", buildString {
            appendValuesList(1, floatArrayOf(0.1f))
        })
    }

    @Test
    fun `should append values list multiple value`() {
        assertEquals("0.1 -0.9 0.3 -0 1", buildString {
            appendValuesList(1, floatArrayOf(0.1f, -0.9f, 0.3f, -0f, 1f))
        })
    }

    @Test
    fun `should append values list optimized no values`() {
        assertEquals("", buildString {
            appendValuesListOptimized(null, emptyArray())
        })
    }

    @Test
    fun `should append values list optimized single value`() {
        assertEquals(".1", buildString {
            appendValuesListOptimized(1, null, floatArrayOf(0.1f))
        })
    }

    @Test
    fun `should append values list optimized multiple value`() {
        assertEquals(".1-.9.3 0 1", buildString {
            appendValuesListOptimized(1, null, floatArrayOf(0.1f, -0.9f, 0.3f, -0f, 1f))
        })
    }

    @Test
    fun `should append values list optimized, adding space to separate previous value`() {
        assertEquals("1 1", buildString {
            appendValuesListOptimized(1, null, floatArrayOf(1f))
            appendValuesListOptimized(1, "1", floatArrayOf(1f))
        })
    }

    @Test
    fun `should append values list optimized, not adding space to separate previous value`() {
        assertEquals(".1.1", buildString {
            appendValuesListOptimized(1, null, floatArrayOf(0.1f))
            appendValuesListOptimized(1, "0.1", floatArrayOf(0.1f))
        })
    }

}
