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

package com.maltaisn.swfconvert.render.svg

enum class SvgFontsMode(val embedded: Boolean) {

    /** Fonts are stored as external TTF files. */
    EXTERNAL(false),

    /** Fonts are stored as base64 encoded data URLs (TTF format). */
    BASE64(true),

    /** Fonts are not used, text is drawn using paths. */
    NONE(true),
}
