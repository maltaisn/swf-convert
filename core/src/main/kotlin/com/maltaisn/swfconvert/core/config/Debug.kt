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

package com.maltaisn.swfconvert.core.config

import com.maltaisn.swfconvert.core.image.data.Color
import com.maltaisn.swfconvert.core.shape.path.PathLineStyle
import java.awt.BasicStroke


/**
 * Debug options
 */
object Debug {

    // PARALLELIZATION SETTINGS

    /** Whether to decode SWF files in parallel. */
    var parallelSwfDecoding = true

    /** Whether to convert input files to intermediate format in parallel. */
    var parallelSwfConversion = true

    /** Whether to rasterize output files in parallel. */
    var parallelFrameRasterization = true

    /** Whether to convert input files from intermediate format to output format in parallel. */
    var parallelFrameRendering = true

    /** Whether to create and optimize output images in parallel. */
    var parallelImageCreation = true


    // DEBUG SETTINGS

    /** Whether to keep the `fonts/` sub-directory after conversion. */
    var keepFonts = false

    /** Whether to keep the `images/` sub-directory after conversion. */
    var keepImages = false

    /** Whether to output glyph images used to run OCR to the `fonts/glyphs/` directory. */
    var outputOcrGlyphs = false

    /** Whether to draw shape bounds to output. */
    var drawShapeBounds = false

    /** Whether to draw text bounds to output. */
    var drawTextBounds = false

    /** Whether to draw clip bounds to output. */
    var drawClipBounds = false

    /** Whether to disable clipping in output. */
    var disableClipping = false

    /** Whether to disable blending (except alpha). */
    var disableBlending = false

    /** Whether to disable masking (alpha blend mode). */
    var disableMasking = false

    /** Padding added around output, in inches. */
    var framePadding = 0f

    /** Linestyle used to draw debug bounds. */
    val linestyle = PathLineStyle(Color(0, 255, 0), 20f,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0f)

}
