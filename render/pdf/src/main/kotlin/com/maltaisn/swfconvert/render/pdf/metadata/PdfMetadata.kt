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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PdfMetadata(
    /**
     * Metadata map of keys and values set on PDF.
     * Standard PDF keys: "Title", "Author", "Subject", "Keywords", "Creator", "Producer",
     * "CreationDate", "ModDate" (according to https://exiftool.org/TagNames/PDF.html).
     * Other keys are also possible.
     */
    @SerialName("metadata")
    val metadata: Map<String, String> = emptyMap(),

    /**
     * Label of each page in the output, in order.
     * Set to `null` to not set custom page labels.
     */
    @SerialName("page_labels")
    val pageLabels: List<String>? = null,

    /**
     * List of outline items added to PDF to create a table of contents.
     * An empty list will result in no outline.
     */
    @SerialName("outline")
    val outline: List<PdfOutlineItem> = emptyList(),

    /**
     * Level up to which outline items should be expanded by default.
     * A level of `0` won't expand any.
     */
    @SerialName("outline_open_level")
    val outlineOpenLevel: Int = 1
)
