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

import com.maltaisn.swfconvert.core.image.Color
import com.maltaisn.swfconvert.render.svg.writer.data.SvgFillColor
import com.maltaisn.swfconvert.render.svg.writer.data.SvgFillRule
import com.maltaisn.swfconvert.render.svg.writer.data.SvgGraphicsState
import com.maltaisn.swfconvert.render.svg.writer.data.SvgMixBlendMode
import com.maltaisn.swfconvert.render.svg.writer.data.SvgNumber
import com.maltaisn.swfconvert.render.svg.writer.data.SvgStrokeLineCap
import com.maltaisn.swfconvert.render.svg.writer.data.SvgTransform
import org.junit.Test
import java.awt.geom.Rectangle2D
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

class SvgStreamWriterTest {

    @Test
    fun `should create empty svg`() {
        assertEquals("""
            |<?xml version="1.1" encoding="UTF-8"?><svg xmlns="http://www.w3.org/2000/svg"
            | xmlns:xlink="http://www.w3.org/1999/xlink" version="2.0" width="10" height="10"/>
        """.trimMargin().asSingleLine(),
            createSvg {
                start(SvgNumber(10f), SvgNumber(10f))
            })
    }

    @Test
    fun `should svg with viewbox and graphics state attrs`() {
        assertEquals("""
                |<?xml version="1.1" encoding="UTF-8"?><svg xmlns="http://www.w3.org/2000/svg"
                | xmlns:xlink="http://www.w3.org/1999/xlink" version="2.0" width="10" height="10"
                | viewBox="0 0 100 100" fill="#fa0000"/>
            """.trimMargin().asSingleLine(),
            createSvg {
                start(SvgNumber(10f), SvgNumber(10f), Rectangle2D.Float(0f, 0f, 100f, 100f), true,
                    SvgGraphicsState(fill = SvgFillColor(Color(250, 0, 0))))
            })
    }

    @Test
    fun `should write defs block`() {
        assertEquals("""<defs/>""",
            createSvg(start = true) {
                writeDefs()
            }.onlySvgContent())
    }

    @Test
    fun `should create group def`() {
        assertEquals("""<g id="foo"/>""",
            createSvg(start = true) {
                writeDef("foo") {
                    group()
                }
            }.onlySvgContent())
    }

    @Test(expected = IllegalStateException::class)
    fun `should fail to create nested defs`() {
        createSvg(start = true) {
            writeDef("def1") {
                group {
                    writeDef("def2") {
                        group()
                    }
                }
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should throw error if no defs written`() {
        createSvg(start = true) {
            writeDef("foo") {}
        }
    }

    @Test
    fun `should write def without id`() {
        assertEquals("""<g/>""",
            createSvg(start = true) {
                writeDef(null) {
                    group()
                }
            }.onlySvgContent())
    }

    @Test(expected = IllegalStateException::class)
    fun `should throw error if multiple defs written`() {
        createSvg(start = true) {
            writeDef("foo") {
                path("")
                path("")
            }
        }
    }

    @Test
    fun `should only write modified graphics state attributes`() {
        assertEquals("""
                    |<g fill="#fa0000"><g fill-opacity=".5"><g stroke-linecap="round">
                    |<g style="mix-blend-mode:multiply"/></g></g></g>
                """.trimMargin().asSingleLine(),
            createSvg(start = true) {
                group(SvgGraphicsState(fill = SvgFillColor(Color(250, 0, 0)))) {
                    group(SvgGraphicsState(fill = SvgFillColor(Color(250, 0, 0)), fillOpacity = 0.5f)) {
                        group(SvgGraphicsState(strokeLineCap = SvgStrokeLineCap.ROUND)) {
                            group(SvgGraphicsState(strokeLineCap = SvgStrokeLineCap.ROUND,
                                mixBlendMode = SvgMixBlendMode.MULTIPLY))
                        }
                    }
                }
            }.onlySvgContent())
    }

    @Test
    fun `should write same transforms twice`() {
        assertEquals("""<g transform="translate(10 10)"><g transform="translate(10 10)"/></g>""",
            createSvg(start = true) {
                group(SvgGraphicsState(transform = listOf(SvgTransform.Translate(10f, 10f)))) {
                    group(SvgGraphicsState(transform = listOf(SvgTransform.Translate(10f, 10f))))
                }
            }.onlySvgContent())
    }

    @Test
    fun `should not discard empty group`() {
        assertEquals("<g><g/></g>",
            createSvg(start = true) {
                group {
                    group(discardIfEmpty = false)
                }
            }.onlySvgContent())
    }

    @Test
    fun `should discard empty groups`() {
        assertEquals("",
            createSvg(start = true) {
                group(discardIfEmpty = true) {
                    group(discardIfEmpty = true)
                }
            }.onlySvgContent())
    }

    @Test
    fun `should create clip path and use it`() {
        assertEquals("""
                |<clipPath id="clip1"><path d="M5 5h5v5h-5Z"/></clipPath>
                |<path d="M0 0h20v20h-20Z" clip-path="url(#clip1)" clip-rule="evenodd"/>
            """.trimMargin().asSingleLine(),
            createSvg(start = true) {
                writeDef("clip1") {
                    clipPath {
                        path("M5 5h5v5h-5Z")
                    }
                }
                path("M0 0h20v20h-20Z", SvgGraphicsState(clipPathId = "clip1",
                    clipRule = SvgFillRule.EVEN_ODD))
            }.onlySvgContent())
    }

    @Test
    fun `should write mask and use it`() {
        assertEquals("""
                |<mask id="mask1"><path d="M5 5h5v5h-5Z"/></mask>
                |<path d="M0 0h20v20h-20Z" mask="url(#mask1)"/>
            """.trimMargin().asSingleLine(),
            createSvg(start = true) {
                writeDef("mask1") {
                    mask {
                        path("M5 5h5v5h-5Z")
                    }
                }
                path("M0 0h20v20h-20Z", SvgGraphicsState(maskId = "mask1"))
            }.onlySvgContent())
    }

    @Test
    fun `should write text`() {
        assertEquals("""<text font-family="foo" font-size="32" x="100" y="100" dx="1 2 3 4">Text</text>""",
            createSvg(start = true) {
                text("Text", floatArrayOf(1f, 2f, 3f, 4f), SvgGraphicsState(
                    x = SvgNumber(100f),
                    y = SvgNumber(100f),
                    fontFamily = "foo",
                    fontSize = 32f))
            }.onlySvgContent())
    }

    private fun createSvg(start: Boolean = false, build: SvgStreamWriter.() -> Unit): String {
        return ByteArrayOutputStream().use { outputStream ->
            SvgStreamWriter(outputStream, 1, 2, 2, false)
                .apply {
                    if (start) {
                        start(SvgNumber(10f), SvgNumber(10f))
                    }
                    build()
                    end()
                }
            outputStream.toString()
        }
    }

    private fun String.onlySvgContent() = """<svg .*?>([\s\S]*)</svg>$""".toRegex()
        .find(this)?.groupValues?.get(1) ?: ""

    private fun String.asSingleLine() = this.replace("""
        |
        |
        """.trimMargin(), "")

}
