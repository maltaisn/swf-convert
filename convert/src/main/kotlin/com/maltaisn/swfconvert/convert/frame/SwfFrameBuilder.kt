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
import com.maltaisn.swfconvert.convert.ConvertConfiguration
import com.maltaisn.swfconvert.convert.context.SwfFileContext
import com.maltaisn.swfconvert.convert.context.SwfObjectContext
import com.maltaisn.swfconvert.convert.conversionError
import com.maltaisn.swfconvert.convert.frame.data.SwfFrame
import com.maltaisn.swfconvert.convert.frame.data.SwfFrameObject
import com.maltaisn.swfconvert.convert.frame.data.SwfObject
import com.maltaisn.swfconvert.convert.frame.data.SwfSprite
import com.maltaisn.swfconvert.convert.wrapper.WPlace
import com.maltaisn.swfconvert.convert.wrapper.toPlaceWrapperOrNull
import com.maltaisn.swfconvert.core.Units
import org.apache.logging.log4j.kotlin.logger
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Creates a list of [SwfFrame] from a [Movie] instance by placing and removing objects from the
 * SWF display list and creating frames when a ShowFrame tag is used.
 */
internal class SwfFrameBuilder @Inject constructor(
    private val config: ConvertConfiguration,
) {

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

        val header = swf.objects.filterIsInstance<MovieHeader>().first()
        if (config.frameSize != null) {
            width = (config.frameSize[0] / Units.TWIPS_TO_INCH).roundToInt()
            height = (config.frameSize[1] / Units.TWIPS_TO_INCH).roundToInt()
        } else {
            // Find frame dimensions
            width = header.frameSize.width
            height = header.frameSize.height
        }

        // Create the frames
        val frames = createFramesForTags(swf.objects, false)
        if (frames.size != header.frameCount && !config.recursiveFrames) {
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
        var hasSpriteFrames = false
        val displayList = DisplayList()
        for (tag in tags) {
            val place = tag.toPlaceWrapperOrNull()
            when {
                place != null -> {
                    val spriteFrames = displayList.addCharacter(place)
                    if (spriteFrames.isNotEmpty()) {  // implies config.recursiveFrames
                        // Create frame at this level for the sprite frames.
                        // This involve copying the current display list and adding the sprite objects on top.
                        for (frame in spriteFrames) {
                            val frameDisplayList = DisplayList(displayList)
                            frameDisplayList.putAll(frame.objects.associateBy { it.place.depth })
                            frames += frameDisplayList.createFrame()
                        }
                        hasSpriteFrames = true
                    }
                }
                tag is Remove -> {
                    val id = displayList.removeCharacter(tag.layer).id
                    if (id != tag.identifier) {
                        // Remove tag has an ID field, not sure what it's used for so let's enforce it.
                        conversionError(swfContext, "Character removed has ID $id, expected ${tag.identifier}.")
                    }
                }
                tag is Remove2 -> displayList.removeCharacter(tag.layer)
                tag is ShowFrame && !hasSpriteFrames -> {
                    frames += displayList.createFrame()
                    if (isSprite && !config.recursiveFrames) {
                        // Not parsing multiple frames in sprites, single frame has been produced so stop here.
                        return frames
                    }
                }
            }
        }

        if (isSprite && displayList.isNotEmpty() && frames.isEmpty()) {
            // Non-empty sprite had no ShowFrame tag, still create a single frame for it.
            frames += displayList.createFrame()
        }

        return frames
    }

    private fun DisplayList.addCharacter(place: WPlace): List<SwfFrame> {
        val id = place.identifier
        val context = SwfObjectContext(swfContext, idStack + id)

        // Only new characters place is supported for now, other could be supported in the future.
        conversionError(place.type == PlaceType.NEW, context) { "Unsupported place type ${place.type}." }
        conversionError(place.depth !in this, context) { "Overwritten character depth" }

        val tag = dictionary[place.identifier]
            ?: conversionError(context, "Unknown character ID $id")

        return if (tag is DefineMovieClip) {
            // DefineSprite.
            val spriteFrames = createFramesForTags(tag.objects, true)
            if (config.recursiveFrames) {
                // Sprite has multiple frames, bring them to parent level.
                spriteFrames
            } else {
                // Single frame, or not parsing frames recursively
                this[place.depth] = SwfSprite(context, id, place, tag, spriteFrames.first().objects)
                emptyList()
            }

        } else {
            // Add the character to the display list.
            this[place.depth] = SwfObject(context, id, place, tag)
            emptyList()
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
