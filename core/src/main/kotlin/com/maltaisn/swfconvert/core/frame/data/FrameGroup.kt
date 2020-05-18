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

package com.maltaisn.swfconvert.core.frame.data

import com.maltaisn.swfconvert.core.Units
import java.awt.geom.AffineTransform


/**
 * A group object representing the root of a SWF file structure.
 * This is the root element of the intermediate representation.
 *
 * In SWF, all coordinates are in twips (1 / 1440 of an inch) and Y positive goes down.
 * In the intermediate representation, all coordinates are in points (1 / 72 of an inch),
 * and Y positive goes up.
 *
 * @param width Frame width in twips.
 * @param height Frame height in twips.
 * @param padding Padding added around frame in twips .
 */
data class FrameGroup(val width: Float, val height: Float, val padding: Float) :
        GroupObject.Transform(0, AffineTransform(Units.TWIPS_TO_POINT, 0f, 0f,
                -Units.TWIPS_TO_POINT, padding * Units.TWIPS_TO_POINT,
                (height + padding) * Units.TWIPS_TO_POINT)) {

    // Scaled frame dimensions in points (including padding).
    val actualWidth get() = (width + padding * 2) * Units.TWIPS_TO_POINT
    val actualHeight get() = (height + padding * 2) * Units.TWIPS_TO_POINT


    override fun copyWithoutObjects() = this.copy()

    override fun toString() = "FrameGroup(width=$width, height=$height, " +
            super.toString().substringAfter('(')

}
