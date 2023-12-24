@file:OptIn(ExperimentalForeignApi::class)

import kotlinx.cinterop.*
import platform.posix.*

fun main(args: Array<String>) {
    val (inputOptions, filePaths) = getOptions(args)
    val options = inputOptions.ifEmpty {
        listOf("-c", "-l", "-w")
    }
    filePaths.forEach { filePath ->
        val results: List<Number> = options.map { option ->
            when (option) {
                "-c" -> {
                    // number of bytes in file
                    bytesInFile(filePath)
                }

                "-l" -> {
                    // number of lines in file
                    linesInFile(filePath)
                }

                "-w" -> {
                    // number of words in file
                    wordsInFile(filePath)
                }

                "-m" -> {
                    // number of characters in file
                    // TODO: multibyte chars
                    charsInFile(filePath)
                }

                else -> throw IllegalArgumentException("Invalid option specified: $option")
            }
        }
        println("${results.joinToString(separator = " ")} $filePath")
    }
}

fun getOptions(args: Array<String>): Pair<List<String>, List<String>> = args.partition { it.startsWith("-") }

fun bytesInFile(filePath: String): Long {
    return memScoped {
        val statResult = alloc<stat>()
        val result = stat(filePath, statResult.ptr)
        if (result == 0) {
            statResult.st_size
        } else {
            throw RuntimeException("Failed to find size of file $filePath")
        }
    }.toLong()
}

fun linesInFile(filePath: String): Int {
    return doFileOperation(filePath) { file ->
        var lines = 0
        var c = fgetc(file)
        while (c != EOF) {
            if (c == '\n'.code) {
                lines++
            }
            c = fgetc(file)
        }
        lines
    }
}

fun wordsInFile(filePath: String): Int {
    return doFileOperation(filePath) { file ->
        var words = 0
        var c = fgetc(file)
        var inWord = false
        while (c != EOF) {
            if (c.isCountableChar() && !inWord) {
                inWord = true
            } else if (!c.isCountableChar() && inWord) {
                words++
                inWord = false
            }
            c = fgetc(file)
        }
        if (inWord) {
            words++
        }
        words
    }
}

fun charsInFile(filePath: String): Int {
    return doFileOperation(filePath) { file ->
        var chars = 0
        var c = fgetwc(file)
        while (c != WEOF) {
            chars++
            c = fgetwc(file)
        }
        chars
    }
}

fun doFileOperation(filePath: String, operation: (CPointer<FILE>) -> Int): Int {
    val file = fopen(filePath, "r") ?: throw IllegalArgumentException("Unable to open file $filePath")
    return try {
        operation(file)
    } finally {
        fclose(file)
    }
}

fun Int.isCountableChar() = this != ' '.code && this != '\n'.code && this != '\t'.code