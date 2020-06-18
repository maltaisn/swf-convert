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

package com.maltaisn.swfconvert.render.svg.writer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame


class SvgExtensionsTest {

    @Test
    fun `should return different NumberFormat instances on different threads`() = runBlocking {
        val nbFmt0 = numberFormat.get()
        val nbFmt1 = numberFormat.get()
        val nbFmt2 = withContext(Dispatchers.IO) {
            numberFormat.get()
        }
        assertSame(nbFmt0, nbFmt1)
        assertNotSame(nbFmt1, nbFmt2)
    }

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


    @Test
    fun `should append values list no values`() {
        assertEquals("", buildString {
            appendValuesList(emptyArray())
        })
    }

    @Test
    fun `should append values list single value`() {
        assertEquals("0.1", buildString {
            appendValuesList(1, 0.1f)
        })
    }

    @Test
    fun `should append values list multiple value`() {
        assertEquals("0.1 -0.9 0.3 -0 1", buildString {
            appendValuesList(1,  0.1f, -0.9f, 0.3f, -0f, 1f)
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
            appendValuesListOptimized(1, null, 0.1f)
        })
    }

    @Test
    fun `should append values list optimized multiple value`() {
        assertEquals(".1-.9.3 0 1", buildString {
            appendValuesListOptimized(1, null, 0.1f, -0.9f, 0.3f, -0f, 1f)
        })
    }

    @Test
    fun `should append values list optimized, adding space to separate previous value`() {
        assertEquals("1 1", buildString {
            appendValuesListOptimized(1, null, 1f)
            appendValuesListOptimized(1, "1", 1f)
        })
    }

    @Test
    fun `should append values list optimized, not adding space to separate previous value`() {
        assertEquals(".1.1", buildString {
            appendValuesListOptimized(1, null, 0.1f)
            appendValuesListOptimized(1, "0.1", 0.1f)
        })
    }

}
