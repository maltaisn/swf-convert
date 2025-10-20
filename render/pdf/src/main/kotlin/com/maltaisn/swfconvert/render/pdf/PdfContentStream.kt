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

@file:Suppress("TooManyFunctions")

package com.maltaisn.swfconvert.render.pdf

import com.maltaisn.swfconvert.core.image.Color
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDFormContentStream
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.graphics.color.PDColor
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.graphics.image.PDInlineImage
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode
import org.apache.pdfbox.util.Matrix

// Note: all content streams in PDFBox 3.x correctly inherit PDAbstractContentStream.
// But we'll keep using PDFBox 2.x for stability.

internal interface PdfContentStream {
    fun beginText()
    fun endText()
    fun setFont(font: PDFont, fontSize: Float)
    fun showTextWithPositioning(textWithPositioningArray: Array<Any>)
    fun showText(text: String)
    fun setLeading(leading: Float)
    fun newLine()
    fun newLineAtOffset(tx: Float, ty: Float)
    fun setTextMatrix(matrix: Matrix)
    fun drawImage(image: PDImageXObject, x: Float, y: Float)
    fun drawImage(image: PDImageXObject, x: Float, y: Float, width: Float, height: Float)
    fun drawImage(image: PDImageXObject, matrix: Matrix)
    fun drawImage(inlineImage: PDInlineImage, x: Float, y: Float)
    fun drawImage(inlineImage: PDInlineImage, x: Float, y: Float, width: Float, height: Float)
    fun drawForm(form: PDFormXObject)
    fun transform(matrix: Matrix)
    fun saveGraphicsState()
    fun restoreGraphicsState()
    fun setStrokingColor(color: PDColor)
    fun setStrokingColor(color: Color)
    fun setStrokingColor(r: Float, g: Float, b: Float)
    fun setStrokingColor(c: Float, m: Float, y: Float, k: Float)
    fun setStrokingColor(g: Float)
    fun setNonStrokingColor(color: PDColor)
    fun setNonStrokingColor(color: Color)
    fun setNonStrokingColor(r: Float, g: Float, b: Float)
    fun setNonStrokingColor(c: Float, m: Float, y: Float, k: Float)
    fun setNonStrokingColor(g: Float)
    fun addRect(x: Float, y: Float, width: Float, height: Float)
    fun curveTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float)
    fun curveTo2(x2: Float, y2: Float, x3: Float, y3: Float)
    fun curveTo1(x1: Float, y1: Float, x3: Float, y3: Float)
    fun moveTo(x: Float, y: Float)
    fun lineTo(x: Float, y: Float)
    fun stroke()
    fun closeAndStroke()
    fun fill()
    fun fillEvenOdd()
    fun fillAndStroke()
    fun fillAndStrokeEvenOdd()
    fun closeAndFillAndStroke()
    fun closeAndFillAndStrokeEvenOdd()
    fun shadingFill(shading: PDShading)
    fun closePath()
    fun clip()
    fun clipEvenOdd()
    fun setLineWidth(lineWidth: Float)
    fun setLineJoinStyle(lineJoinStyle: Int)
    fun setLineCapStyle(lineCapStyle: Int)
    fun setLineDashPattern(pattern: FloatArray, phase: Float)
    fun setMiterLimit(miterLimit: Float)
    fun beginMarkedContent(tag: COSName)
    fun beginMarkedContent(tag: COSName, propertyList: PDPropertyList)
    fun endMarkedContent()
    fun setGraphicsStateParameters(state: PDExtendedGraphicsState)
    fun addComment(comment: String)
    fun setCharacterSpacing(spacing: Float)
    fun setWordSpacing(spacing: Float)
    fun setHorizontalScaling(scale: Float)
    fun setRenderingMode(rm: RenderingMode)
    fun setTextRise(rise: Float)
    fun close()
}

internal class PdfPageContentStream(pdfDoc: PDDocument, page: PDPage, compress: Boolean) : PdfContentStream {

