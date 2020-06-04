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

package com.maltaisn.swfconvert.core.text

import com.maltaisn.swfconvert.core.shape.Path

data class GlyphData(val advanceWidth: Float,
                     val contours: List<Path>) {

    val isWhitespace: Boolean
        get() = contours.isEmpty()


    override fun toString() = buildString {
        append("GlyphData(advance=")
        append(advanceWidth)
        append(", path='")
        for (contour in contours) {
            append(contour.toSvg())
            append(' ')
        }
        if (contours.isNotEmpty()) {
            deleteCharAt(length - 1)
        }
        append("')")
    }


    companion object {
        /**
         * The size of the EM square in the intermediate representation.
         * This value should be equal to the size of the EM square used by doubletype.
         * The glyph contours data and the advance width are defined in EM square space.
         */
        const val EM_SQUARE_SIZE = 1024f

        /** Advance width used for whitespace characters. */
        const val WHITESPACE_ADVANCE_WIDTH = EM_SQUARE_SIZE / 4
    }

}
