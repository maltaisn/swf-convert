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

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.maltaisn.swfconvert.core.Configuration
import com.maltaisn.swfconvert.core.OutputFormat
import java.io.File


@Parameters
class Args {

    @Parameter(description = "Input files or directories")
    var input: List<String> = mutableListOf()

    @Parameter(names = ["-o", "--output"], variableArity = true, description = "Output files or directories", order = 0)
    var output: List<String> = mutableListOf()

    @Parameter(names = ["-t", "--tempdir"], description = "Temp directory used for debugging and intermediate files.", order = 10)
    var tempDir: String? = null

    @Parameter(names = ["-f", "--format"], description = "Output format: pdf", order = 20)
    var outputFormatName: String = "pdf"

    @Parameter(names = ["-h", "--help"], description = "Show help message", help = true, order = 1000)
    var help = false

    @Parameter(names = ["-v", "--version"], description = "Show version", order = 1010)
    var version = false

    private var _inputFileCollections: List<List<File>>? = null
    private val inputFileCollections: List<List<File>>
        get() {
            if (_inputFileCollections != null) {
                return _inputFileCollections!!
            }
            argError(input.isNotEmpty()) { "No input files." }

            val collections = mutableListOf<List<File>>()
            for (filename in input) {
                val file = File(filename)
                argError(file.exists()) { "Input file '$filename' doesn't exist." }

                collections += if (file.isDirectory) {
                    // File is directory, create collection for its content
                    val collection = file.listFiles()!!.filter { it.isSwfFile() }
                    collection.sortedBy { it.nameWithoutExtension }
                    argError(collection.isNotEmpty()) { "Input folder '$filename' has no SWF files." }
                    collection

                } else {
                    // Create collection from file.
                    argError(file.isSwfFile()) { "Input file '$filename' is not a SWF file." }
                    listOf(file)
                }
            }

            _inputFileCollections = collections
            return collections
        }

    private var _outputFiles: List<File>? = null
    private val outputFiles: List<File>
        get() {
            if (_outputFiles != null) {
                return _outputFiles!!
            }

            val input = inputFileCollections
            if (output.isEmpty()) {
                // Use same directory as input collections
                _outputFiles = input.map { it.first().parentFile }

            } else {
                argError(output.size == input.size) {
                    "Expected as many output files or directories as input."
                }

                val outputFiles = mutableListOf<File>()
                for ((i, filename) in output.withIndex()) {
                    val file = File(filename)
                    outputFiles += file
                    if (file.name.matches("""^.\..*$""".toRegex())) {
                        val format = outputFormat
                        argError(format.singleFileOutput || input[i].size == 1) {
                            "Cannot use single output file for multiple input files and that output format."
                        }

                        // Treat as a file, check extension.
                        when (format) {
                            OutputFormat.PDF -> argError(file.extension.toLowerCase() == OUTPUT_FORMAT_PDF) {
                                "Output file for PDF output should have PDF extension."
                            }
                        }

                    } else {
                        // Treat as directory, create it.
                        file.mkdirs()
                    }
                }
                _outputFiles = outputFiles
            }
            return _outputFiles!!
        }

    private val outputFormat: OutputFormat
        get() = when (outputFormatName.toLowerCase()) {
            OUTPUT_FORMAT_PDF -> OutputFormat.PDF
            else -> argError("Unknown output format '$outputFormatName'")
        }

    /**
     * Create a configuration per input file collection.
     * Throws [ArgumentException] if an argument value is invalid.
     */
    fun createConfigurations(): List<Configuration> {
        val outputFormat = outputFormat
        return inputFileCollections.mapIndexed { i, input ->
            val tempDir = input.first().parentFile
            Configuration(input, outputFiles[i], tempDir, outputFormat)
        }
    }


    private fun File.isSwfFile() = this.extension.toLowerCase() == "swf"

    companion object {
        private const val OUTPUT_FORMAT_PDF = "pdf"
    }
}
