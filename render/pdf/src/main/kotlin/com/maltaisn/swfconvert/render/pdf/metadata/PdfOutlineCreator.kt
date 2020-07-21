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

package com.maltaisn.swfconvert.render.pdf.metadata

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitHeightDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitRectangleDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import javax.inject.Inject

internal class PdfOutlineCreator @Inject constructor() {

    fun createOutline(pdfDoc: PDDocument, items: List<PdfOutlineItem>, openLevel: Int) {
        val outline = PDDocumentOutline()
        pdfDoc.documentCatalog.documentOutline = outline
        for (item in items) {
            addItemToOutline(item, outline, 1, openLevel)
        }
        if (openLevel > 0) {
            outline.openNode()
        }
    }

    private fun addItemToOutline(item: PdfOutlineItem, outline: PDOutlineNode, level: Int, openLevel: Int) {
        val pdfItem = PDOutlineItem()
        val destination = createPdfOutlineDestination(item)
        pdfItem.title = item.title
        pdfItem.destination = destination
        destination.pageNumber = item.page
        outline.addLast(pdfItem)
        for (child in item.children) {
            addItemToOutline(child, pdfItem, level + 1, openLevel)
        }
        if (level <= openLevel) {
            pdfItem.openNode()
        } else {
            pdfItem.closeNode()
        }
    }

    private fun createPdfOutlineDestination(item: PdfOutlineItem) = when (item) {
        is PdfOutlineItem.FitHeight -> PDPageFitHeightDestination().apply {
            left = item.x
        }
        is PdfOutlineItem.FitWidth -> PDPageFitWidthDestination().apply {
            top = item.y
        }
        is PdfOutlineItem.FitRect -> PDPageFitRectangleDestination().apply {
            left = item.left
            bottom = item.bottom
            right = item.right
            top = item.top
        }
        is PdfOutlineItem.FitXYZ -> PDPageXYZDestination().apply {
            left = item.x
            top = item.y
            zoom = item.zoom
        }
    }

}
