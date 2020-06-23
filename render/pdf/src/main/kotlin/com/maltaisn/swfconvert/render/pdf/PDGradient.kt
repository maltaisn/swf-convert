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

package com.maltaisn.swfconvert.render.pdf

import com.maltaisn.swfconvert.core.image.Color
import org.apache.pdfbox.cos.COSArray
import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.cos.COSFloat
import org.apache.pdfbox.cos.COSInteger
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.common.PDRange
import org.apache.pdfbox.pdmodel.common.function.PDFunction
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType2
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType3
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2

internal class PDGradient(parts: List<GradientPart>) : PDShadingType2(COSDictionary()) {

    init {
        // PDF 1.7 - 8.7.4.5.3 Type 2 (Axial) Shadings
        colorSpace = PDDeviceRGB.INSTANCE
        shadingType = SHADING_TYPE2
        function = createGradientFunction(parts)
    }

    private fun createGradientFunction(parts: List<GradientPart>): PDFunction {
        require(parts.size >= 2) { "Gradient must have at least 2 colors." }

        val first = parts.first()
        require(first.ratio == 0f) { "Gradient first color ratio must be 0." }
        val last = parts.last()
        require(last.ratio == 1f) { "Gradient last color ratio must be 1." }

        if (parts.size == 2) {
            // Only two colors, use exponential function.
            return createColorFunction(first.color, last.color)
        }

        // Multiple colors, use stitching function to combine exponential functions
        // PDF 1.7 - 7.10.4 Type 3 (Stitching) Functions
        val dict = COSDictionary()
        val functions = COSArray()
        val bounds = COSArray()
        val encode = COSArray()
        var lastPart = first
        for (i in 1 until parts.size) {
            val part = parts[i]

            // Add exponential function for interpolating between these two colors.
            functions.add(createColorFunction(lastPart.color, part.color))

            // Specify function bounds, except for first and last, which are specified by domain.
            if (i != parts.lastIndex) {
                bounds.add(COSFloat(part.ratio))
            }

            // Used to interpolate stitching function subdomain (eg: [0.2 0.5] 
            // to the exponential function domain, which is always [0.0 1.0].
            encode.add(COSInteger.ZERO)
            encode.add(COSInteger.ONE)
            lastPart = part
        }
        dict.setInt(COSName.FUNCTION_TYPE, STITCHING_FUNC_TYPE)
        dict.setItem(COSName.DOMAIN, PDRange()) // [0.0 1.0]
        dict.setItem(COSName.FUNCTIONS, functions)
        dict.setItem(COSName.BOUNDS, bounds)
        dict.setItem(COSName.ENCODE, encode)
        return PDFunctionType3(dict)
    }

    private fun createColorFunction(start: Color, end: Color): PDFunction {
        // PDF 1.7 - 7.10.3 Type 2 (Exponential Interpolation) Functions
        val dict = COSDictionary()
        dict.setInt(COSName.FUNCTION_TYPE, 2)
        dict.setItem(COSName.DOMAIN, PDRange()) // [0.0 1.0]
        dict.setItem(COSName.C0, createColorCOSArray(start))
        dict.setItem(COSName.C1, createColorCOSArray(end))
        dict.setInt(COSName.N, 1) // Linear interpolation
        return PDFunctionType2(dict)
    }

    private fun createColorCOSArray(color: Color): COSArray {
        // Create a COSArray for a color. 
        // Color class uses RGB (0-255) but PDF uses RGB (0-1)
        val a = COSArray()
        a.add(COSFloat(color.floatR))
        a.add(COSFloat(color.floatG))
        a.add(COSFloat(color.floatB))
        return a
    }

    /**
     * Specifies a color and its position in a [PDGradient].
     */
    class GradientPart(val color: Color, val ratio: Float)

    companion object {
        private const val STITCHING_FUNC_TYPE = 3
    }
}
