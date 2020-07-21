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
 * Marker interface for a class that must be disposed after use.
 * Class shouldn't be used after having been disposed.
 */
interface Disposable {

    fun dispose()

}

inline fun <T : Disposable, R> T.use(block: (T) -> R): R {
    val result = block(this)
    this.dispose()
    return result
}
