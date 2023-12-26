@file:OptIn(ExperimentalForeignApi::class)

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.*

fun main(args: Array<String>) {
    val (inputOptions, filePaths) = getOptions(args)
    val options = inputOptions.ifEmpty {
        listOf("-c", "-l", "-w")
    }
    filePaths.asSequence()
        .map { filePath ->
            Input.File({ fopen(filePath, "r") }, filePath)
        }
        .ifEmpty {
            // TODO process multiple options for stdin
            sequenceOf(Input.StdIn { stdin })
        }
        .forEach { input ->
            val results: List<Number> = options.map { option ->
                val fPointer = input.fPointer() ?: return
                try {
                    when (option) {
                        "-c" -> {
                            // number of bytes in file
                            bytesInFile(fPointer)
                        }

                        "-l" -> {
                            // number of lines in file
                            linesInFile(fPointer)
                        }

                        "-w" -> {
                            // number of words in file
                            wordsInFile(fPointer)
                        }

                        "-m" -> {
                            // number of characters in file
                            charsInFile(fPointer)
                        }

                        else -> throw IllegalArgumentException("Invalid option specified: $option")
                    }
                } finally {
                    fclose(fPointer)
                }
            }
            input.print(results)
        }
}

sealed class Input {
    abstract val fPointer: () -> CPointer<FILE>?
    abstract fun print(results: List<Number>)

    data class File(
        override val fPointer: () -> CPointer<FILE>?,
        val filePath: String,
    ) : Input() {
        override fun print(results: List<Number>) {
            println("${results.joinToString(separator = " ")} $filePath")
        }
    }

    data class StdIn(
        override val fPointer: () -> CPointer<FILE>?,
    ) : Input() {
        override fun print(results: List<Number>) {
            println(results.joinToString(separator = " "))
        }
    }
}

fun getOptions(args: Array<String>): Pair<List<String>, List<String>> = args.partition { it.startsWith("-") }

fun bytesInFile(fPointer: CPointer<FILE>): Int {
    var charCount = 0
    var c = fgetc(fPointer)
    while (c != EOF) {
        charCount++
        c = fgetc(fPointer)
    }
    return charCount
}

fun linesInFile(fPointer: CPointer<FILE>): Int {
    var lineCount = 0
    var c = fgetc(fPointer)
    while (c != EOF) {
        if (c == '\n'.code) {
            lineCount++
        }
        c = fgetc(fPointer)
    }
    return lineCount
}

fun wordsInFile(fPointer: CPointer<FILE>): Int {
    var wordCount = 0
    var c = fgetc(fPointer)
    var inWord = false
    while (c != EOF) {
        if (c.isCountableChar() && !inWord) {
            inWord = true
        } else if (!c.isCountableChar() && inWord) {
            wordCount++
            inWord = false
        }
        c = fgetc(fPointer)
    }
    if (inWord) {
        wordCount++
    }
    return wordCount
}

fun charsInFile(fPointer: CPointer<FILE>): Int {
    setlocale(LC_ALL, "en_US.utf8")
    var charCount = 0
    var c = fgetwc(fPointer)
    while (c != WEOF) {
        charCount++
        c = fgetwc(fPointer)
    }
    return charCount
}

fun Int.isCountableChar() = this != ' '.code && this != '\n'.code && this != '\t'.code