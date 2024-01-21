package ca.kamper.klox

import org.assertj.core.api.Assertions.assertThat
import java.nio.file.Path
import kotlin.io.path.readText

class ScriptTestCase(private val scriptFile: Path) {
    private val scriptContent = scriptFile.readText()
    private val errors = mutableListOf<Pair<Int, String>>()
    private val printedOutput = mutableListOf<String>()

    init {
        val scanner = Scanner(scriptContent, ::scannerError)
        val parser = Parser(scanner.scanTokens(), ::parserError)
        val tokens = parser.parse()
        if (!scriptHadErrors()) {
            val interpreter = Interpreter(::capturePrint)
            interpreter.interpret(tokens, ::interpreterError)
        }
    }

    private fun scannerError(line: Int, message: String) {
        errors.add(line to message)
    }

    private fun parserError(token: Token, message: String) {
        errors.add(token.line to message)
    }

    private fun capturePrint(value: String) {
        printedOutput.add(value)
    }

    private fun interpreterError(error: RuntimeError) {
        errors.add(error.token.line to (error.message ?: ""))
    }

    private fun scriptHadErrors(): Boolean = errors.isNotEmpty()

    fun assertNoErrors() {
        assertThat(errors).`as`("script errors").isEmpty()
    }

    fun assertOutput(expected: List<String>) {
        assertThat(printedOutput).`as`("script output").isEqualTo(expected)
    }

    override fun toString() = scriptFile.toString()
}