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

package com.maltaisn.swfconvert.app

import com.maltaisn.swfconvert.core.ProgressCallback
import org.apache.logging.log4j.kotlin.logger
import java.util.concurrent.atomic.AtomicInteger

class ProgressPrinter : ProgressCallback {

    private val logger = logger()

    private val stepStack = ArrayDeque<Step>()

    private var progressShown = false
    private var total = -1
    private val progress = AtomicInteger()

    /**
     * Whether there has been an action (new progress or new step) after the
     * last ended step. This is needed to know when to insert new lines.
     */
    private var actionAfterEnd = false

    override fun beginStep(name: String, important: Boolean) {
        stepStack += Step(name, important)
        actionAfterEnd = true
        updateLine()
        logger.info { stepStack.joinToString(": ") { it.name } }
    }

    override fun endStep() {
        stepStack.removeLast()
        if (actionAfterEnd) {
            println()
        }
        actionAfterEnd = false
    }

    override fun beginProgress(total: Int) {
        check(!progressShown) { "Progress already shown." }
        check(total >= 0) { "Total must be positive." }
        progressShown = true
        actionAfterEnd = true
        this.total = total
        progress.set(0)
        updateLine()
    }

    override fun endProgress() {
        checkProgressShown()
        progressShown = false
        updateLine()
        this.total = -1
        progress.set(0)
    }

    @Synchronized
    override fun incrementProgress() {
        checkProgressShown()
        val value = progress.incrementAndGet()
        check(value <= total) { "Progress value is greater than total." }
        updateLine()
    }

    @Synchronized
    override fun publishProgress(value: Int) {
        checkProgressShown()
        check(value <= total) { "Progress value is greater than total." }
        progress.set(value)
        updateLine()
    }

    private fun checkProgressShown() = check(progressShown) { "No progress shown." }

    private fun updateLine() {
        // Print step name
        for ((i, step) in stepStack.withIndex()) {
            if (step.important) {
                print("\u001b[1m") // Enable bold
            }
            print(step.name)
            if (i != stepStack.lastIndex) {
                print(": ")
            }
            if (step.important) {
                print("\u001b[0m") // Disable bold
            }
        }
        print(" ")

        // Print progress
        val value = progress.get()
        if (progressShown && total > 0) {
            val chars = (value / total.toFloat() * PROGRESS_BAR_SIZE).toInt()
            print("[${"#".repeat(chars)}${"-".repeat(PROGRESS_BAR_SIZE - chars)}]")
        }
        if (total != -1) {
            print(" ($value / $total)")
        }
        print("\r")
    }

    private data class Step(val name: String, val important: Boolean)

    companion object {
        private const val PROGRESS_BAR_SIZE = 30
    }

}
