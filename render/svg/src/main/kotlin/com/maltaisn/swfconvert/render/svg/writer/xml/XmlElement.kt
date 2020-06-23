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

internal interface XmlElement

internal data class XmlProlog(val attrs: Map<String, Any?>) : XmlElement

internal data class XmlText(val text: String) : XmlElement

internal data class XmlTag(
    val name: String,
    val attrs: Map<String, Any?> = mutableMapOf(),
    val children: MutableList<XmlElement> = mutableListOf(),
    val parent: XmlTag? = null
) : XmlWriter(), XmlElement {

    override val currentTag: String
        get() = name

    override fun start(name: String, vararg attrs: Pair<String, *>): XmlWriter {
        val tag = XmlTag(name, attrs.toMap(mutableMapOf()), mutableListOf(), this)
        children += tag
        return tag
    }

    override fun end() = parent

    override fun prolog(vararg attrs: Pair<String, Any?>) {
        children += XmlProlog(attrs.toMap(mutableMapOf()))
    }

    override fun text(text: String) {
        children += XmlText(text)
    }
}