    private val stream = PDPageContentStream(pdfDoc, page,
        PDPageContentStream.AppendMode.OVERWRITE, compress)

    override fun beginText() = stream.beginText()
    override fun endText() = stream.endText()
    override fun setFont(font: PDFont, fontSize: Float) = stream.setFont(font, fontSize)
    override fun showTextWithPositioning(textWithPositioningArray: Array<Any>) =
        stream.showTextWithPositioning(textWithPositioningArray)

    override fun showText(text: String) = stream.showText(text)
    override fun setLeading(leading: Float) = stream.setLeading(leading)
    override fun newLine() = stream.newLine()
    override fun newLineAtOffset(tx: Float, ty: Float) = stream.newLineAtOffset(tx, ty)
    override fun setTextMatrix(matrix: Matrix) = stream.setTextMatrix(matrix)
    override fun drawImage(image: PDImageXObject, x: Float, y: Float) = stream.drawImage(image, x, y)
    override fun drawImage(image: PDImageXObject, x: Float, y: Float, width: Float, height: Float) =
        stream.drawImage(image, x, y, width, height)

    override fun drawImage(image: PDImageXObject, matrix: Matrix) = stream.drawImage(image, matrix)
    override fun drawImage(inlineImage: PDInlineImage, x: Float, y: Float) = stream.drawImage(inlineImage, x, y)
    override fun drawImage(inlineImage: PDInlineImage, x: Float, y: Float, width: Float, height: Float) =
        stream.drawImage(inlineImage, x, y, width, height)

    override fun drawForm(form: PDFormXObject) = stream.drawForm(form)
    override fun transform(matrix: Matrix) = stream.transform(matrix)
    override fun saveGraphicsState() = stream.saveGraphicsState()
    override fun restoreGraphicsState() = stream.restoreGraphicsState()
    override fun setStrokingColor(color: PDColor) = stream.setStrokingColor(color)
    override fun setStrokingColor(color: Color) = stream.setStrokingColor(color.toAwtColor())
    override fun setStrokingColor(r: Float, g: Float, b: Float) = stream.setStrokingColor(r, g, b)
    override fun setStrokingColor(c: Float, m: Float, y: Float, k: Float) = stream.setStrokingColor(c, m, y, k)
    override fun setStrokingColor(g: Float) = stream.setStrokingColor(g)
    override fun setNonStrokingColor(color: PDColor) = stream.setNonStrokingColor(color)
    override fun setNonStrokingColor(color: Color) = stream.setNonStrokingColor(color.toAwtColor())
    override fun setNonStrokingColor(r: Float, g: Float, b: Float) = stream.setNonStrokingColor(r, g, b)
    override fun setNonStrokingColor(c: Float, m: Float, y: Float, k: Float) = stream.setNonStrokingColor(c, m, y, k)
    override fun setNonStrokingColor(g: Float) = stream.setNonStrokingColor(g)
    override fun addRect(x: Float, y: Float, width: Float, height: Float) = stream.addRect(x, y, width, height)
    override fun curveTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
        stream.curveTo(x1, y1, x2, y2, x3, y3)

