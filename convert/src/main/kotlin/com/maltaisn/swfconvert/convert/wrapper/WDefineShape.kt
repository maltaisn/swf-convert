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
import com.flagstone.transform.datatype.Bounds
import com.flagstone.transform.fillstyle.FillStyle
import com.flagstone.transform.linestyle.LineStyle
import com.flagstone.transform.shape.DefineShape
import com.flagstone.transform.shape.DefineShape2
import com.flagstone.transform.shape.DefineShape3
import com.flagstone.transform.shape.DefineShape4
import com.flagstone.transform.shape.Shape

internal data class WDefineShape(
    val identifier: Int,
    val shape: Shape,
    val bounds: Bounds,
    val fillStyles: List<FillStyle>,
    val lineStyles: List<LineStyle>
) {

    constructor(s: DefineShape) : this(s.identifier, s.shape, s.bounds, s.fillStyles, s.lineStyles)
    constructor(s: DefineShape2) : this(s.identifier, s.shape, s.bounds, s.fillStyles, s.lineStyles)
    constructor(s: DefineShape3) : this(s.identifier, s.shape, s.bounds, s.fillStyles, s.lineStyles)
    constructor(s: DefineShape4) : this(s.identifier, s.shape, s.bounds, s.fillStyles, s.lineStyles)

}

internal fun MovieTag.toShapeWrapperOrNull() = when (this) {
    is DefineShape -> WDefineShape(this)
    is DefineShape2 -> WDefineShape(this)
    is DefineShape3 -> WDefineShape(this)
    is DefineShape4 -> WDefineShape(this)
    else -> null
}
