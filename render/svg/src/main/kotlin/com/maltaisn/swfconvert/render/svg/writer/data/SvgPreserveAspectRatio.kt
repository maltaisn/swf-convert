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


internal data class SvgPreserveAspectRatio(val align: Align, val slice: Boolean = false) {

    fun toSvg() = align.svgName + if (slice) " slice" else ""

    enum class Align(val svgName: String) {
        NONE("none"),
        X_MIN_Y_MIN("xMinYMin"),
        X_MID_Y_MIN("xMidYMin"),
        X_MAX_Y_MIN("xMaxYMin"),
        X_MIN_Y_MID("xMinYMid"),
        X_MID_Y_MID("xMidYMid"),
        X_MAX_Y_MID("xMaxYMid"),
        X_MIN_Y_MAX("xMinYMax"),
        X_MID_Y_MAX("xMidYMax"),
        X_MAX_Y_MAX("xMaxYMax")
    }

    enum class MeetOrSlice(val svgName: String) {
        MEET("meet"),
        SLICE("slice")
    }

    companion object {
        val NONE = SvgPreserveAspectRatio(Align.NONE)
    }

}
