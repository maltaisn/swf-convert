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
