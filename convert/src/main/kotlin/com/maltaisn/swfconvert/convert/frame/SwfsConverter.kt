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

package com.maltaisn.swfconvert.convert.frame

import com.flagstone.transform.Movie
import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.convert.context.ConvertContext
import com.maltaisn.swfconvert.convert.context.SwfFileContext
import com.maltaisn.swfconvert.core.FrameGroup
import com.maltaisn.swfconvert.core.ProgressCallback
import com.maltaisn.swfconvert.core.mapInParallel
import com.maltaisn.swfconvert.core.showProgress
import com.maltaisn.swfconvert.core.showStep
import com.maltaisn.swfconvert.core.text.Font
import com.maltaisn.swfconvert.core.text.FontId
import com.maltaisn.swfconvert.core.use
import javax.inject.Inject
import javax.inject.Provider

/**
 * Converts a collection of SWF files to the [FrameGroup] intermediate representation.
 */
internal class SwfsConverter @Inject constructor(
    private val config: ConvertConfiguration,
    private val progressCb: ProgressCallback,
    private val swfConverterProvider: Provider<SwfConverter>
) {

    suspend fun createFrameGroups(
        context: ConvertContext,
        swfs: List<Movie>,
        fontsMap: Map<FontId, Font>
    ): List<FrameGroup> {
        return progressCb.showStep("Converting SWFs", true) {
            progressCb.showProgress(swfs.size) {
                swfs.withIndex().mapInParallel(
                    config.parallelSwfConversion) { (i, swf) ->
                    val swfContext = SwfFileContext(context, config.input[i], i)
                    val frameGroup = swfConverterProvider.get().use {
                        it.createFrameGroup(swfContext, swf, fontsMap)
                    }
                    progressCb.incrementProgress()
                    frameGroup
                }
            }
        }
    }

}
