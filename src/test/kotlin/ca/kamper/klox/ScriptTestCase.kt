package ca.kamper.klox

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Executes a script file, collects output and errors and provides facilities
 * for asserting against those results.
 */
class ScriptTestCase(private val scriptFile: Path) {
    data class ScriptError(val line: Int, val msg: String)

    private val scriptContent = scriptFile.readText()
    private val errors = mutableListOf<ScriptError>()
    private val printedOutput = mutableListOf<String>()

    init {
        val scanner = Scanner(scriptContent, ::scannerError)
        val parser = Parser(scanner.scanTokens(), ::parserError)
        val tokens = parser.parse()
        if (!scriptHadErrors()) {
            val interpreter = Interpreter(::capturePrint)
            try {
                interpreter.interpret(tokens, ::interpreterError)
            } catch (e: Exception) {
                addError(0, "unexpected exception: $e")
            }
        }
    }

    private fun scannerError(line: Int, message: String) {
        addError(line, message)
    }

    private fun parserError(token: Token, message: String) {
        addError(token.line, message)
    }

    private fun capturePrint(value: String) {
        printedOutput.add(value)
    }

    private fun interpreterError(error: RuntimeError) {
        addError(error.token.line, error.message ?: "")
    }

    private fun addError(line: Int, msg: String) {
        errors.add(ScriptError(line, msg))
    }

    private fun scriptHadErrors(): Boolean = errors.isNotEmpty()

    fun assertNoErrors() {
        assertThat(errors).isEmpty()
    }

    fun assertOutput(expected: List<String>) {
        assertThat(printedOutput).`as`("script output").isEqualTo(expected)
    }

    fun assertError(line: Int, error: String) {
        // TODO: is there any useful assertion we want to make against the message?
        // should the expected error be a regex?
        assertThat(errors).haveAtLeastOne(errorOnLine(line))
    }

    private fun errorOnLine(expLine: Int): Condition<ScriptError> = Condition(
        { expLine == it.line },
        ": an error on line $expLine",
    )

    override fun toString() = scriptFile.toString()
}