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

package com.maltaisn.swfconvert.render.ir

import com.maltaisn.swfconvert.core.config.Configuration
import com.maltaisn.swfconvert.core.config.FormatConfiguration
import com.maltaisn.swfconvert.core.frame.FramesRenderer
import kotlinx.coroutines.CoroutineScope


/**
 * Configuration for the intermediate representation output format.
 */
data class IrConfiguration(

        /** Whether to pretty print JSON or not. */
        val prettyPrint: Boolean

) : FormatConfiguration<IrConfiguration> {

    override fun createRenderer(coroutineScope: CoroutineScope,
                                config: Configuration): FramesRenderer =
            IrFramesRenderer(coroutineScope, config)

}
