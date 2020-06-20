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
import com.maltaisn.swfconvert.render.svg.writer.toSvgUrlReference


sealed class SvgFill {
    abstract fun toSvg(): String
    override fun toString() = toSvg()
}

data class SvgFillColor(val color: Color) : SvgFill() {
    override fun toSvg(): String {
        val r = color.r
        val g = color.g
        val b = color.b
        return if (r.isColorComponentFoldable && g.isColorComponentFoldable && b.isColorComponentFoldable) {
            // Can use #xxx shorthand
            String(charArrayOf('#', HEX_CHARS[r and 0xF], HEX_CHARS[g and 0xF], HEX_CHARS[b and 0xF]))
        } else {
            // #xxxxxx
            color.toStringNoAlpha().toLowerCase()
        }
    }
}

data class SvgFillId(val id: String) : SvgFill() {
    override fun toSvg() = id.toSvgUrlReference()
}

object SvgFillNone : SvgFill() {
    override fun toSvg() = "none"
}


/**
 * Returns whether the 4 most significant bits are equal to the 4 least
 * significant bits in the least significant byte of [this] int.
 * Basically this returns `true` for 0x00, 0x11, 0x22, 0x33, 0x44, etc.
 */
private val Int.isColorComponentFoldable: Boolean
    get() = (this and 0xF) == (this ushr 4)

private const val HEX_CHARS = "0123456789abcdef"
