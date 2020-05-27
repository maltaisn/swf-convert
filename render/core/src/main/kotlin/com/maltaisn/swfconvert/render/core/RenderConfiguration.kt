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

package com.maltaisn.swfconvert.render.core

import java.io.File


/**
 * Configuration used to convert from the intermediate representation to an output format.
 */
interface RenderConfiguration {

    /** Output files, size should be 1 or same as input size. */
    val output: List<File>

    /** Directory to which temp and debug files are written. */
    val tempDir: File

    /** Whether to convert input files from intermediate format to output format in parallel. */
    val parallelFrameRendering: Boolean

}
