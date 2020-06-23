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

    private fun addItemToOutline(
        item: PdfOutlineItem, outline: PDOutlineNode,
        level: Int, openLevel: Int
    ) {
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
