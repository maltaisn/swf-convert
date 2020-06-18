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

import com.maltaisn.swfconvert.render.core.RenderConfiguration
import java.io.File


/**
 * Configuration for the SVG output format.
 */
data class SvgConfiguration(

        override val output: List<File>,
        override val tempDir: File,

        /** Whether to pretty print SVG or not. */
        val prettyPrint: Boolean,

        /** General number precision used for path, position and dimension values. */
        val precision: Int,

        /** Number precision used for scale and shear componenets of transforms. */
        val transformPrecision: Int,

        /** Number precision used for percentage values (gradient stops and opacity). */
        val percentPrecision: Int,

        /** Whether to write XML prolog in SVG documents or not. */
        val writeProlog: Boolean,

        // DEBUG OPTIONS

        override val parallelFrameRendering: Boolean

) : RenderConfiguration
