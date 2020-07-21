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

package com.maltaisn.swfconvert.core

import java.awt.geom.AffineTransform

/**
 * A group object representing the root of a SWF file structure.
 * This is the root element of the intermediate representation.
 *
 * In SWF, all coordinates are in twips (1 / 1440 of an inch) and Y positive goes down.
 * In the intermediate representation, all coordinates are in points (1 / 72 of an inch),
 * and Y positive can go in either direction.
 *
 * @param width Frame width in twips.
 * @param height Frame height in twips.
 * @param padding Padding added around frame in twips.
 */
data class FrameGroup(
    val width: Float,
    val height: Float,
    val padding: Float,
    override val transform: AffineTransform
) :
    GroupObject.Transform(0, transform) {

    // Scaled frame dimensions in points (including padding).
    val actualWidth get() = (width + padding * 2) * Units.TWIPS_TO_POINT
    val actualHeight get() = (height + padding * 2) * Units.TWIPS_TO_POINT

    override fun copyWithoutObjects() = this.copy()

    override fun toString() = "FrameGroup(width=$width, height=$height, " +
            super.toString().substringAfter('(')

    companion object {

        fun create(
            width: Float,
            height: Float,
            padding: Float,
            yAxisDirection: YAxisDirection
        ): FrameGroup {
            val scale = Units.TWIPS_TO_POINT
            val scaleY = when (yAxisDirection) {
                YAxisDirection.DOWN -> 1f
                YAxisDirection.UP -> -1f
            } * scale
            val translateX = padding * scale
            val translateY = (when (yAxisDirection) {
                YAxisDirection.UP -> height
                YAxisDirection.DOWN -> 0f
            } + padding) * scale

            val transform = AffineTransform(scale, 0f, 0f, scaleY, translateX, translateY)
            return FrameGroup(width, height, padding, transform)
        }

    }

}
