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
        val outline: List<PdfOutlineItem> = emptyList()
)