    override fun curveTo2(x2: Float, y2: Float, x3: Float, y3: Float) = stream.curveTo2(x2, y2, x3, y3)
    override fun curveTo1(x1: Float, y1: Float, x3: Float, y3: Float) = stream.curveTo1(x1, y1, x3, y3)
    override fun moveTo(x: Float, y: Float) = stream.moveTo(x, y)
    override fun lineTo(x: Float, y: Float) = stream.lineTo(x, y)
    override fun stroke() = stream.stroke()
    override fun closeAndStroke() = stream.closeAndStroke()
    override fun fill() = stream.fill()
    override fun fillEvenOdd() = stream.fillEvenOdd()
    override fun fillAndStroke() = stream.fillAndStroke()
    override fun fillAndStrokeEvenOdd() = stream.fillAndStrokeEvenOdd()
    override fun closeAndFillAndStroke() = stream.closeAndFillAndStroke()
    override fun closeAndFillAndStrokeEvenOdd() = stream.closeAndFillAndStrokeEvenOdd()
    override fun shadingFill(shading: PDShading) = stream.shadingFill(shading)
    override fun closePath() = stream.closePath()
    override fun clip() = stream.clip()
    override fun clipEvenOdd() = stream.clipEvenOdd()
    override fun setLineWidth(lineWidth: Float) = stream.setLineWidth(lineWidth)
    override fun setLineJoinStyle(lineJoinStyle: Int) = stream.setLineJoinStyle(lineJoinStyle)
    override fun setLineCapStyle(lineCapStyle: Int) = stream.setLineCapStyle(lineCapStyle)
    override fun setLineDashPattern(pattern: FloatArray, phase: Float) = stream.setLineDashPattern(pattern, phase)
    override fun setMiterLimit(miterLimit: Float) = stream.setMiterLimit(miterLimit)
    override fun beginMarkedContent(tag: COSName) = stream.beginMarkedContent(tag)
    override fun beginMarkedContent(tag: COSName, propertyList: PDPropertyList) =
        stream.beginMarkedContent(tag, propertyList)

    override fun endMarkedContent() = stream.endMarkedContent()
    override fun setGraphicsStateParameters(state: PDExtendedGraphicsState) = stream.setGraphicsStateParameters(state)
    override fun addComment(comment: String) = stream.addComment(comment)
    override fun setCharacterSpacing(spacing: Float) = stream.setCharacterSpacing(spacing)
    override fun setWordSpacing(spacing: Float) = stream.setWordSpacing(spacing)
    override fun setHorizontalScaling(scale: Float) = stream.setHorizontalScaling(scale)
    override fun setRenderingMode(rm: RenderingMode) = stream.setRenderingMode(rm)
    override fun setTextRise(rise: Float) = stream.setTextRise(rise)
    override fun close() = stream.close()
}

internal class PdfFormContentStream(form: PDFormXObject) : PdfContentStream {

    private val stream = PDFormContentStream(form)

    override fun beginText() = stream.beginText()
    override fun endText() = stream.endText()
    override fun setFont(font: PDFont, fontSize: Float) = stream.setFont(font, fontSize)
    override fun showTextWithPositioning(textWithPositioningArray: Array<Any>) =
        stream.showTextWithPositioning(textWithPositioningArray)

    override fun showText(text: String) = stream.showText(text)
    override fun setLeading(leading: Float) = stream.setLeading(leading)
    override fun newLine() = stream.newLine()
    override fun newLineAtOffset(tx: Float, ty: Float) = stream.newLineAtOffset(tx, ty)
    override fun setTextMatrix(matrix: Matrix) = stream.setTextMatrix(matrix)
    override fun drawImage(image: PDImageXObject, x: Float, y: Float) = stream.drawImage(image, x, y)
    override fun drawImage(image: PDImageXObject, x: Float, y: Float, width: Float, height: Float) =
        stream.drawImage(image, x, y, width, height)

    override fun drawImage(image: PDImageXObject, matrix: Matrix) = stream.drawImage(image, matrix)
    override fun drawImage(inlineImage: PDInlineImage, x: Float, y: Float) = stream.drawImage(inlineImage, x, y)
    override fun drawImage(inlineImage: PDInlineImage, x: Float, y: Float, width: Float, height: Float) =
        stream.drawImage(inlineImage, x, y, width, height)

