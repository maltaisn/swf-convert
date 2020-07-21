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

package com.maltaisn.swfconvert.render.svg.writer

import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

class XmlStreamWriterTest {

    @Test
    fun `should create empty xml`() {
        assertEquals("", createXml {})
    }

    @Test
    fun `should create empty tag`() {
        assertEquals("<tag/>", createXml {
            start("tag")
            end()
        })
    }

    @Test
    fun `should create empty tag DSL`() {
        assertEquals("<tag/>", createXml {
            "tag"()
        })
    }

    @Test
    fun `should create empty tag (pretty)`() {
        assertEquals("<tag/>", createXml(pretty = true) {
            "tag"()
        })
    }

    @Test
    fun `should create self-closing tag with attributes`() {
        assertEquals("""<tag attr1="1" attr2="text"/>""", createXml {
            "tag"(arrayOf("attr1" to 1, "attr2" to "text"))
        })
    }

    @Test
    fun `should create self-closing tag with attributes (pretty)`() {
        assertEquals("""<tag attr1="1" attr2="text"/>""", createXml(pretty = true) {
            "tag"(arrayOf("attr1" to 1, "attr2" to "text"))
        })
    }

    @Test
    fun `should wrap long attribute (pretty)`() {
        assertEquals("""
            |<tag
            |    attr1="${"x".repeat(200)}"
            |    attr2="text" attr3="3"/>
        """.trimMargin().fixLineBreaks(), createXml(pretty = true) {
            "tag"(arrayOf(
                "attr1" to "x".repeat(200),
                "attr2" to "text",
                "attr3" to 3
            ))
        })
    }

    @Test
    fun `should wrap attributes near max line width correctly (pretty)`() {
        // This aims to test the 3 possibilities when wrapping attributes:
        // - No wrap if all attributes fit within line width.
        // - First attribute on first line, wrap others if first fits line width but not others.
        // - All attributes on separate lines if none fit line width.

        lateinit var addTag: XmlStreamWriter.(String, Int) -> Unit
        addTag = { tag, n ->
            if (n > 0) {
                tag(arrayOf("attr1" to "value1", "attr2" to "value2")) {
                    this.addTag(tag, n - 1)
                }
            }
        }

        assertEquals("""
            |<tag attr1="value1" attr2="value2">
            |    <tag attr1="value1"
            |         attr2="value2">
            |        <tag attr1="value1"
            |             attr2="value2">
            |            <tag attr1="value1"
            |                 attr2="value2">
            |                <tag attr1="value1"
            |                     attr2="value2">
            |                    <tag
            |                        attr1="value1"
            |                        attr2="value2"/>
            |                </tag>
            |            </tag>
            |        </tag>
            |    </tag>
            |</tag>
        """.trimMargin().fixLineBreaks(), createXml(pretty = true, maxLineWidth = 36) {
            addTag("tag", 6)
        })
    }

    @Test
    fun `should write namespaces on root tag`() {
        assertEquals("""<tag xmlns="foo" xmlns:ns="bar"/>""", createXml(
            mapOf(null to "foo", "ns" to "bar")) {
            "tag"()
        })
    }

    @Test
    fun `should create self-closing tag with namespaces and attributes`() {
        assertEquals("""<tag xmlns="https://www.example.com" attr1="1" attr2="text"/>""", createXml(
            mapOf(null to "https://www.example.com")) {
            "tag"(arrayOf("attr1" to 1, "attr2" to "text"))
        })
    }

    @Test
    fun `should create self-closing tag with namespaces and namespaced attributes`() {
        assertEquals("""<ns:tag xmlns:ns="https://www.example.com" ns:attr1="1" ns:attr2="text"/>""", createXml(
            mapOf("ns" to "https://www.example.com")) {
            "ns:tag"(arrayOf("ns:attr1" to 1, "ns:attr2" to "text"))
        })
    }

    @Test
    fun `should create empty tag (omit null attributes)`() {
        assertEquals("<tag/>", createXml {
            "tag"(arrayOf("attr1" to null, "attr2" to null))
        })
    }

    @Test
    fun `should create tag children`() {
        assertEquals("<parent><child1/><child2/></parent>", createXml {
            "parent" {
                "child1"()
                "child2"()
            }
        })
    }

    @Test
    fun `should create tag children (pretty)`() {
        assertEquals("""
            |<parent>
            |    <child1/>
            |    <child2/>
            |</parent>
        """.trimMargin().fixLineBreaks(), createXml(pretty = true) {
            "parent" {
                "child1"()
                "child2"()
            }
        })
    }

    @Test
    fun `should create tag children multiple levels`() {
        assertEquals("<level0><level1><level2><level3><level4/>" +
                "</level3></level2></level1></level0>", createXml {
            start("level0")
            start("level1")
            start("level2")
            start("level3")
            start("level4")
            end()
            end()
            end()
            end()
            end()
        })
    }

    @Test
    fun `should create tag children multiple levels DSL`() {
        assertEquals("<level0><level1><level2><level3><level4/>" +
                "</level3></level2></level1></level0>", createXml {
            "level0" {
                "level1" {
                    "level2" {
                        "level3" {
                            "level4"()
                        }
                    }
                }
            }
        })
    }

    @Test
    fun `should create tag children multiple levels (pretty)`() {
        assertEquals("""
            |<level0>
            |    <level1>
            |        <level2>
            |            <level3>
            |                <level4/>
            |            </level3>
            |        </level2>
            |    </level1>
            |</level0>
        """.trimMargin().fixLineBreaks(), createXml(pretty = true) {
            "level0" {
                "level1" {
                    "level2" {
                        "level3" {
                            "level4"()
                        }
                    }
                }
            }
        })
    }

