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

import org.junit.ComparisonFailure
import org.junit.Test
import kotlin.test.assertEquals


class SvgPathWriterTest {

    @Test
    fun `should write 'move to' command`() {
        assertPathEquals("M10 10") {
            moveTo(10f, 10f)
        }
    }

    @Test
    fun `should write 'line to' command`() {
        assertPathEquals("L10 10") {
            lineTo(10f, 10f)
        }
    }

    @Test
    fun `should write 'quad to' command`() {
        assertPathEquals("Q5 5 10 10") {
            quadTo(5f, 5f, 10f, 10f)
        }
    }

    @Test
    fun `should write 'cubic to' command`() {
        assertPathEquals("C5 5 10 10 15 15") {
            cubicTo(5f, 5f, 10f, 10f, 15f, 15f)
        }
    }

    @Test
    fun `should write horizontal line (absolute)`() {
        assertPathEquals("M10 10H8") {
            moveTo(10f, 10f)
            lineTo(8f, 10f)
        }
    }

    @Test
    fun `should write horizontal line (relative)`() {
        assertPathEquals("M10 10h2") {
            moveTo(10f, 10f)
            lineTo(12f, 10f)
        }
    }

    @Test
    fun `should write vertical line (absolute)`() {
        assertPathEquals("M10 10V8") {
            moveTo(10f, 10f)
            lineTo(10f, 8f)
        }
    }

    @Test
    fun `should write vertical line (relative)`() {
        assertPathEquals("M10 10v2") {
            moveTo(10f, 10f)
            lineTo(10f, 12f)
        }
    }

    @Test
    fun `should write shorter absolute command`() {
        assertPathEquals("M10 10Q9 9 8 8") {
            moveTo(10f, 10f)
            quadTo(9f, 9f, 8f, 8f)
        }
    }

    @Test
    fun `should write shorter relative command`() {
        assertPathEquals("M10 10q1 1 2 2") {
            moveTo(10f, 10f)
            quadTo(11f, 11f, 12f, 12f)
        }
    }

    @Test
    fun `should write multiple commands repeated ('quad to')`() {
        assertPathEquals("M10 10Q8 8 6 6 4 4 2 2") {
            moveTo(10f, 10f)
            quadTo(8f, 8f, 6f, 6f)
            quadTo(4f, 4f, 2f, 2f)
        }
    }

    @Test
    fun `should not write multiple commands repeated ('move to')`() {
        assertPathEquals("M10 10M20 20M30 30") {
            moveTo(10f, 10f)
            moveTo(20f, 20f)
            moveTo(30f, 30f)
        }
    }

    @Test
    fun `should write multiple commands with implicit 'line to' (absolute)`() {
        assertPathEquals("M10 10 30 30 55 50 80 70") {
            moveTo(10f, 10f)
            lineTo(30f, 30f)
            lineTo(55f, 50f)
            lineTo(80f, 70f)
        }
    }

    @Test
    fun `should write multiple commands with implicit 'line to' (relative)`() {
        assertPathEquals("M110 110m1 1 19 19 25 20 25 20") {
            // Larger numbers and 2 'move to' commands are used so that relative commands will be shorter.
            moveTo(110f, 110f)
            moveTo(111f, 111f)
            lineTo(130f, 130f)
            lineTo(155f, 150f)
            lineTo(180f, 170f)
        }
    }

    @Test
    fun `should not write leading zeroes`() {
        assertPathEquals("M.1-.1Q1-1 0 .5") {
            moveTo(0.1f, -0.1f)
            quadTo(1f, -1f, 0f, 0.5f)
        }
    }

    @Test
    fun `should not write whitespace if sign and decimal point allow it`() {
        assertPathEquals("M.1.1Q0 .1 0-.1L.1 1.1Z") {
            moveTo(0.1f, 0.1f)
            quadTo(0f, 0.1f, 0f, -0.1f)
            lineTo(0.1f, 1.1f)
            closePath()
        }
    }

    @Test
    fun `should not write minus zero ('-0')`() {
        assertPathEquals("M0 0") {
            moveTo(-0f, -0.0001f)
        }
    }

    @Test
    fun `should not write invisible 'line to'`() {
        assertPathEquals("M10 10") {
            moveTo(10f, 10f)
            lineTo(10f, 10f)
        }
    }

    @Test
    fun `should not write invisible 'quad to'`() {
        assertPathEquals("M10 10") {
            moveTo(10f, 10f)
            quadTo(20f, 20f, 10f, 10f)
        }
    }

    @Test
    fun `should not write invisible 'cubic to'`() {
        assertPathEquals("M10 10") {
            moveTo(10f, 10f)
            cubicTo(20f, 20f, 30f, 30f,10f, 10f)
        }
    }

    @Test
    fun `should not write invisible horizontal line`() {
        assertPathEquals("M10 10") {
            moveTo(10f, 10f)
            lineTo(10.01f, 10f)
        }
    }

    @Test
    fun `should not write invisible vertical line`() {
        assertPathEquals("M10 10") {
            moveTo(10f, 10f)
            lineTo(10f, 10.01f)
        }
    }

    @Test
    fun `should not accumulate rounding error with relative commands (precision 0)`() {
        assertPathEquals("M10 10h1", precision = 0) {
            moveTo(10f, 10f)
            // Let's write 6 x 0.1 px horizontal line segments that individually round to "h 0"
            // but should eventually lead to "h 1" when taken together.
            repeat(6) {
                lineTo(10f + (it + 1) * 0.1f, 10f)
            }
        }
    }

    @Test
    fun `should not accumulate rounding error with relative commands (precision 1)`() {
        assertPathEquals("M10 10h.1.1.1.1.2.1", "M10 10h.1.1.1.1.1.2", precision = 1) {
            moveTo(10f, 10f)
            // Same as the other test, but with more precision.
            // Note that multiple results are accepted here due to float rounding errors.
            repeat(6) {
                lineTo(10f + (it + 1) * 0.11f, 10f)
            }
        }
    }

    @Test
    fun `should write unoptimized path`() {
        assertPathEquals("M 10 10 H 20 V 20 H 10 Z", optimize = false) {
            moveTo(10f, 10f)
            lineTo(20f, 10f)
            lineTo(20f, 20f)
            lineTo(10f, 20f)
            closePath()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should fail to create writer with negative precision`() {
        SvgPathWriter(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should fail to create writer with excessive precision`() {
        SvgPathWriter(100)
    }

    private fun assertPathEquals(vararg expected: String, precision: Int = 1,
                                 optimize: Boolean = true,
                                 write: SvgPathWriter.() -> Unit) {
        val actual = SvgPathWriter(precision, optimize).apply(write).toString()
        var passed = 0
        for ((i, exp) in expected.withIndex()) {
            try {
                assertEquals(exp, actual)
                passed++
            } catch (e: ComparisonFailure) {
                if (i == expected.lastIndex) {
                    // All failed
                    throw e
                }
            }
        }
    }

}
