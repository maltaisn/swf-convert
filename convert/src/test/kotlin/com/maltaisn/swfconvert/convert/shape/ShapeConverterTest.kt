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

package com.maltaisn.swfconvert.convert.shape

import com.maltaisn.swfconvert.core.shape.PathElement.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class ShapeConverterTest {

    @Test
    fun `should not create rectangle with not enough path elements`() {
        assertNull(ShapeConverter.convertPathToRectangle(listOf(
                MoveTo(0f, 0f),
                LineTo(0f, 1f),
                LineTo(1f, 0f),
                ClosePath
        )))
    }

    @Test
    fun `should not create rectangle with too many path elements`() {
        assertNull(ShapeConverter.convertPathToRectangle(listOf(
                MoveTo(0f, 0f),
                LineTo(0f, 1f),
                LineTo(1f, 2f),
                LineTo(3f, 1f),
                LineTo(3f, 0f),
                ClosePath
        )))
    }

    @Test
    fun `should create rectangle (start from bottom left)`() {
        assertEquals(Rectangle(0f, 0f, 1f, 1f),
                ShapeConverter.convertPathToRectangle(listOf(
                MoveTo(0f, 0f),
                LineTo(0f, 1f),
                LineTo(1f, 1f),
                LineTo(1f, 0f),
                ClosePath
        )))
    }

}
