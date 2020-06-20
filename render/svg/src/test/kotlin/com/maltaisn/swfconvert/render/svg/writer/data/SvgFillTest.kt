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


class SvgFillTest {

    @Test
    fun `should return hex color`() {
        assertEquals("#7f1f3e", SvgFillColor(Color(0x7f, 0x1f, 0x3e)).toSvg())
    }

    @Test
    fun `should return hex color shorthand`() {
        repeat(16) {
            val color = Color(it * 17, it * 17, it * 17)
            val hexStr = "#" + HEX_CHARS[it].toString().repeat(3)
            assertEquals(hexStr, SvgFillColor(color).toSvg())
        }
    }

    companion object {
        private const val HEX_CHARS = "0123456789abcdef"
    }

}
