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

package com.maltaisn.swfconvert.core

import com.maltaisn.swfconvert.core.image.ImageData
import com.maltaisn.swfconvert.core.shape.PathFillStyle
import com.maltaisn.swfconvert.core.shape.ShapeObject
import com.maltaisn.swfconvert.core.text.Font
import com.maltaisn.swfconvert.core.text.TextObject
import java.io.File

fun GroupObject.findAllImagesTo(dst: MutableList<PathFillStyle.Image>) {
    findAllObjectsOfType<ShapeObject> { shapeObject ->
        shapeObject.paths.mapNotNullTo(dst) { path ->
            path.fillStyle as? PathFillStyle.Image
        }
    }
}

fun GroupObject.findAllImageDataTo(dst: MutableSet<ImageData>) {
    findAllObjectsOfType<ShapeObject> { shapeObject ->
        shapeObject.paths.mapNotNullTo(dst) { path ->
            (path.fillStyle as? PathFillStyle.Image)?.imageData
        }
    }
}

fun GroupObject.findAllFontsTo(dst: MutableSet<Font>) {
    findAllObjectsOfType<TextObject> { textObject ->
        dst += textObject.font
    }
}

fun GroupObject.findAllFontFilesTo(dst: MutableSet<File>) {
    findAllObjectsOfType<TextObject> { textObject ->
        val font = textObject.font.fontFile
        if (font != null) {
            dst += font
        }
    }
}

fun GroupObject.findAllTextObjectsTo(dst: MutableList<TextObject>) {
    findAllObjectsOfType<TextObject> { textObject ->
        dst += textObject
    }
}

/**
 * Iterate over all objects and sub-objects of [this] group, applying a [block] to all objects of type [T].
 */
private inline fun <reified T : FrameObject> GroupObject.findAllObjectsOfType(block: (T) -> Unit) {
    val groupStack = ArrayDeque<GroupObject>()
    groupStack += this
    while (groupStack.isNotEmpty()) {
        for (obj in groupStack.first().objects) {
            if (obj is T) {
                block(obj)
            } else if (obj is GroupObject) {
                groupStack += obj
            }
        }
        groupStack.removeFirst()
    }
}
