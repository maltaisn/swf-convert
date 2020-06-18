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

package com.maltaisn.swfconvert.render.svg.writer.xml



/**
 * Base class for a XML writer.
 */
internal abstract class XmlWriter {

    abstract val currentTag: String

    /**
     * Start a new tag with a [name] and [attrs].
     * The started tag becomes the current tag.
     */
    abstract fun start(name: String, vararg attrs: Pair<String, *>): XmlWriter

    /**
     * End the current tag, returning the name of the ended tag.
     */
    abstract fun end(): XmlWriter?

    /**
     * Write XML prolog with [attrs] at current position.
     */
    abstract fun prolog(vararg attrs: Pair<String, Any?>)

    /**
     * Write text at current position.
     */
    abstract fun text(text: String)

    /**
     * Builder factory function to write a new tag with [this] name and [attrs].
     */
    inline operator fun String.invoke(vararg attrs: Pair<String, *>, @XmlDsl build: XmlWriter.() -> Unit = {}) {
        val writer = start(this, *attrs)
        writer.build()
        end()
    }
}

/**
 * Builder factory function for this XML writer.
 */
inline operator fun <T : XmlWriter> T.invoke(@XmlDsl build: T.() -> Unit) = apply(build)

@DslMarker
internal annotation class XmlDsl
