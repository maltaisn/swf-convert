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

package com.maltaisn.swfconvert.convert.frame

import com.flagstone.transform.*
import com.flagstone.transform.datatype.Blend
import com.flagstone.transform.datatype.CoordTransform
import com.flagstone.transform.filter.ColorMatrixFilter
import com.flagstone.transform.movieclip.DefineMovieClip
import com.flagstone.transform.shape.DefineShape
import com.flagstone.transform.shape.DefineShape2
import com.flagstone.transform.shape.DefineShape3
import com.flagstone.transform.shape.DefineShape4
import com.flagstone.transform.text.StaticTextTag
import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.convert.context.SwfFileContext
import com.maltaisn.swfconvert.convert.context.SwfObjectContext
import com.maltaisn.swfconvert.convert.conversionError
import com.maltaisn.swfconvert.convert.image.CompositeColorTransform
import com.maltaisn.swfconvert.convert.shape.StyledShapeConverter
import com.maltaisn.swfconvert.convert.toAffineTransformOrIdentity
import com.maltaisn.swfconvert.convert.wrapper.WDefineShape
import com.maltaisn.swfconvert.convert.wrapper.WPlace
import com.maltaisn.swfconvert.core.*
import com.maltaisn.swfconvert.core.shape.Path
import com.maltaisn.swfconvert.core.shape.PathElement
import com.maltaisn.swfconvert.core.shape.ShapeObject
import com.maltaisn.swfconvert.core.text.Font
import com.maltaisn.swfconvert.core.text.FontId
import org.apache.logging.log4j.kotlin.logger
import java.awt.Rectangle
import java.awt.geom.AffineTransform
import java.util.*
import javax.inject.Inject


/**
 * Converts a single SWF file to the [FrameGroup] intermediate representation.
 */
