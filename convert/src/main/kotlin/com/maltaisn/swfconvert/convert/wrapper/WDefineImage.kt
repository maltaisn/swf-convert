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

import com.flagstone.transform.image.DefineImage
import com.flagstone.transform.image.DefineImage2
import com.maltaisn.swfconvert.convert.zlibDecompress

@Suppress("ArrayInDataClass")
internal class WDefineImage(
    val width: Int,
    val height: Int,
    data: ByteArray,
    val bits: Int,
    val tableSize: Int
) {

    val data = data.zlibDecompress()

    constructor(image: DefineImage) : this(image.width, image.height, image.image, image.pixelSize, image.tableSize)
    constructor(image: DefineImage2) : this(image.width, image.height, image.image, image.pixelSize, image.tableSize)

}
