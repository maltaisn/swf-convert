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

package com.maltaisn.swfconvert.render.core

/**
 * Use [readLine] to read the answer to a [message], either `Y` or `N` from input.
 * Returns `true` if answer is yes, `false` if no, otherwise retries until input is valid.
 */
fun readAffirmativeAnswer(message: String): Boolean {
    retry@ while (true) {
        print(message)
        print(" Retry (Y/N)? ")
        return when (readLine()?.toLowerCase()) {
            "y" -> true
            "n" -> false
            else -> continue@retry
        }
    }
}
