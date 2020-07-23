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

import com.maltaisn.swfconvert.render.pdf.PdfConfiguration
import org.apache.logging.log4j.kotlin.logger
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.common.PDPageLabelRange
import org.apache.pdfbox.pdmodel.common.PDPageLabels
import javax.inject.Inject

/**
 * Create optimized PDF page labels from a list of page names.
 */
internal class PdfPageLabelsCreator @Inject constructor(
    private val config: PdfConfiguration
) {

    private val logger = logger()

    fun createPageLabels(pdfDoc: PDDocument, pageLabels: List<String>) {
        if (pdfDoc.numberOfPages != pageLabels.size) {
            logger.error {
                "PDF metadata error: document has ${pdfDoc.numberOfPages} pages " +
                        "but found ${pageLabels.size} page labels."
            }
            return
        }

        pdfDoc.documentCatalog.pageLabels = if (config.optimizePageLabels) {
            createOptimizedLabels(pdfDoc, pageLabels)
        } else {
            createLabels(pdfDoc, pageLabels)
        }
    }

    /**
     * Create page labels, grouping similar names with ranges.
     */
    private fun createOptimizedLabels(pdfDoc: PDDocument, pageLabels: List<String>): PDPageLabels {
        val pdfPageLabels = PDPageLabels(pdfDoc)
        var lastRange: PDPageLabelRange? = null
        var lastNumber = -1
        for ((i, pageName) in pageLabels.withIndex()) {
            val range = createPageLabelRange(pageName)
            if (lastRange == null || !range.hasSameStyleAs(lastRange) || range.start != lastNumber + 1) {
                // Create new range because either:
                // - There's no previous range
                // - Previous range has different style has current one
                // - New range numbering doesn't extend previous numbering.
                lastRange = range
                pdfPageLabels.setLabelItem(i, range)
            }
            lastNumber = range.start
        }
        return pdfPageLabels
    }

    private fun PDPageLabelRange.hasSameStyleAs(other: PDPageLabelRange) =
        this.style == other.style && this.prefix == other.prefix

    /**
     * Create page labels with a range for each page name.
     */
    private fun createLabels(pdfDoc: PDDocument, pageLabels: List<String>): PDPageLabels {
        val pdfPageLabels = PDPageLabels(pdfDoc)
        for ((i, pageName) in pageLabels.withIndex()) {
            val range = PDPageLabelRange()
            range.prefix = pageName
            range.style = null
            pdfPageLabels.setLabelItem(i, range)
        }
        return pdfPageLabels
    }

    private fun createPageLabelRange(pageName: String): PDPageLabelRange {
        val range = PDPageLabelRange()

        // Get all numeric character to get page name and try to parse it.
        val pageNumber = pageName.takeLastWhile { it in '0'..'9' }
        var start = pageNumber.toIntOrNull()
        range.style = when {
            pageName.isEmpty() -> null
            start == null -> {
                // Page number isn't decimal, try roman numbers.
                start = pageName.romanNumberToIntOrNull()
                when {
                    start == null -> {
                        // Page number isn't roman, try letters numbers.
                        start = pageName.lettersNumberToIntOrNull()
                        when {
                            start == null -> {
                                // Page number isn't of any built-in styles, use prefix without style.
                                range.prefix = pageNumber
                                null
                            }
                            pageName.isUppercase() -> PDPageLabelRange.STYLE_LETTERS_UPPER
                            else -> PDPageLabelRange.STYLE_LETTERS_LOWER
                        }
                    }
                    pageName.isUppercase() -> PDPageLabelRange.STYLE_ROMAN_UPPER
                    else -> PDPageLabelRange.STYLE_ROMAN_LOWER
                }
            }
            start <= 0 -> null
            else -> {
                val prefixLength = pageName.length - pageNumber.length
                range.prefix = if (prefixLength == 0) null else pageName.substring(0, prefixLength)
                PDPageLabelRange.STYLE_DECIMAL
            }
        }
        if (start != null) {
            range.start = start
        }

        return range
    }

    private fun String.romanNumberToIntOrNull(): Int? {
        var n = 0
        for ((i, c) in this.withIndex()) {
            val v = ROMAN_VALUES[c.toUpperCase()] ?: return null
            if (i == this.length - 1 || v >= ROMAN_VALUES[this[i + 1].toUpperCase()] ?: return null) {
                n += v
            } else {
                n -= v
            }
        }
        return n
    }

    private fun String.lettersNumberToIntOrNull(): Int? {
        val c = this[0]
        if (c.toUpperCase() !in 'A'..'Z' && !this.all { it == c }) {
            return null
        }
        return (c.toUpperCase() - 'A' + 1) * this.length
    }

    private fun String.isUppercase() = (this.toUpperCase() == this)

    companion object {
        private val ROMAN_VALUES = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
    }

}
