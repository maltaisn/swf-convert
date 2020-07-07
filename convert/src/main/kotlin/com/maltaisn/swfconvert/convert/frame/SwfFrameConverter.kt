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

import com.flagstone.transform.datatype.Blend
import com.flagstone.transform.filter.ColorMatrixFilter
import com.flagstone.transform.text.StaticTextTag
import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.convert.context.ConvertContext
import com.maltaisn.swfconvert.convert.conversionError
import com.maltaisn.swfconvert.convert.font.FontsMap
import com.maltaisn.swfconvert.convert.frame.data.SwfFrame
import com.maltaisn.swfconvert.convert.frame.data.SwfFrameObject
import com.maltaisn.swfconvert.convert.frame.data.SwfObjectGroup
import com.maltaisn.swfconvert.convert.frame.data.SwfSprite
import com.maltaisn.swfconvert.convert.image.CompositeColorTransform
import com.maltaisn.swfconvert.convert.shape.StyledShapeConverter
import com.maltaisn.swfconvert.convert.toAffineTransformOrIdentity
import com.maltaisn.swfconvert.convert.toBlendMode
import com.maltaisn.swfconvert.convert.wrapper.WDefineShape
import com.maltaisn.swfconvert.convert.wrapper.toShapeWrapperOrNull
import com.maltaisn.swfconvert.core.BlendMode
import com.maltaisn.swfconvert.core.Disposable
import com.maltaisn.swfconvert.core.FrameGroup
import com.maltaisn.swfconvert.core.GroupObject
import com.maltaisn.swfconvert.core.Units
import com.maltaisn.swfconvert.core.shape.Path
import com.maltaisn.swfconvert.core.shape.PathElement
import com.maltaisn.swfconvert.core.shape.ShapeObject
import org.apache.logging.log4j.kotlin.logger
import java.awt.Rectangle
import java.awt.geom.AffineTransform
import javax.inject.Inject

/**
 * Converts a single SWF frame to a [FrameGroup] (the intermediate representation).
 */
