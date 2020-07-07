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

package com.maltaisn.swfconvert.core

/**
 * Blend modes in the intermediate representation, mostly similar to the ones defined by SWF.
 *
 * These blend modes should be easily applied for most output formats, with the exception of:
 * - `ALPHA`: this blend mode acts like a mask which is supported with [GroupObject.Masked].
 * - `LAYER`: sets the opacity of the layer to 100% before blending, limited support.
 * - `INVERT`: inverts color, not really a blend mode. If further support is intended, this shouldn't be a blend mode.
 *
 * An output format should log an error if unsupported blend modes are used.
 */
enum class BlendMode {
    NULL,
    NORMAL,
    LAYER,
    MULTIPLY,
    SCREEN,
    LIGHTEN,
    DARKEN,
    ADD,
    SUBTRACT,
    DIFFERENCE,
    INVERT,
    ERASE,
    OVERLAY,
    HARDLIGHT
}
