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
