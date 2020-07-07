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

package com.maltaisn.swfconvert.convert.context

/**
 * Context for an object in SWF being converted.
 * Object ID is the last of [ids], which form the tree of object IDs leading to this object.
 */
internal class SwfObjectContext(
    parent: SwfFileContext,
    private val ids: List<Int>
) : ConvertContext(parent) {

    override val description: String
        get() = buildString {
            if (ids.isEmpty()) {
                append("root")
            } else {
                append("object ID ")
                append(ids.last())
                if (ids.size > 1) {
                    append(" (${ids.joinToString(" > ")})")
                }
            }
        }
}
