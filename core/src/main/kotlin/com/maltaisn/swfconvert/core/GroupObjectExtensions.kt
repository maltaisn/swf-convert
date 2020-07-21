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
