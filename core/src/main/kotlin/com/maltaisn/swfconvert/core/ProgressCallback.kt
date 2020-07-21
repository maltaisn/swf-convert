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

package com.maltaisn.swfconvert.core

/**
 * Callback to indicate progress during conversion.
 */
interface ProgressCallback {

    fun beginStep(name: String)
    fun endStep()

    fun beginProgress(total: Int)
    fun endProgress()

    fun incrementProgress()
    fun publishProgress(value: Int)

}

inline fun <R> ProgressCallback.showStep(name: String, block: () -> R): R {
    this.beginStep(name)
    val result = block()
    this.endStep()
    return result
}

inline fun <R> ProgressCallback.showProgress(total: Int, block: () -> R): R {
    this.beginProgress(total)
    val result = block()
    this.endProgress()
    return result
}
