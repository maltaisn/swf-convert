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

package com.maltaisn.swfconvert.render.svg.writer.data

import com.maltaisn.swfconvert.core.image.Color
import org.junit.Test
import kotlin.test.assertEquals

class SvgGraphicsStateTest {

    @Test
    fun `should use base values for attributes`() {
        val base = SvgGraphicsState(
            fill = SvgFillColor(Color.RED),
            fillRule = SvgFillRule.EVEN_ODD)
        val new = SvgGraphicsState(base,
            fill = SvgFillColor(Color.BLUE))
        assertEquals(new.fill, SvgFillColor(Color.BLUE))
        assertEquals(new.fillRule, SvgFillRule.EVEN_ODD)
    }

    @Test
    fun `should clean duplicate inheritable attribute values`() {
        val ancestor1 = SvgGraphicsState(
            fill = SvgFillColor(Color.RED),
            fillRule = SvgFillRule.EVEN_ODD,
            x = SvgNumber(100f))
        val ancestor2 = SvgGraphicsState(
            fillRule = SvgFillRule.NON_ZERO)
        val latest = SvgGraphicsState(
            fill = SvgFillColor(Color.RED),
            fillRule = SvgFillRule.NON_ZERO,
            x = SvgNumber(100f))
        assertEquals(SvgGraphicsState(
            x = SvgNumber(100f)
        ), latest.clean(listOf(ancestor2, ancestor1)))
    }

    @Test
    fun `should clean identity values`() {
        val state = SvgGraphicsState(
            transform = emptyList(),
            x = SvgNumber.ZERO,
            y = SvgNumber.ZERO)
        assertEquals(SvgGraphicsState(), state.clean())
    }

    @Test
    fun `should omit clip if no ancestor resets it`() {
        val base = SvgGraphicsState(clipPathId = "clip")
        val new = SvgGraphicsState(clipPathId = "clip")
        assertEquals(SvgGraphicsState(), new.clean(listOf(base)))
    }

    @Test
    fun `should omit clip if no ancestor resets it 2`() {
        val base1 = SvgGraphicsState(x = SvgNumber(10f))
        val base2 = SvgGraphicsState(clipPathId = "clip")
        val new = SvgGraphicsState(clipPathId = "clip")
        assertEquals(SvgGraphicsState(), new.clean(listOf(base2, base1)))
    }

    @Test
    fun `should reset clip if non-identity x, y or transform is in ancestors`() {
        val root = SvgGraphicsState(clipPathId = "clip")
        val stateX = SvgGraphicsState(x = SvgNumber(10f))
        val stateY = SvgGraphicsState(y = SvgNumber(10f))
        val stateTransform = SvgGraphicsState(transform = listOf(SvgTransform.Translate(10f)))
        val new = SvgGraphicsState(clipPathId = "clip")
        assertEquals(SvgGraphicsState(clipPathId = "clip"), new.clean(listOf(stateX, root)))
        assertEquals(SvgGraphicsState(clipPathId = "clip"), new.clean(listOf(stateY, root)))
        assertEquals(SvgGraphicsState(clipPathId = "clip"), new.clean(listOf(stateTransform, root)))
    }

    @Test
    fun `should not reset clip if identity x, y or transform is in ancestors`() {
        val root = SvgGraphicsState(clipPathId = "clip")
        val stateX = SvgGraphicsState(x = SvgNumber.ZERO)
        val stateY = SvgGraphicsState(y = SvgNumber.ZERO)
        val stateTransform = SvgGraphicsState(transform = emptyList())
        val new = SvgGraphicsState(clipPathId = "clip")
        assertEquals(SvgGraphicsState(), new.clean(listOf(stateX, root)))
        assertEquals(SvgGraphicsState(), new.clean(listOf(stateY, root)))
        assertEquals(SvgGraphicsState(), new.clean(listOf(stateTransform, root)))
    }

    @Test
    fun `should reset clip if non-identity x and same clip is in direct ancestor`() {
        val root = SvgGraphicsState(x = SvgNumber(10f), clipPathId = "clip")
        val new = SvgGraphicsState(clipPathId = "clip")
        assertEquals(SvgGraphicsState(clipPathId = "clip"), new.clean(listOf(root)))
    }

}
