/*
 * Copyright (C) 2020 Nicolas Maltais
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
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
