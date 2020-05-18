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

import com.maltaisn.swfconvert.core.frame.BlendMode
import com.maltaisn.swfconvert.core.shape.path.Path
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D


/**
 * An object containing other objects in the intermediate representation.
 */
sealed class GroupObject(override val id: Int) : FrameObject(id) {

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
