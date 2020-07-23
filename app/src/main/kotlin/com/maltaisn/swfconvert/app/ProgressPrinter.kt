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

package com.maltaisn.swfconvert.app

import com.maltaisn.swfconvert.core.ProgressCallback
import org.apache.logging.log4j.kotlin.logger
import java.util.concurrent.atomic.AtomicInteger

internal class ProgressPrinter(val silent: Boolean) : ProgressCallback {

    private val logger = logger()

    private val stepStack = ArrayDeque<String>()

    private var progressShown = false
    private var total = -1
    private val progress = AtomicInteger()

    /**
     * Whether there has been an action (new progress or new step) after the
     * last ended step. This is needed to know when to insert new lines.
     */
    private var actionAfterEnd = false

    override fun beginStep(name: String) {
        stepStack += name
        actionAfterEnd = true
        updateLine()
        logger.info { stepStack.joinToString(": ") }
    }

    override fun endStep() {
        stepStack.removeLast()
        if (actionAfterEnd && !silent) {
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
        if (silent) return

        // Print step name
        for ((i, step) in stepStack.withIndex()) {
            print(step)
            if (i != stepStack.lastIndex) {
                print(": ")
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
        print('\r')
    }

    companion object {
        private const val PROGRESS_BAR_SIZE = 30
    }

}