    override fun drawForm(form: PDFormXObject) = stream.drawForm(form)
    override fun transform(matrix: Matrix) = stream.transform(matrix)
    override fun saveGraphicsState() = stream.saveGraphicsState()
    override fun restoreGraphicsState() = stream.restoreGraphicsState()
    override fun setStrokingColor(color: PDColor) = stream.setStrokingColor(color)
    override fun setStrokingColor(color: Color) = stream.setStrokingColor(color.toAwtColor())
    override fun setStrokingColor(r: Float, g: Float, b: Float) = stream.setStrokingColor(r, g, b)
    override fun setStrokingColor(c: Float, m: Float, y: Float, k: Float) = stream.setStrokingColor(c, m, y, k)
    override fun setStrokingColor(g: Float) = stream.setStrokingColor(g)
    override fun setNonStrokingColor(color: PDColor) = stream.setNonStrokingColor(color)
    override fun setNonStrokingColor(color: Color) = stream.setNonStrokingColor(color.toAwtColor())
    override fun setNonStrokingColor(r: Float, g: Float, b: Float) = stream.setNonStrokingColor(r, g, b)
    override fun setNonStrokingColor(c: Float, m: Float, y: Float, k: Float) = stream.setNonStrokingColor(c, m, y, k)
    override fun setNonStrokingColor(g: Float) = stream.setNonStrokingColor(g)
    override fun addRect(x: Float, y: Float, width: Float, height: Float) = stream.addRect(x, y, width, height)
    override fun curveTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
        stream.curveTo(x1, y1, x2, y2, x3, y3)

    override fun curveTo2(x2: Float, y2: Float, x3: Float, y3: Float) = stream.curveTo2(x2, y2, x3, y3)
    override fun curveTo1(x1: Float, y1: Float, x3: Float, y3: Float) = stream.curveTo1(x1, y1, x3, y3)
    override fun moveTo(x: Float, y: Float) = stream.moveTo(x, y)
    override fun lineTo(x: Float, y: Float) = stream.lineTo(x, y)
    override fun stroke() = stream.stroke()
    override fun closeAndStroke() = stream.closeAndStroke()
    override fun fill() = stream.fill()
    override fun fillEvenOdd() = stream.fillEvenOdd()
    override fun fillAndStroke() = stream.fillAndStroke()
    override fun fillAndStrokeEvenOdd() = stream.fillAndStrokeEvenOdd()
    override fun closeAndFillAndStroke() = stream.closeAndFillAndStroke()
    override fun closeAndFillAndStrokeEvenOdd() = stream.closeAndFillAndStrokeEvenOdd()
    override fun shadingFill(shading: PDShading) = stream.shadingFill(shading)
    override fun closePath() = stream.closePath()
    override fun clip() = stream.clip()
    override fun clipEvenOdd() = stream.clipEvenOdd()
    override fun setLineWidth(lineWidth: Float) = stream.setLineWidth(lineWidth)
    override fun setLineJoinStyle(lineJoinStyle: Int) = stream.setLineJoinStyle(lineJoinStyle)
    override fun setLineCapStyle(lineCapStyle: Int) = stream.setLineCapStyle(lineCapStyle)
    override fun setLineDashPattern(pattern: FloatArray, phase: Float) = stream.setLineDashPattern(pattern, phase)
    override fun setMiterLimit(miterLimit: Float) = stream.setMiterLimit(miterLimit)
    override fun beginMarkedContent(tag: COSName) = stream.beginMarkedContent(tag)
    override fun beginMarkedContent(tag: COSName, propertyList: PDPropertyList) =
        stream.beginMarkedContent(tag, propertyList)

    override fun endMarkedContent() = stream.endMarkedContent()
    override fun setGraphicsStateParameters(state: PDExtendedGraphicsState) = stream.setGraphicsStateParameters(state)
    override fun addComment(comment: String) = stream.addComment(comment)
    override fun setCharacterSpacing(spacing: Float) = stream.setCharacterSpacing(spacing)
    override fun setWordSpacing(spacing: Float) = stream.setWordSpacing(spacing)
    override fun setHorizontalScaling(scale: Float) = stream.setHorizontalScaling(scale)
    override fun setRenderingMode(rm: RenderingMode) = stream.setRenderingMode(rm)
    override fun setTextRise(rise: Float) = stream.setTextRise(rise)
    override fun close() = stream.close()
}
