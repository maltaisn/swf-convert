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
