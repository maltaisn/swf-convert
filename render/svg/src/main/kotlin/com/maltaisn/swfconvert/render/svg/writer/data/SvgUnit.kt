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


/**
 * Units that can be used by [SvgNumber].
 * [https://www.w3.org/TR/SVG2/coords.html#Units] and [https://www.w3.org/TR/css-values/#lengths].
 * Most units are provided for completeness but only are actually used.
 */
internal enum class SvgUnit(val symbol: String) {
    USER(""),  // User units, as defined by the viewport.
    PX("px"),
    CM("cm"),
    MM("mm"),
    PT("pt"),
    IN("in"),
    EM("em"),
    PERCENT("%")
}
