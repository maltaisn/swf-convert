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

package com.maltaisn.swfconvert.convert.wrapper

import com.flagstone.transform.MovieTag
import com.flagstone.transform.Place
import com.flagstone.transform.Place2
import com.flagstone.transform.Place3
import com.flagstone.transform.PlaceType
import com.flagstone.transform.datatype.Blend
import com.flagstone.transform.datatype.ColorTransform
import com.flagstone.transform.datatype.CoordTransform
import com.flagstone.transform.filter.Filter

internal data class WPlace(
    val type: PlaceType,
    val transform: CoordTransform?,
    var colorTransform: ColorTransform?,
    val filters: List<Filter>,
    val identifier: Int,
    val depth: Int,
    val clipDepth: Int,
    val blendMode: Blend?,
    val ratio: Int?
) {

    val hasClip: Boolean
        get() = clipDepth != NO_CLIP_DEPTH

    constructor(place: Place) : this(PlaceType.NEW, place.transform, place.colorTransform,
        emptyList(), place.identifier, place.layer, NO_CLIP_DEPTH, null, null)

    constructor(place: Place2) : this(place.type, place.transform, place.colorTransform,
        emptyList(), place.identifier, place.layer, place.depth ?: NO_CLIP_DEPTH, null, place.ratio)

    constructor(place: Place3) : this(place.type, place.transform, place.colorTransform,
        place.filters, place.identifier, place.layer, place.depth ?: NO_CLIP_DEPTH, place.blend, place.ratio)

    companion object {
        const val NO_CLIP_DEPTH = 0
    }
}

internal fun MovieTag.toPlaceWrapperOrNull() = when (this) {
    is Place -> WPlace(this)
    is Place2 -> WPlace(this)
    is Place3 -> WPlace(this)
    else -> null
}
