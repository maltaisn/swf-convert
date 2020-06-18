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

import java.text.DecimalFormat
import java.text.NumberFormat
import kotlin.math.sign


/**
 * A numeric value used in SVG, can be associated with a length unit
 * or be dimensionless with [SvgUnit.USER].
 */
internal data class SvgNumber(val value: Float,
                              val units: SvgUnit = SvgUnit.USER) {

    fun toSvg(nbFmt: NumberFormat) = nbFmt.format(value) + units.symbol

    override fun toString() = toSvg(DecimalFormat.getInstance())

    operator fun compareTo(other: SvgNumber): Int {
        require(units == other.units || value.sign != other.value.sign || value == 0f) {
            "Can't compare numbers with different units"
        }
        return value.compareTo(other.value)
    }

    companion object {
        val ZERO = SvgNumber(0f)
    }

}