internal class SwfConverter @Inject constructor(
        private val config: ConvertConfiguration,
        private val textConverter: TextConverter,
        private val shapeParser: StyledShapeConverter
) : Disposable {

    private val logger = logger()

    private lateinit var context: SwfFileContext
    private lateinit var fontsMap: Map<FontId, Font>

    private val groupStack = LinkedList<GroupObject>()
    private val currentGroup: GroupObject
        get() = groupStack.first

    private var objectsMap: Map<Int, DefineTag> = emptyMap()
    private val colorTransform = CompositeColorTransform()

    private val idStack = LinkedList<Int>()
    private val blendStack = LinkedList<BlendMode>()
    private val clipStack = LinkedList<Int>()
    private val transformStack = LinkedList<AffineTransform>()


    fun createFrameGroup(context: SwfFileContext, swf: Movie,
                         fontsMap: Map<FontId, Font>): FrameGroup {
        this.context = context
        this.fontsMap = fontsMap

        objectsMap = swf.objects.filterIsInstance<DefineTag>().associateBy { it.identifier }

        colorTransform.transforms.clear()
        groupStack.clear()
        clipStack.clear()

        blendStack.clear()
        blendStack.push(BlendMode.NORMAL)

        shapeParser.initialize(objectsMap, colorTransform)

        // Create frame group
        val movieHeader = swf.objects.find { it is MovieHeader } as MovieHeader
        val frameGroup = FrameGroup.create(movieHeader.frameSize.width.toFloat(),
                movieHeader.frameSize.height.toFloat(),
                config.framePadding / Units.TWIPS_TO_INCH,
                config.yAxisDirection)

        // Create root frame
        groupStack.push(frameGroup)
        transformStack.push(frameGroup.transform)

        val frameTags = mutableListOf<MovieTag>()
        for (obj in swf.objects) {
            when (obj) {
                is Place, is Place2, is Place3, is ShowFrame -> {
                    frameTags += obj
                }
                is Export -> {
                    for (id in obj.objects.keys) {
                        frameTags += Place(id, frameTags.size + 1,
                                CoordTransform.translate(0, 0))
                    }
                }
            }
        }
        val rootFrameContext = SwfObjectContext(context, listOf(0))
        createFrame(rootFrameContext, frameTags)

        conversionError( currentGroup === frameGroup, context) {
            "Expected only frame group in group stack"
        }
        return frameGroup
    }

    private fun createFrame(frameContext: SwfObjectContext, frameTags: List<MovieTag>) {
        //conversionError(frameTags.last() is ShowFrame) { "Frame last object must be ShowFrame" }

        // Draw frame objects
        var lastDepth = 0
        for (frameTag in frameTags) {
            val placeTag = frameTag.toPlaceTagOrNull()
            if (placeTag != null && placeTag.ratio == null) {
                // Remove tags and frames with ratios are ignored.
                conversionError(placeTag.type == PlaceType.NEW, frameContext) { "Unsupported place tag type" }

                // Check if tags are sorted by depth
                conversionError(placeTag.depth >= lastDepth, frameContext) { "Frame objects not sorted by depth" }
                lastDepth = placeTag.depth

                // Place object
                idStack.addLast(placeTag.identifier)
                val objContext = SwfObjectContext(context, idStack.toList())
                val obj = objectsMap[placeTag.identifier] ?: conversionError(objContext, "Invalid object ID")
                createObject(objContext, placeTag, obj)
                idStack.removeLast()
            }
        }
    }

    private fun createObject(objContext: SwfObjectContext, placeTag: WPlace, objTag: DefineTag) {
        var groupsBefore = groupStack.size
        val id = objTag.identifier

        val wshape = objTag.toShapeTagOrNull()
        val transform = placeTag.transform.toAffineTransformOrIdentity()

        // Check if all filters are supported
        for (filter in placeTag.filters) {
            if (filter !is ColorMatrixFilter
                    || filter.matrix?.contentEquals(IDENTITY_COLOR_MATRIX) == false) {
                conversionError(objContext, "Unsupported place filter $filter")
            }
        }

        // Color transform
        val colorTransform = placeTag.colorTransform
        if (colorTransform != null) {
            this.colorTransform.transforms.push(colorTransform)
        }

        // Blend mode / masked group
        val blendMode = placeTag.blendMode
        var blendModeChanged = false
        if (blendMode != null) {
            if (blendMode == Blend.ALPHA) {
                if (!config.disableMasking) {
                    if (wshape == null) {
                        // Using text as soft mask is unsupported for example.
                        conversionError(objContext, "Unsupported mask object ${objTag.javaClass.simpleName}" )
                    }

                    // Find mask object bounds
                    val boundsRect = Rectangle(wshape.bounds.minX, wshape.bounds.minY,
                            wshape.bounds.width, wshape.bounds.height)
                    val bounds = transform.createTransformedShape(boundsRect).bounds2D

                    // Replace last group with a masked group. Current object will be used as mask.
                    val group = currentGroup
                    val maskedGroup = GroupObject.Masked(group.id, bounds)
                    maskedGroup.objects += group.objects
                    group.objects.clear()
                    if (group is GroupObject.Blend) {
                        // If current group is blend mode, this is the blend mode that will be used to
                        // draw objects to mask. But it will hide previous blend mode so it has to be ignored.
                        groupStack.pop()
                        currentGroup.objects -= group
                    }
                    addGroup(maskedGroup)
                }

            } else if (config.disableBlending) {
                // Blending disabled, add no-op group.
                addGroup(GroupObject.Simple(id))

            } else {
                val blend = when (blendMode) {
                    Blend.LAYER -> BlendMode.NORMAL
                    Blend.MULTIPLY -> BlendMode.MULTIPLY
                    Blend.LIGHTEN -> BlendMode.LIGHTEN
                    Blend.DARKEN -> BlendMode.DARKEN
                    Blend.HARDLIGHT -> BlendMode.HARD_LIGHT
                    Blend.SCREEN -> BlendMode.SCREEN  // Not exactly the same?
                    Blend.OVERLAY -> BlendMode.OVERLAY  // Not exactly the same?
                    else -> {
                        conversionError(objContext, "Unsupported blend mode ${blendMode.name}")
                    }
                }
                if (blend != blendStack.peek()) {
                    addGroup(GroupObject.Blend(id, blend))
                    blendStack.push(blend)
                    blendModeChanged = true
                }
            }
        }

        // Transform
        if (!transform.isIdentity && !placeTag.hasClip) {
            // If object is a clipping shape, transform will be pre-applied on path later.
            addGroup(GroupObject.Transform(id, transform))

            val newTransform = AffineTransform(transformStack.peek())
            newTransform.preConcatenate(transform)
            transformStack.push(newTransform)
        }

        // Create object
        when {
            wshape != null -> {
                createShape(objContext, wshape, placeTag)
                if (placeTag.hasClip) {
                    // To avoid removing clip group when restoring group stack.
                    groupsBefore++
                }
            }
            objTag is StaticTextTag -> createText(objContext, objTag)
            objTag is DefineMovieClip -> {
                if (groupStack.size == groupsBefore) {
                    // No special groups added for frame, add simple group. This is important for
                    // when masked groups are added, because they mask the content of the last frame,
                    // and a frame without special blending or transform is still a frame.
                    addGroup(GroupObject.Simple(id))
                }
                createFrame(objContext, objTag.objects)
            }
            else -> {
                // Unsupported types are ignored.
                logger.error { "Unsupported object type ${objTag.javaClass.simpleName} ($objContext)" }
            }
        }

        // Restore the group stack
        while (groupStack.size > groupsBefore) {
            val group = groupStack.pop()

            if (group is GroupObject.Transform) {
                transformStack.pop()
            }

            // If group has no objects, remove it from parent.
            if (group.objects.isEmpty()) {
                currentGroup.objects.removeAt(currentGroup.objects.lastIndex)
            }
        }

        // Restore previous blend mode
        if (blendModeChanged) {
            blendStack.pop()
        }

        // Unclip all shapes with clip depth lower or equal than current depth.
        while (clipStack.isNotEmpty() && placeTag.depth >= clipStack.peek()) {
            clipStack.pop()

            if (groupStack.peek() !is GroupObject.Clip) {
                conversionError(objContext, "Expected clip group")
                // TODO: clips are per frame, not per file.
            }

            val group = groupStack.pop()

            // If clip group has no objects, remove it from parent.
            if (group.objects.isEmpty()) {
                currentGroup.objects.removeAt(currentGroup.objects.lastIndex)
            }
        }

        // Remove color transform
        if (colorTransform != null) {
            this.colorTransform.transforms.pop()
        }
    }

    private fun createShape(objContext: SwfObjectContext, shapeTag: WDefineShape, placeTag: WPlace) {
        // If the shape is for clipping, the transform must be pre-applied because it only applies 
        // to the clipping shape while the clip applies to many objects.
        val transform = placeTag.transform.takeIf { placeTag.hasClip }.toAffineTransformOrIdentity()

        // Parse the shape into paths and images.
        val paths = shapeParser.parseShape(objContext, shapeTag.shape,
                shapeTag.fillStyles, shapeTag.lineStyles, transform,
                transformStack.peek(), placeTag.hasClip, true)
        val shapeObject = ShapeObject(shapeTag.identifier, paths)

        if (placeTag.hasClip) {
            // Clip shape.
            // Clips cannot be interlaced in intermediate representation, although SWF supports it.
            // Also, empty clips (i.e with no paths), must be kept since they represent a frame
            // and masked group only applies to last frame.
            conversionError(clipStack.isEmpty() || placeTag.clipDepth <= clipStack.peek(), objContext) {
                "Unsupported interlaced clips"
            }

            clipStack.push(placeTag.clipDepth)
            addGroup(GroupObject.Clip(shapeTag.identifier,
                    if (config.disableClipping) {
                        emptyList()
                    } else {
                        shapeObject.paths.toSet().toList()
                    }))

        } else if (paths.isNotEmpty()) {
            // Add debug shape bounds
            if (config.drawShapeBounds) {
                val bounds = shapeTag.bounds
                currentGroup.objects += ShapeObject(shapeTag.identifier,
                        listOf(Path(listOf(PathElement.Rectangle(
                                bounds.minX.toFloat(), bounds.minY.toFloat(),
                                bounds.width.toFloat(), bounds.height.toFloat())),
                                lineStyle = config.debugLineStyle)))
            }

            // Add shape to current group.
            currentGroup.objects += shapeObject
        }
    }

    private fun createText(objContext: SwfObjectContext, textTag: StaticTextTag) {
        currentGroup.objects += textConverter.parseText(objContext, textTag, colorTransform, fontsMap)
    }

    private fun addGroup(group: GroupObject) {
        currentGroup.objects += group
        groupStack.push(group)
    }

    private fun MovieTag.toPlaceTagOrNull() = when (this) {
        is Place -> WPlace(this)
        is Place2 -> WPlace(this)
        is Place3 -> WPlace(this)
        else -> null
    }

    private fun MovieTag.toShapeTagOrNull() = when (this) {
        is DefineShape -> WDefineShape(this)
        is DefineShape2 -> WDefineShape(this)
        is DefineShape3 -> WDefineShape(this)
        is DefineShape4 -> WDefineShape(this)
        else -> null
    }

    override fun dispose() {
        shapeParser.dispose()
    }

    companion object {
        private val IDENTITY_COLOR_MATRIX = floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f)
    }

}
