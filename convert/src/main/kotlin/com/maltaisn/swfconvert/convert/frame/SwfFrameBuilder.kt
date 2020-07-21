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

package com.maltaisn.swfconvert.convert.frame

import com.flagstone.transform.DefineTag
import com.flagstone.transform.Movie
import com.flagstone.transform.MovieHeader
import com.flagstone.transform.MovieTag
import com.flagstone.transform.PlaceType
import com.flagstone.transform.Remove
import com.flagstone.transform.Remove2
import com.flagstone.transform.ShowFrame
import com.flagstone.transform.movieclip.DefineMovieClip
import com.maltaisn.swfconvert.convert.context.SwfFileContext
import com.maltaisn.swfconvert.convert.context.SwfObjectContext
import com.maltaisn.swfconvert.convert.conversionError
import com.maltaisn.swfconvert.convert.frame.data.SwfFrame
import com.maltaisn.swfconvert.convert.frame.data.SwfFrameObject
import com.maltaisn.swfconvert.convert.frame.data.SwfObject
import com.maltaisn.swfconvert.convert.frame.data.SwfSprite
import com.maltaisn.swfconvert.convert.wrapper.WPlace
import com.maltaisn.swfconvert.convert.wrapper.toPlaceWrapperOrNull
import org.apache.logging.log4j.kotlin.logger
import javax.inject.Inject

/**
 * Creates a list of [SwfFrame] from a [Movie] instance by placing and removing objects from the
 * SWF display list and creating frames when a ShowFrame tag is used.
 */
internal class SwfFrameBuilder @Inject constructor() {

    private val logger = logger()

    private lateinit var swfContext: SwfFileContext

    /**
     * The SWF object dictionary where each object is mapped to its ID.
     * Objects without an ID aren't present in this dictionary.
     */
    private var dictionary = emptyMap<Int, DefineTag>()

    /**
     * A stack of character IDs leading to the current object.
     * This is used only for debugging purpose, i.e. creating contexts.
     */
    private val idStack = ArrayDeque<Int>()

    private var width = 0
    private var height = 0

    fun createFrames(swf: Movie, context: SwfFileContext): List<SwfFrame> {
        swfContext = context

        // Create the object dictionary
        dictionary = swf.objects
            .asSequence()
            .filterIsInstance<DefineTag>()
            .associateBy { it.identifier }

        // Find frame dimensions
        val header = swf.objects.filterIsInstance<MovieHeader>().first()
        width = header.frameSize.width
        height = header.frameSize.height

        // Create the frames
        val frames = createFramesForTags(swf.objects, false)
        if (frames.size != header.frameCount) {
            logger.warn {
                "Frames count mismatch between header and content: " +
                        "expected ${header.frameCount}, found ${frames.size}."
            }
        }
        return frames
    }

    /**
     * Create frames for a list of display list [tags], returning a frame for each [ShowFrame].
     * @param isSprite Whether creating a sprite. In this case, if there's at least one character
     * on the display list, a frame will be created even if there's no [ShowFrame] tag.
     * A maximum of one frame is also created.
     */
    private fun createFramesForTags(tags: List<MovieTag>, isSprite: Boolean): List<SwfFrame> {
        val frames = mutableListOf<SwfFrame>()
        val displayList = DisplayList()
        for (tag in tags) {
            val place = tag.toPlaceWrapperOrNull()
            when {
                place != null -> displayList.addCharacter(place)
                tag is Remove -> {
                    val id = displayList.removeCharacter(tag.layer).id
                    if (id != tag.identifier) {
                        // Remove tag has an ID field, not sure what it's used for so let's enforce it.
                        conversionError(swfContext, "Character removed has ID $id, expected ${tag.identifier}.")
                    }
                }
                tag is Remove2 -> displayList.removeCharacter(tag.layer)
                tag is ShowFrame -> {
                    frames += displayList.createFrame()
                    if (isSprite) {
                        // Sprite could have many frames but that wouldn't make sense in our static context.
                        return frames
                    }
                }
            }
        }

        if (isSprite && displayList.isNotEmpty()) {
            // Non-empty sprite had no ShowFrame tag, still create a single frame for it.
            frames += displayList.createFrame()
        }

        return frames
    }

    private fun DisplayList.addCharacter(place: WPlace) {
        val id = place.identifier
        val context = SwfObjectContext(swfContext, idStack + id)

        // Only new characters place is supported for now, other could be supported in the future.
        conversionError(place.type == PlaceType.NEW, context) { "Unsupported place type ${place.type}." }
        conversionError(place.depth !in this, context) { "Overwritten character depth" }

        val tag = dictionary[place.identifier]
            ?: conversionError(context, "Unknown character ID $id")

        this[place.depth] = if (tag is DefineMovieClip) {
            // DefineSprite.
            val spriteFrames = createFramesForTags(tag.objects, true)
            if (spriteFrames.size > 1) {
                // Multiple frames in a sprite don't really make sense in this context.
                logger.warn { "Found ${spriteFrames.size} frames in sprite at $context, using only first." }
            }
            val spriteFrame = spriteFrames.firstOrNull() ?: return
            SwfSprite(context, id, place, tag, spriteFrame.objects)

        } else {
            // Add the character to the display list.
            SwfObject(context, id, place, tag)
        }
    }

    private fun DisplayList.removeCharacter(depth: Int) = remove(depth)
            ?: conversionError(swfContext, "No object to remove at depth $depth.")

    private fun DisplayList.createFrame(): SwfFrame {
        // Create frame from the current display list.
        val context = SwfObjectContext(swfContext, idStack.toList())
        return SwfFrame(context, idStack.lastOrNull() ?: 0, dictionary, width, height, this.values.toList())
    }
}

/**
 * A SWF display list, which is actually a map of characters by depth.
 */
private typealias DisplayList = LinkedHashMap<Int, SwfFrameObject>
