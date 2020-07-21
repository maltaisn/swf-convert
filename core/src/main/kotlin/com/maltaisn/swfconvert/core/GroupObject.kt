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

import com.maltaisn.swfconvert.core.shape.Path
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D

/**
 * An object containing other objects in the intermediate representation.
 */
sealed class GroupObject(override val id: Int) : FrameObject {

    val objects = mutableListOf<FrameObject>()

    abstract fun copyWithoutObjects(): GroupObject

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(objects.size)
        sb.appendln(" objects)")
        for (obj in objects) {
            val lines = obj.toString().lines()
            for (line in lines) {
                sb.append("    ")
                sb.appendln(line)
            }
        }
        sb.delete(sb.length - System.lineSeparator().length, sb.length)
        return sb.toString()
    }

    data class Simple(override val id: Int) : GroupObject(id) {
        override fun copyWithoutObjects() = this.copy()
        override fun toString() = "Group[$id](" + super.toString()
    }

    open class Transform(override val id: Int, open val transform: AffineTransform) : GroupObject(id) {
        override fun copyWithoutObjects() = Transform(id, transform)
        override fun toString() = "TransformGroup[$id](transform=$transform, " + super.toString()
    }

    data class Blend(override val id: Int, val blendMode: BlendMode) : GroupObject(id) {
        override fun copyWithoutObjects() = this.copy()
        override fun toString() = "BlendGroup[$id](blend=${blendMode.name}, " + super.toString()
    }

    data class Clip(override val id: Int, val clips: List<Path>) : GroupObject(id) {
        override fun copyWithoutObjects() = this.copy()
        override fun toString() = "ClipGroup[$id](clips=[" +
                clips.joinToString { "'${it.toSvg()}'" } + "], " + super.toString()
    }

    data class Masked(override val id: Int, val bounds: Rectangle2D) : GroupObject(id) {
        override fun copyWithoutObjects() = this.copy()
        override fun toString() = "MaskedGroup[$id](bounds=$bounds, " + super.toString()
    }
}