    @Test
    fun `should add text only in tag`() {
        assertEquals("<tag>text</tag>", createXml {
            "tag" {
                text("text")
            }
        })
    }

    @Test
    fun `should add text only in tag (pretty)`() {
        assertEquals("""
            |<tag>
            |    text
            |</tag>
        """.trimMargin().fixLineBreaks(), createXml(pretty = true) {
            "tag" {
                text("text")
            }
        })
    }

    @Test
    fun `should add text and tag in parent tag`() {
        assertEquals("<parent>text<child/></parent>", createXml {
            "parent" {
                text("text")
                "child"()
            }
        })
    }

    @Test
    fun `should add text and tag in parent tag (pretty)`() {
        assertEquals("""
            |<parent>
            |    text
            |    <child/>
            |</parent>
        """.trimMargin().fixLineBreaks(), createXml(pretty = true) {
            "parent" {
                text("text")
                "child"()
            }
        })
    }

    @Test
    fun `should add text with multiple lines (pretty)`() {
        assertEquals("""
            |<parent>
            |    text spanning
            |multiple lines
            |</parent>
        """.trimMargin().fixLineBreaks(), createXml(pretty = true) {
            "parent" {
                text("""
                    |text spanning
                    |multiple lines""".trimMargin().fixLineBreaks())
            }
        })
    }

    @Test
    fun `should close start tag after empty text`() {
        assertEquals("<tag/>", createXml {
            "tag" {
                text("")
            }
        })
    }

    @Test
    fun `should escape chars in text`() {
        assertEquals("<tag>&lt;>&amp;'\"</tag>", createXml {
            "tag" {
                text("<>&'\"")
            }
        })
    }

    @Test
    fun `should escape chars in attributes`() {
        assertEquals("""<tag text="&lt;>&amp;'&quot;"/>""", createXml {
            "tag"(arrayOf("text" to "<>&'\""))
        })
    }

    @Test
    fun `should write prolog`() {
        assertEquals("""<?xml version="1.1" encoding="utf-8"?>""", createXml {
            prolog(arrayOf("version" to "1.1", "encoding" to "utf-8"))
        })
    }

    @Test
    fun `should write prolog and root tag`() {
        assertEquals("""<?xml foo="bar"?><root bar="baz"/>""", createXml {
            prolog(arrayOf("foo" to "bar"))
            "root"(arrayOf("bar" to "baz"))
        })
    }

    @Test
    fun `should write prolog and root tag (pretty)`() {
        assertEquals("""
            |<?xml foo="bar"?>
            |<root bar="baz"/>
        """.trimMargin().fixLineBreaks(), createXml(pretty = true) {
            prolog(arrayOf("foo" to "bar"))
            "root"(arrayOf("bar" to "baz"))
        })
    }

    @Test
    fun `should respect custom indent size`() {
        assertEquals("""
            |<tag1>
            |  <tag2>
            |    <tag3/>
            |  </tag2>
            |</tag1>
        """.trimMargin().fixLineBreaks(), createXml(pretty = true, indentSize = 2) {
            "tag1" {
                "tag2" {
                    "tag3"()
                }
            }
        })
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail to write multiple root tags`() {
        createXml {
            "tag1"()
            "tag2"()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail to write prolog after root tag`() {
        createXml {
            "tag1"()
            prolog()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail empty tag`() {
        createXml {
            ""()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail empty attr name`() {
        createXml {
            "tag"(arrayOf("" to 0))
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail illegal chars in tag`() {
        createXml {
            "<"()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail illegal chars in tag 2`() {
        createXml {
            "\uFFFF"()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail illegal chars in attr`() {
        createXml {
            "tag"(arrayOf("<" to 0))
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail illegal chars in attr 2`() {
        createXml {
            "tag"(arrayOf("\uFFFF" to 0))
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail illegal chars in text`() {
        createXml {
            "tag" {
                text("\uFFFF")
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail multiple colon in tag`() {
        createXml(mapOf("ns" to "_")) {
            "ns:foo:bar"()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail multiple colon in attr`() {
        createXml(mapOf("ns" to "_")) {
            "tag"(arrayOf("ns:foo:bar" to 0))
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail invalid tag namespace`() {
        createXml(mapOf("ns" to "_")) {
            "foo:bar"()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail invalid attr namespace`() {
        createXml(mapOf("ns" to "_")) {
            "tag"(arrayOf("foo:bar" to 0))
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail colon in namespace name`() {
        createXml(mapOf("foo:bar" to "_")) {
            "tag"()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail to close non existent tag`() {
        createXml {
            end()
        }
    }

    private fun createXml(
        namespaces: Map<String?, String> = emptyMap(),
        pretty: Boolean = false,
        maxLineWidth: Int = 128,
        indentSize: Int = 4,
        build: XmlStreamWriter.() -> Unit
    ): String {
        return ByteArrayOutputStream().use { outputStream ->
            XmlStreamWriter(outputStream,
                namespaces,
                pretty,
                maxLineWidth,
                indentSize)
                .apply(build)
                .close()
            outputStream.toString()
        }
    }

    // XML writer uses system separator but this file may use something else.
    private fun String.fixLineBreaks() = this.replace("""
        |
        |
        """.trimMargin(), System.lineSeparator())

}
