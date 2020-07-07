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

package com.maltaisn.swfconvert.convert.frame.data

import com.flagstone.transform.DefineTag
import com.maltaisn.swfconvert.convert.context.SwfObjectContext
import com.maltaisn.swfconvert.convert.wrapper.WPlace

internal interface SwfObjectGroup {
    val context: SwfObjectContext
    val id: Int
    val objects: List<SwfFrameObject>
}

internal data class SwfFrame(
    override val context: SwfObjectContext,
    override val id: Int,
    val dictionary: SwfDictionary,
    val width: Int,
    val height: Int,
    override val objects: List<SwfFrameObject>
) : SwfObjectGroup

internal sealed class SwfFrameObject {
    abstract val context: SwfObjectContext
    abstract val id: Int
    abstract val place: WPlace
    abstract val tag: DefineTag
}

internal data class SwfObject(
    override val context: SwfObjectContext,
    override val id: Int,
    override val place: WPlace,
    override val tag: DefineTag
) : SwfFrameObject()

internal data class SwfSprite(
    override val context: SwfObjectContext,
    override val id: Int,
    override val place: WPlace,
    override val tag: DefineTag,
    override val objects: List<SwfFrameObject>
) : SwfFrameObject(), SwfObjectGroup

typealias SwfDictionary = Map<Int, DefineTag>
