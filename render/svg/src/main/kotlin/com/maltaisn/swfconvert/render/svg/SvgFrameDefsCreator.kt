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

package com.maltaisn.swfconvert.render.svg

import com.maltaisn.swfconvert.core.FrameGroup
import com.maltaisn.swfconvert.core.FrameObject
import com.maltaisn.swfconvert.core.GroupObject
import com.maltaisn.swfconvert.core.findAllTextObjectsTo
import com.maltaisn.swfconvert.core.image.ImageData
import com.maltaisn.swfconvert.core.shape.Path
import com.maltaisn.swfconvert.core.shape.PathFillStyle
import com.maltaisn.swfconvert.core.shape.ShapeObject
import com.maltaisn.swfconvert.core.text.TextObject
import java.io.File
import javax.inject.Inject

/**
 * Class used to assign SVG `<defs>` IDs to all elements that will require one when rendered using [SvgFrameRenderer].
 * Duplicate defs, such as identical glyphs, images, masks, or clip paths are only assigned a single ID.
 */
internal class SvgFrameDefsCreator @Inject constructor(
    private val config: SvgConfiguration
) {

    private val defs = mutableMapOf<FrameDef, String>()

    private val defsByType = mutableMapOf<Class<FrameDef>, Int>()

    /**
     * Create `<defs>` IDs for a [frame].
     */
    fun createFrameDefs(frame: FrameGroup): Map<FrameDef, String> {
        defs.clear()

        if (config.fontsMode == SvgFontsMode.NONE) {
            // If using paths for glyphs instead of fonts, start by creating glyph defs.
            // They will be the most used defs so we might as well give them the shortest IDs.
            createGlyphDefs(frame)
        }

        // Create defs for the frame objects
        createGroupDefs(frame)

        return defs.toMap()
    }

    private fun createObjectDefs(obj: FrameObject) {
        when (obj) {
            is GroupObject -> createGroupDefs(obj)
            is ShapeObject -> createShapeDefs(obj)
            is TextObject -> createTextDefs(obj)
            else -> error("Unknown frame object")
        }
    }

    private fun createGroupDefs(group: GroupObject) {
        // Create def for the group itself
        when (group) {
            is GroupObject.Clip -> {
                if (group.clips.isNotEmpty()) {
                    createDef(FrameDef.ClipDef(group.clips))
                }
            }
            is GroupObject.Masked -> {
                if (group.objects.size >= 2) {
                    createDef(FrameDef.MaskDef(group.objects.last()))
                }
            }
            else -> Unit
        }

        // Create defs for the children objects
        for (obj in group.objects) {
            createObjectDefs(obj)
        }
    }

    private fun createShapeDefs(shape: ShapeObject) {
        for (path in shape.paths) {
            createPathDefs(path)
        }
    }

    private fun createTextDefs(text: TextObject) {
        if (config.fontsMode != SvgFontsMode.NONE) {
            val fontFile = checkNotNull(text.font.fontFile) { "Missing font" }
            createDef(FrameDef.FontDef(fontFile))
        }
    }

    private fun createPathDefs(path: Path) {
        when (val fill = path.fillStyle) {
            is PathFillStyle.Solid -> Unit
            is PathFillStyle.Image -> createImageDefs(path, fill)
            is PathFillStyle.Gradient -> createDef(FrameDef.GradientDef(fill))
        }
    }

    private fun createImageDefs(path: Path, imageFill: PathFillStyle.Image) {
        val imageData = imageFill.imageData

        if (imageFill.clip) {
            createDef(FrameDef.ClipDef(listOf(path)))
        }

        val alphaDataFile = imageData.alphaDataFile
        if (alphaDataFile != null) {
            createDef(FrameDef.ImageMaskDef(alphaDataFile, imageData))
        }

        if (config.imagesMode == SvgImagesMode.BASE64) {
            val imageFile = checkNotNull(imageData.dataFile) { "Missing image" }
            createDef(FrameDef.ImageDef(imageFile, imageData))
        }
    }

    private fun createGlyphDefs(frame: FrameGroup) {
        val allTexts = mutableListOf<TextObject>()
        frame.findAllTextObjectsTo(allTexts)
        for (text in allTexts) {
            val font = text.font
            for (glyphIndex in text.glyphIndices) {
                val glyph = font.glyphs[glyphIndex]
                if (!glyph.isWhitespace) {
                    createDef(FrameDef.GlyphDef(glyph.data.contours))
                }
            }
        }
    }

    private fun createDef(def: FrameDef) {
        if (def !in defs) {
            defs[def] = if (config.prettyPrint) {
                createPrettyDefId(def)
            } else {
                createOptimizedDefId()
            }
        }
    }

    private fun createOptimizedDefId(): String {
        var v = defs.size
        return buildString {
            // See https://www.w3.org/TR/2008/REC-xml-20081126/#NT-Name. XML IDs have different chars
            // valid for the first char and the rest of the ID.
            append(XML_NAME_START_CHARS[v % XML_NAME_START_CHARS.length])
            v /= XML_NAME_START_CHARS.length
            while (v > 0) {
                insert(0, XML_NAME_CHARS[v % XML_NAME_CHARS.length])
                v /= XML_NAME_CHARS.length
            }
        }
    }

    private fun createPrettyDefId(def: FrameDef): String {
        // Create ID matching def type
        val type = def.javaClass
        val count = defsByType[type] ?: 0
        defsByType[type] = count + 1
        return when (def) {
            is FrameDef.FontDef -> "font"
            is FrameDef.ImageDef -> "image"
            is FrameDef.ImageMaskDef -> "image_mask"
            is FrameDef.MaskDef -> "mask"
            is FrameDef.ClipDef -> "clip"
            is FrameDef.GlyphDef -> "glyph"
            is FrameDef.GradientDef -> "gradient"
        } + "_$count"
    }

    companion object {
        private const val XML_NAME_START_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_:"
        private const val XML_NAME_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_:-."
    }
}

sealed class FrameDef {
    data class FontDef(val file: File) : FrameDef()
    data class ImageDef(val file: File, val imageData: ImageData) : FrameDef()
    data class ImageMaskDef(val file: File, val imageData: ImageData) : FrameDef()
    data class MaskDef(val mask: FrameObject) : FrameDef()
    data class ClipDef(val paths: List<Path>) : FrameDef()
    data class GlyphDef(val contours: List<Path>) : FrameDef()
    data class GradientDef(val gradient: PathFillStyle.Gradient) : FrameDef()
}
