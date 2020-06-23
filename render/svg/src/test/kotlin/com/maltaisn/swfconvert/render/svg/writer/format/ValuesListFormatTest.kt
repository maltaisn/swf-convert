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
