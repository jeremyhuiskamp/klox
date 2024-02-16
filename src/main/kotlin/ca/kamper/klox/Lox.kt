package ca.kamper.klox

import java.io.File
import kotlin.system.exitProcess

var hadError = false
var hadRuntimeError = false
val interpreter = Interpreter()

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
    if (hadRuntimeError) exitProcess(70)
}

fun run(code: String) {
    val scanner = Scanner(code)
    val tokens = scanner.scanTokens()
    val parser = Parser(tokens)
    val stmts = parser.parse()
    if (hadError) return
    interpreter.interpret(stmts)
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

fun error(token: Token, message: String) {
    val where = if (token.type == TokenType.EOF) "end" else "'${token.lexeme}'"
    report(token.line, " at $where", message)
}

fun report(line: Int, where: String, message: String) {
    System.err.println("[line $line] Error$where: $message")
    hadError = true
}

fun runtimeError(error: RuntimeError) {
    System.err.println("[line ${error.token.line}] ${error.message}")
    hadRuntimeError = true
}
