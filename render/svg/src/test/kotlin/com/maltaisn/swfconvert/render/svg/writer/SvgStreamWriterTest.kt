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

import com.maltaisn.swfconvert.core.image.Color
import com.maltaisn.swfconvert.render.svg.writer.data.SvgClipPathRule
import com.maltaisn.swfconvert.render.svg.writer.data.SvgGraphicsState
import com.maltaisn.swfconvert.render.svg.writer.data.SvgMixBlendMode
import com.maltaisn.swfconvert.render.svg.writer.data.SvgStrokeLineCap
import org.junit.Test
import java.awt.geom.Rectangle2D
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals


class SvgStreamWriterTest {

    @Test
    fun `should create empty svg`() {
        assertEquals("""<?xml version="1.1" encoding="UTF-8"?><svg xmlns="http://www.w3.org/2000/svg" version="2.0" width="10" height="10"/>""",
                createSvg {
                    start(10f, 10f)
                })
    }

    @Test
    fun `should svg with viewbox and graphics state attrs`() {
        assertEquals("""<?xml version="1.1" encoding="UTF-8"?><svg xmlns="http://www.w3.org/2000/svg" """ +
                """version="2.0" width="10" height="10" viewBox="0 0 100 100" fill="#FF0000"/>""",
                createSvg {
                    start(10f, 10f, Rectangle2D.Float(0f, 0f, 100f, 100f),
                            SvgGraphicsState(fillColor = Color(255, 0, 0)))
                })
    }

    @Test
    fun `should create group def`() {
        assertEquals("""<defs><g id="foo"/></defs>""",
                createSvg {
                    start(10f, 10f)
                    writeDef("foo") {
                        startGroup()
                        endGroup()
                    }
                }.onlySvgContent())
    }

    @Test(expected = IllegalStateException::class)
    fun `should throw error if no defs written`() {
        createSvg {
            start(10f, 10f)
            writeDef("foo") {}
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `should throw error if multiple defs written`() {
        createSvg {
            start(10f, 10f)
            writeDef("foo") {
                path("")
                path("")
            }
        }
    }

    @Test
    fun `should only write modified graphics state attributes`() {
        assertEquals("""<g fill="#FF0000"><g fill-opacity="0.5"><g stroke-linecap="round">""" +
                """<g style="mix-blend-mode:multiply;isolation:isolate;"/></g></g></g>""",
                createSvg {
                    start(10f, 10f)
                    group(SvgGraphicsState(fillColor = Color(255, 0, 0))) {
                        group(SvgGraphicsState(fillColor = Color(255, 0, 0, 127))) {
                            group(SvgGraphicsState(strokeLineCap = SvgStrokeLineCap.ROUND)) {
                                group(SvgGraphicsState(strokeLineCap = SvgStrokeLineCap.ROUND,
                                        mixBlendMode = SvgMixBlendMode.MULTIPLY))
                            }
                        }
                    }
                }.onlySvgContent())
    }

    @Test
    fun `should create clip path and use it`() {
        assertEquals("""<path d="M0 0h20v20h-20Z" clip-path="url(#clip1)" clip-rule="evenodd"/>""" +
                """<defs><clipPath id="clip1"><path d="M5 5h5v5h-5Z"/></clipPath></defs>""",
                createSvg {
                    start(10f, 10f)
                    writeDef("clip1") {
                        clipPath {
                            path("M5 5h5v5h-5Z")
                        }
                    }
                    path("M0 0h20v20h-20Z", SvgGraphicsState(clipPathId = "clip1",
                            clipPathRule = SvgClipPathRule.EVEN_ODD))
                }.onlySvgContent())
    }

    @Test
    fun `should write mask and use it`() {
        assertEquals("""<path d="M0 0h20v20h-20Z" mask="url(#mask1)"/>""" +
                """<defs><mask id="mask1"><path d="M5 5h5v5h-5Z"/></mask></defs>""",
                createSvg {
                    start(10f, 10f)
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
        assertEquals("""<text x="100" y="100" dx="1 2 3 4" font-family="foo">Text</text>""",
                createSvg {
                    start(10f, 10f)
                    text(100f, 100f, floatArrayOf(1f, 2f, 3f, 4f), "foo", "Text")
                }.onlySvgContent())
    }


    private fun createSvg(build: SvgStreamWriter.() -> Unit): String {
        return ByteArrayOutputStream().use { outputStream ->
            SvgStreamWriter(outputStream)
                    .apply(build)
                    .apply { end() }
            outputStream.toString()
        }
    }

    private fun String.onlySvgContent() = """<svg .*?>([\s\S]*)</svg>$""".toRegex()
            .find(this)?.groupValues?.get(1) ?: error("No SVG found")

}
