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

package com.maltaisn.swfconvert.render.svg.writer

import com.maltaisn.swfconvert.render.svg.writer.format.createNumberFormat
import java.io.Closeable
import java.io.Flushable
import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * A class to write XML to an [outputStream]. XML is written as it's built, there's no
 * DOM being created in memory. Also provides a DSL to create XML.
 * Note that the writer must be [flushed][flush] manually after writing is done.
 *
 * @property namespaces Namespace declarations added to root tag and that will be accepted in XML.
 * @property prettyPrint Whether to pretty print XML or not.
 */
internal class XmlStreamWriter(
    outputStream: OutputStream,
    private val namespaces: Map<String?, String> = emptyMap(),
    private val prettyPrint: Boolean = false,
    private val maxLineWidth: Int = 128,
    private val indentSize: Int = 4
) : Closeable, Flushable {

    private val writer = OutputStreamWriter(outputStream).buffered()
    private val numberFmt = createNumberFormat()

    /** Whether current position is currently on root or not. */
    val isRoot: Boolean
        get() = currentLevel == 0

    /** Current tag level, `0` for root. */
    val currentLevel: Int
        get() = tagStack.size

    /** Stack of tags leading to current position. */
    private val tagStack = ArrayDeque<Tag>()

    val currentTag: String?
        get() = tagStack.lastOrNull()?.name

    /** Whether writer has written the root tag or not. */
    private var hasRootTag = false

    /** Current indentation level in number of spaces. */
    private var indentLevel = 0

    /** Current indentation level for tag attributes. */
    private var attrIndentLevel = 0

    /** Number of chars in current line. */
    private var currentLineWidth = 0

    override fun flush() {
        writer.flush()
    }

    override fun close() {
        flush()
        writer.close()
    }

    fun start(name: String, attrs: AttributesArray = emptyArray()) {
        check(!isRoot || !hasRootTag) { "There must be a single root tag" }
        name.checkXmlName()

        // If last write was a start tag, close it.
        // At this point we know that the tag won't be self-closing since it has this child tag.
        closeStartTagIfNeeded()

        writeStartTag(name, attrs)

        if (isRoot) {
            // Currently on root, mark root tag as written.
            hasRootTag = true
        }
        tagStack += Tag(name, false)
        indentLevel += indentSize
    }

    fun end() {
        check(!isRoot) { "No tag started." }

        indentLevel -= indentSize

        val tag = tagStack.removeLast()
        if (tag.hasChildren) {
            writeIndent()
            write("</")
            write(tag.name)
            write(">")
        } else {
            write("/>")
        }
        if (!isRoot) {
            writeLine()
        }
    }

    /**
     * Builder factory function to write a new tag with [this] name and [attrs].
     */
    inline operator fun String.invoke(
        attrs: AttributesArray = emptyArray(),
        @XmlDsl build: XmlStreamWriter.() -> Unit = {}
    ) {
        start(this, attrs)
        this@XmlStreamWriter.build()
        end()
    }

    fun prolog(attrs: AttributesArray = emptyArray()) {
        check(!hasRootTag) { "Prolog must be the first XML element" }

        write("<")
        write(XML_PROLOG)
        writeAttributes(XML_PROLOG, attrs)
        attrIndentLevel = 0
        write("?>")
        writeLine()
    }

    fun text(text: String) {
        if (text.isEmpty()) {
            return
        }
        check(ILLEGAL_CHARS_REGEX !in text) { "Illegal chars in text" }

        closeStartTagIfNeeded()
        writeIndent()
        write(text.replace("&", "&amp;")
            .replace("<", "&lt;"))
        writeLine()
    }

    private fun <K, V> Map<K, V>.toTypedArray(): Array<Pair<K, V>> {
        val arr = arrayOfNulls<Pair<K, V>>(this.size)
        for ((i, entry) in this.entries.withIndex()) {
            arr[i] = entry.key to entry.value
        }
        return arr.requireNoNulls()
    }

    private fun write(str: String) {
        writer.write(str)
        currentLineWidth += str.length
    }

    private fun writeLine() {
        if (prettyPrint) {
            writer.write(System.lineSeparator())
            currentLineWidth = 0
        }
    }

    private fun writeIndent() {
        if (prettyPrint) {
            write(" ".repeat(indentLevel + attrIndentLevel))
        }
    }

    private fun writeStartTag(name: String, attrs: Array<out Pair<String, *>>) {
        writeIndent()
        write("<")
        write(name)

        // Write attributes with special indent
        if (isRoot) {
            // Root tag, write namespace attributes.
            writeNamespaceAttributes(name)
        }
        writeAttributes(name, attrs)
        attrIndentLevel = 0
    }

    private fun closeStartTagIfNeeded() {
        // If last write was a start tag, close it.
        val tag = tagStack.lastOrNull()
        if (tag?.hasChildren == false) {
            write(">")
            writeLine()
            tag.hasChildren = true
        }
    }

    private fun writeAttributes(tagName: String, attrs: Array<out Pair<String, *>>) {
        for ((name, value) in attrs) {
            if (value != null) {
                name.checkXmlName()
                writeAttribute(tagName, name, value)
            }
        }
    }

    private fun writeNamespaceAttributes(tagName: String) {
        for ((namespace, value) in namespaces) {
            check(namespace == null || ':' !in namespace) { "Invalid colon in namespace name" }
            check(value.isNotEmpty()) { "Invalid empty namespace" }
            if (namespace == null) {
                writeAttribute(tagName, "xmlns", value)
            } else {
                writeAttribute(tagName, "xmlns:$namespace", value)
            }
        }
    }

    private fun writeAttribute(tagName: String, name: String, value: Any) {
        val valueStr = if (value is Number) {
            numberFmt.format(value) // For floating point, omit '.0'.
        } else {
            value.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace("\"", "&quot;")
        }

        val lineWidthAfter = currentLineWidth + name.length + valueStr.length + "=\"\"".length

        if (prettyPrint && attrIndentLevel == 0) {
            // Attribute indentation level wasn't set yet. (first attribute)
            attrIndentLevel = if (lineWidthAfter >= maxLineWidth) {
                // First attribute will be on next line, use default indent.
                indentSize
            } else {
                // First attribute will be on same line, align others with first.
                tagName.length + "< ".length
            }
        }

        // Write indent/whitespace
        if (prettyPrint && lineWidthAfter >= maxLineWidth) {
            writeLine()
            writeIndent()
        } else {
            write(" ")
        }

        // Write attribute
        write(name)
        write("=\"")
        write(valueStr)
        write("\"")
    }

    private fun String.checkXmlName() {
        check(this.isNotEmpty()) { "Invalid empty name" }
        check(ILLEGAL_CHARS_REGEX !in this &&
                INVALID_NAME_CHARS_REGEX !in this) { "Invalid chars in name" }
        if (':' in this) {
            check(this.substringBefore(':') in namespaces) { "Unknown attribute namespace" }
            check(this.count { it == ':' } == 1) { "Multiple colons in name" }
        }
    }

    data class Tag(
        val name: String,
        var hasChildren: Boolean
    )

    companion object {
        private const val XML_PROLOG = "?xml"

        private val ILLEGAL_CHARS_REGEX = """[^\u0001-\ud7ff\ue000-\ufffd]""".toRegex()
        private val INVALID_NAME_CHARS_REGEX = """[<>&'"\v]""".toRegex()
    }

}

/**
 * Builder factory function for this XML writer.
 */
internal inline operator fun XmlStreamWriter.invoke(@XmlDsl build: XmlStreamWriter.() -> Unit) = run(build)

@DslMarker
internal annotation class XmlDsl

internal typealias AttributesArray = Array<out Pair<String, Any?>>
