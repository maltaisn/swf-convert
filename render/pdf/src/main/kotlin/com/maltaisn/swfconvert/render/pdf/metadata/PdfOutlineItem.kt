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


/**
 * An item of a PDF outline.
 */
@Serializable
sealed class PdfOutlineItem {

    abstract val title: String
    abstract val page: Int
    abstract val children: List<PdfOutlineItem>

    @Serializable
    @SerialName("fit_width")
    data class FitWidth(
            @SerialName("title") override val title: String,
            @SerialName("page") override val page: Int,
            @SerialName("children") override val children: List<PdfOutlineItem> = emptyList(),
            @SerialName("y") val y: Int = 0
    ) : PdfOutlineItem()

    @Serializable
    @SerialName("fit_height")
    data class FitHeight(
            @SerialName("title") override val title: String,
            @SerialName("page") override val page: Int,
            @SerialName("children") override val children: List<PdfOutlineItem> = emptyList(),
            @SerialName("x") val x: Int = 0
    ) : PdfOutlineItem()

    @Serializable
    @SerialName("fit_rect")
    data class FitRect(
            @SerialName("title") override val title: String,
            @SerialName("page") override val page: Int,
            @SerialName("children") override val children: List<PdfOutlineItem> = emptyList(),
            @SerialName("top") val top: Int = 0,
            @SerialName("left") val left: Int = 0,
            @SerialName("bottom") val bottom: Int = 0,
            @SerialName("right") val right: Int = 0
    ) : PdfOutlineItem()

    @Serializable
    @SerialName("fit_xyz")
    data class FitXYZ(
            @SerialName("title") override val title: String,
            @SerialName("page") override val page: Int,
            @SerialName("children") override val children: List<PdfOutlineItem> = emptyList(),
            @SerialName("x") val x: Int = 0,
            @SerialName("y") val y: Int = 0,
            @SerialName("zoom") val zoom: Float = 0f
    ) : PdfOutlineItem()

}
