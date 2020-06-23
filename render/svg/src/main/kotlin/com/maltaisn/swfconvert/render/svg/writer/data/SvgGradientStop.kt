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

internal data class SvgGradientStop(
    val offset: Float,
    val color: Color,
    val opacity: Float
) {
    init {
        require(offset in 0f..1f) { "Gradient stop offset must be between 0 and 1" }
    }
}

internal fun List<SvgGradientStop>.validateGradientStops() {
    require(this.size >= 2) { "Gradient must have at least 2 stops" }
    require(this.first().offset == 0f) { "First stop offset must be 0." }
    require(this.last().offset == 1f) { "Last stop offset must be 1." }
    var lastOffset = 0f
    for (i in 1 until this.lastIndex) {
        val offset = this[i].offset
        require(offset > lastOffset || offset == 1f) { "Stop offset must be greater than last." }
        lastOffset = offset
    }
}
