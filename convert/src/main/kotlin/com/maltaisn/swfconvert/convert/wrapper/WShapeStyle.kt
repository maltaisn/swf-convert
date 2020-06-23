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

import com.flagstone.transform.fillstyle.FillStyle
import com.flagstone.transform.linestyle.LineStyle
import com.flagstone.transform.shape.ShapeStyle
import com.flagstone.transform.shape.ShapeStyle2

internal data class WShapeStyle(
    val moveX: Int?,
    val moveY: Int?,
    val fillStyle0: Int?,
    val fillStyle1: Int?,
    val lineStyle: Int?,
    val fillStyles: List<FillStyle>,
    val lineStyles: List<LineStyle>
) {

    constructor(style: ShapeStyle) : this(style.moveX, style.moveY, style.fillStyle,
        style.altFillStyle, style.lineStyle, style.fillStyles, style.lineStyles)

    constructor(style: ShapeStyle2) : this(style.moveX, style.moveY, style.fillStyle,
        style.altFillStyle, style.lineStyle, style.fillStyles, style.lineStyles)

}
