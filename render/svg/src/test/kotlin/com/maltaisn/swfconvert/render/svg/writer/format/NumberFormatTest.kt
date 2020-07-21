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