internal class SwfFrameConverter @Inject constructor(
    private val config: ConvertConfiguration,
    private val textConverter: TextConverter,
    private val shapeParser: StyledShapeConverter
) : Disposable {

    private val logger = logger()

    private lateinit var frame: SwfFrame
    private lateinit var fontsMap: FontsMap

    private val groupStack = ArrayDeque<GroupObject>()
    private val transformStack = ArrayDeque<AffineTransform>()
    private val blendStack = ArrayDeque<BlendMode>()
    private val clipStack = ArrayDeque<Int>()
    private val colorTransform = CompositeColorTransform()

    private val currentGroup: GroupObject
        get() = groupStack.last()

    /**
     * Create a [FrameGroup] from a [SwfFrame].
     */
    fun createFrameGroup(frame: SwfFrame, fontsMap: FontsMap): FrameGroup {
        this.frame = frame
        this.fontsMap = fontsMap

        val frameGroup = FrameGroup.create(frame.width.toFloat(), frame.height.toFloat(),
            config.framePadding / Units.TWIPS_TO_INCH, config.yAxisDirection)

        // Initialize state
        resetState()
        groupStack += frameGroup
        transformStack += frameGroup.transform
        blendStack += BlendMode.NORMAL
        shapeParser.initialize(frame.dictionary, colorTransform)

        createGroup(frame)

        conversionError(currentGroup === frameGroup, frame.context) {
            "Expected only frame group in group stack"
        }

        return frameGroup
    }

    private fun resetState() {
        groupStack.clear()
        transformStack.clear()
        blendStack.clear()
        clipStack.clear()
        colorTransform.clear()
    }

    /**
     * Create a group of objects, either a frame or a sprite.
     */
    private fun createGroup(group: SwfObjectGroup) {
        // Add simple group for each sprite. This is important for when masked groups are added,
        // this group helps to identity the content to mask.
        val groupObject = GroupObject.Simple(group.id)
        addGroup(groupObject)

        // Clear the clip stack for this group. Although this is unclear from the reference, it seems from test data
        // that depth is per sprite, not per SWF file. This implies that clips can only be removed from the group
        // they defined in and not from a child sprite for example.
        val clipsBefore = clipStack.toList()
        clipStack.clear()

        // Create objects in group
        for (obj in group.objects) {
            createObject(obj)
        }

        // Restore previous clip stack.
        conversionError(clipStack.isEmpty(), group.context) { "Expected empty child clip stack" }
        clipStack += clipsBefore

        // Restore group stack
        groupStack.removeLast()
    }

    /**
     * Create an object (a character), applying the attributes of the place tag used to display it.
     */
    private fun createObject(obj: SwfFrameObject) {
        var groupsCountBefore = groupStack.size

        // Update state according to place tag attributes
        checkPlaceFilters(obj)
        val colorTransformChanged = applyPlaceColorTransform(obj)
        val blendModeChanged = applyPlaceBlendMode(obj)
        val transformChanged = applyPlaceTransfrom(obj)

        // Create object
        val tag = obj.tag
        val wshape = tag.toShapeWrapperOrNull()
        when {
            wshape != null -> if (obj.place.hasClip) {
                val createdClipGroup = createClipGroup(obj, wshape)
                if (createdClipGroup) {
                    // Increment the number of groups to avoid removing clip group when restoring group stack afterwards.
                    // Clip group should be removed when its clip depth is exceeded.
                    groupsCountBefore++
                }
            } else {
                createShape(obj, wshape)
            }
            tag is StaticTextTag -> createText(obj, tag)
            obj is SwfSprite -> createGroup(obj)
            else -> {
                // Unsupported types are ignored.
                logger.error { "Unsupported object type ${tag.javaClass.simpleName} at ${obj.context}" }
            }
        }

        // Restore previous state
        if (transformChanged) transformStack.removeLast()
        if (colorTransformChanged) this.colorTransform.pop()
        if (blendModeChanged) blendStack.removeLast()

        restoreGroupStack(groupsCountBefore)
        restoreClipStack(obj.place.depth, obj.context)
    }

    private fun checkPlaceFilters(obj: SwfFrameObject) {
        for (filter in obj.place.filters) {
            when (filter) {
                is ColorMatrixFilter -> {
                    if (filter.matrix?.contentEquals(IDENTITY_COLOR_MATRIX) == false) {
                        // Unsupported non-identity color matrix
                        // Note that color matrix could have easily been integrated with [CompositeColorTransform].
                        // However, I had no test data to check if it worked correctly.
                        conversionError(obj.context, "Unsupported non-identity color matrix filter $filter")
                    }
                }
                else -> {
                    // Other filters would be harder to implement.
                    // ClipEventFlags could technically be ignored though.
                    conversionError(obj.context, "Unsupported place filter $filter")
                }
            }
        }
    }

    private fun applyPlaceTransfrom(obj: SwfFrameObject): Boolean {
        val transform = obj.place.transform.toAffineTransformOrIdentity()
        return if (!transform.isIdentity && !obj.place.hasClip) {
            // If object is a clipping shape, transform will be pre-applied on path later.
            // Otherwise the transform will be applied on whole the clip group (in other words on all clipped objects)
            // whereas it should only apply to the clipping shape.
            addGroup(GroupObject.Transform(obj.id, transform))

            val newTransform = AffineTransform(transformStack.last())
            newTransform.preConcatenate(transform)
            transformStack += newTransform
            true
        } else {
            false
        }
    }

    private fun applyPlaceColorTransform(obj: SwfFrameObject): Boolean {
        val colorTransform = obj.place.colorTransform
        return if (colorTransform != null) {
            this.colorTransform.push(colorTransform)
            true
        } else {
            false
        }
    }

    private fun applyPlaceBlendMode(obj: SwfFrameObject): Boolean =
        when (obj.place.blendMode) {
            null -> false
            Blend.ALPHA -> {
                applyPlaceAlphaBlendMode(obj)
                false
            }
            else -> applyPlaceNormalBlendMode(obj)
        }

    private fun applyPlaceAlphaBlendMode(obj: SwfFrameObject) {
        if (config.disableMasking) return

        val shape = obj.tag.toShapeWrapperOrNull()
        // Mask must be a shape. Using text as soft mask is unsupported for example.
            ?: conversionError(obj.context, "Unsupported mask object of type ${obj.tag.javaClass.simpleName}")

        // Find mask object bounds and apply total transform on it.
        // Bounds are needed for PDF. Page bounds could have been used but smaller bounds should improve performance.
        val boundsRect = Rectangle(shape.bounds.minX, shape.bounds.minY, shape.bounds.width, shape.bounds.height)
        val transform = obj.place.transform.toAffineTransformOrIdentity()
        val bounds = transform.createTransformedShape(boundsRect).bounds2D

        // Alpha blend mode applies the transparency of the current layer to the underlying layer.
        // The current layer hasn't been created yet. The underlying layer starts with the last sprite group, find it.
        while (currentGroup !is GroupObject.Simple) {
            groupStack.removeLast()
        }
        val group = currentGroup

        // Put the objects of the underlying layer in the masked group
        val maskedGroup = GroupObject.Masked(group.id, bounds)
        maskedGroup.objects += group.objects
        group.objects.clear()
        addGroup(maskedGroup)
    }

    private fun applyPlaceNormalBlendMode(obj: SwfFrameObject): Boolean {
        val blend = obj.place.blendMode?.toBlendMode() ?: return false
        return if (blend != blendStack.last()) {
            addGroup(GroupObject.Blend(obj.id, blend))
            blendStack += blend
            true
        } else {
            false
        }
    }

    private fun createShape(obj: SwfFrameObject, shape: WDefineShape) {
        // Parse the shape into paths and images.
        val paths = shapeParser.parseShape(
            context = obj.context,
            shape = shape.shape,
            fillStyles = shape.fillStyles,
            lineStyles = shape.lineStyles,
            currentTransform = transformStack.last()
        ).ifEmpty { return }

        // Add debug shape bounds
        if (config.drawShapeBounds) {
            val bounds = shape.bounds
            currentGroup.objects += ShapeObject(shape.identifier,
                listOf(Path(listOf(PathElement.Rectangle(
                    bounds.minX.toFloat(), bounds.minY.toFloat(),
                    bounds.width.toFloat(), bounds.height.toFloat())),
                    lineStyle = config.debugLineStyle)))
        }

        // Add shape to current group.
        currentGroup.objects += ShapeObject(shape.identifier, paths)
    }

    private fun createClipGroup(obj: SwfFrameObject, shape: WDefineShape): Boolean {
        if (config.disableClipping) return false

        val place = obj.place

        val transform = place.transform.toAffineTransformOrIdentity()

        // Parse the shape into paths and images.
        // Only distinct paths are kept, duplicate clips are meaningless.
        // Styles are ignored, they are also meaningless for clipping.
        val paths = shapeParser.parseShape(
            context = obj.context,
            shape = shape.shape,
            fillStyles = shape.fillStyles,
            lineStyles = shape.lineStyles,
            transform = transform,
            currentTransform = transformStack.last(),
            ignoreStyles = true
        ).distinct().ifEmpty { return false }

        conversionError(clipStack.isEmpty() || place.clipDepth <= clipStack.last(), obj.context) {
            // Clips cannot be interlaced in intermediate representation, although SWF supports it.
            // It would be pretty hard to implement in output formats anyway, requiring path union logic.
            "Unsupported interlaced clips: current clip ends at depth ${place.clipDepth}, " +
                    "after last clip which ends at depth ${clipStack.last()}"
        }

        clipStack += place.clipDepth
        addGroup(GroupObject.Clip(shape.identifier, paths))
        return true
    }

    private fun createText(obj: SwfFrameObject, text: StaticTextTag) {
        currentGroup.objects += textConverter.createTextObjects(obj.context, text, colorTransform, fontsMap)
    }

    private fun addGroup(group: GroupObject) {
        currentGroup.objects += group
        groupStack += group
    }

    private fun restoreGroupStack(groupsCountBefore: Int) {
        while (groupStack.size > groupsCountBefore) {
            val group = groupStack.removeLast()
            if (group.objects.isEmpty()) {
                // If group has no objects, remove it from parent.
                currentGroup.objects.removeAt(currentGroup.objects.lastIndex)
            }
        }
    }

    private fun restoreClipStack(currentDepth: Int, context: ConvertContext) {
        while (clipStack.isNotEmpty() && currentDepth >= clipStack.last()) {
            clipStack.removeLast()
            val group = groupStack.removeLast()
            conversionError(group is GroupObject.Clip, context) { "Expected clip group" }
            if (group.objects.isEmpty()) {
                // If clip group has no objects, remove it from parent.
                currentGroup.objects.removeAt(currentGroup.objects.lastIndex)
            }
        }
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
