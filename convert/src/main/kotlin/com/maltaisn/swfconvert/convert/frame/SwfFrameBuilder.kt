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
        val frames = createSpriteFrames(swf.objects)
        if (frames.size != header.frameCount) {
            logger.warn {
                "Frames count mismatch between header and content: " +
                        "expected ${header.frameCount}, found ${frames.size}."
            }
        }
        return frames
    }

    /**
     * Create frames for a sprite with a list of display list [tags].
     * If there's no [ShowFrame] tags, the returned list will be empty.
     * @param maxFrames Maximum frames to create.
     */
    private fun createSpriteFrames(tags: Iterable<MovieTag>, maxFrames: Int = Int.MAX_VALUE): List<SwfFrame> {
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
                    if (frames.size >= maxFrames) {
                        return frames
                    }
                }
            }
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
            val spriteFrames = createSpriteFrames(tag.objects, 1)
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
