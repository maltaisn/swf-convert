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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class NumberFormatTest {

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

    @Test(expected = IllegalArgumentException::class)
    fun `should throw negative precision`() {
        requireSvgPrecision(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw excessive precision`() {
        requireSvgPrecision(100)
    }

}
