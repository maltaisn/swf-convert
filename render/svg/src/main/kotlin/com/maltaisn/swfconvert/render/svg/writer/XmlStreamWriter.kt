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

import java.io.Closeable
import java.io.Flushable
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*


/**
 * A class to write XML to an [outputStream]. XML is written as it's built, there's no
 * DOM being created in memory. Also provides a DSL to create XML.
 * Note that the writer must be [flushed][flush] manually after writing is done.
 *
 * @property namespaces Namespace declarations added to root tag and that will be accepted in XML.
 * @property prettyPrint Whether to pretty print XML or not.
 */
class XmlStreamWriter(outputStream: OutputStream,
                      private val namespaces: Map<String?, String> = emptyMap(),
                      private val prettyPrint: Boolean = false) : Closeable, Flushable {

    private val writer = OutputStreamWriter(outputStream).buffered()

    /** Whether current position is currently on root or not. */
    val isRoot: Boolean
        get() = currentLevel == 0

    /** Current tag level, `0` for root. */
    val currentLevel: Int
        get() = tagStack.size

    /** Stack of tags leading to current position. */
    private val tagStack = LinkedList<Tag>()

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

    /**
     * Write XML prolog with [attrs]
     */
    fun prolog(vararg attrs: Pair<String, Any?>) {
        check(!hasRootTag) { "Prolog must be the first XML element" }

        write("<")
        write(XML_PROLOG)
        writeAttributes(XML_PROLOG, *attrs)
        write("?>")
        writeLine()
    }

    /**
     * Start a new tag with a [name] and [attrs].
     * The started tag becomes the current tag.
     */
    fun start(name: String,
              vararg attrs: Pair<String, *>) {
        check(!isRoot || !hasRootTag) { "There must be a single root tag" }
        name.checkXmlName()

        // If last write was a start tag, close it.
        // At this point we know that the tag won't be self-closing since it has this child tag.
        closeStartTagIfNeeded()

        writeStartTag(name, *attrs)

        if (isRoot) {
            // Currently on root, mark root tag as written.
            hasRootTag = true
        }
        tagStack.push(Tag(name, false))
        indentLevel += PRETTY_PRINT_INDENT_SIZE
    }

    /**
     * End the current tag, returning the name of the ended tag.
     */
    fun end(): String {
        check(!isRoot) { "No tag started." }

        indentLevel -= PRETTY_PRINT_INDENT_SIZE

        val tag = tagStack.pop()
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

        return tag.name
    }

    /**
     * Write text at current position.
     */
    fun text(text: String) {
        if (text.isEmpty()) {
            return
        }
        check(ILLEGAL_CHARS_REGEX !in text) { "Illegal chars in text" }

        raw(text
                .replace("&", "&amp;")
                .replace("<", "&lt;"))
    }

    /**
     * Write raw XML data at current position.
     */
    fun raw(xml: String) {
        closeStartTagIfNeeded()
        writeIndent()
        write(xml)
        writeLine()
    }

    // DSL

    /**
     * [build] XML using this writer.
     */
    inline operator fun invoke(@XmlDsl build: XmlStreamWriter.() -> Unit) = apply(build)

    /**
     * Write a XML tag with [attrs] and [build] its children.
     */
    inline operator fun String.invoke(vararg attrs: Pair<String, *>,
                               @XmlDsl build: XmlStreamWriter.() -> Unit = {}) {
        start(this, *attrs)
        this@XmlStreamWriter.build()
        end()
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

    private fun writeStartTag(name: String, vararg attrs: Pair<String, *>) {
        writeIndent()
        write("<")
        write(name)

        // Write attributes with special indent
        if (isRoot) {
            // Root tag, write namespace attributes.
            writeNamespaceAttributes(name)
        }
        writeAttributes(name, *attrs)
        attrIndentLevel = 0
    }

    private fun closeStartTagIfNeeded() {
        // If last write was a start tag, close it.
        val tag = tagStack.peek()
        if (tag?.hasChildren == false) {
            write(">")
            writeLine()
            tag.hasChildren = true
        }
    }

    private fun writeAttributes(tagName: String, vararg attrs: Pair<String, *>) {
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
            NUMBER_FMT.format(value)  // For floating point, omit '.0'.
        } else {
            value.toString()
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace("\"", "&quot;")
        }

        val lineWidthAfter = currentLineWidth + name.length + valueStr.length + 3

        if (prettyPrint && attrIndentLevel == 0) {
            // Attribute indentation level wasn't set yet. (first attribute)
            attrIndentLevel = if (lineWidthAfter >= PRETTY_PRINT_LINE_LIMIT) {
                // First attribute will be on next line, use default indent.
                PRETTY_PRINT_INDENT_SIZE
            } else {
                // First attribute will be on same line, align others with first.
                // +2 chars for '<' and the space separating the tag and the first attribute.
                tagName.length + 2
            }
        }

        // Write indent/whitespace
        if (prettyPrint && lineWidthAfter >= PRETTY_PRINT_LINE_LIMIT) {
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
        check(ILLEGAL_CHARS_REGEX !in this
                && INVALID_NAME_CHARS_REGEX !in this) { "Invalid chars in name" }
        if (':' in this) {
            check(this.substringBefore(':') in namespaces) { "Unknown attribute namespace" }
            check(this.count { it == ':' } == 1) { "Multiple colons in name" }
        }
    }


    data class Tag(val name: String,
                   var hasChildren: Boolean)


    companion object {
        private const val XML_PROLOG = "?xml"

        private val ILLEGAL_CHARS_REGEX = """[^\u0001-\ud7ff\ue000-\ufffd]""".toRegex()
        private val INVALID_NAME_CHARS_REGEX = """[<>&'"\v]""".toRegex()

        private const val PRETTY_PRINT_INDENT_SIZE = 4
        private const val PRETTY_PRINT_LINE_LIMIT = 128

        private val NUMBER_FMT = DecimalFormat().apply {
            isGroupingUsed = false
            decimalFormatSymbols = DecimalFormatSymbols().apply {
                decimalSeparator = '.'
            }
        }
    }

}

@DslMarker
private annotation class XmlDsl
