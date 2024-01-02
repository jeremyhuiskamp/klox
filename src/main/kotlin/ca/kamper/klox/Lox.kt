package ca.kamper.klox

import java.io.File
import kotlin.system.exitProcess

var hadError = false

fun main(args: Array<String>) {
    if (args.size > 1) {
        println("Usage: klox [script]")
        exitProcess(64)
    } else if (args.size == 1) {
        runFile(args[0])
    } else {
        runPrompt()
    }
}

fun runFile(path: String) {
    run(File(path).readText())

    if (hadError) exitProcess(65)
}

fun run(code: String) {
    val scanner = Scanner(code)
    val tokens = scanner.scanTokens()
    for (token in tokens) {
        println(token)
    }
}

fun runPrompt() {
    while (true) {
        print("> ")
        val line = readlnOrNull() ?: break
        run(line)
        hadError = false
    }
}

fun error(line: Int, message: String) {
    report(line, "", message)
}

fun report(line: Int, where: String, message: String) {
    System.err.println("[line $line] Error$where: $message")
    hadError = true
}
