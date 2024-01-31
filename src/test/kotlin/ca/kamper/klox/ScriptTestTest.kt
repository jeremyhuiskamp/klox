package ca.kamper.klox

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

/**
 * The framework for testing the interpreter is non-trivial,
 * so this is a test of the tests.
 */
class ScriptTestTest {
    private val metaTestDir = Paths.get("src", "test", "resources", "loxtest")

    @Test
    fun `incorrect prints`() {
        val fail = assertThrows<AssertionError> {
            testScript("wrong_output.lox")
        }
        assertThat(fail).hasMessageContaining("script output")
    }

    @Test
    fun `correct prints`() {
        testScript("correct_output.lox")
    }

    @Test
    fun `handles scanner error`() {
        testScript("scanner_error.lox")
    }

    @Test
    fun `finds missing scanner error`() {
        val fail = assertThrows<AssertionError> {
            testScript("missed_scanner_error.lox")
        }
        // not clear what message we can really count on here, but it
        // should have the error from the scanner:
        assertThat(fail).hasMessageContaining("Unterminated")
    }

    @Test
    fun `handles parser error`() {
        testScript("parser_error.lox")
    }

    @Test
    fun `finds missing parser error`() {
        val fail = assertThrows<AssertionError> {
            testScript("missed_parser_error.lox")
        }
        // not clear what message we can really count on here, but it
        // should have the error from the parser:
        assertThat(fail).hasMessageContaining("Expect ';'")
    }

    @Test
    fun `handles interpreter error`() {
        testScript("intepreter_error.lox")
    }

    @Test
    fun `finds missing interpreter error`() {
        val fail = assertThrows<AssertionError> {
            testScript("missed_interpreter_error.lox")
        }
        // not clear what message we can really count on here, but it
        // should have the error from the parser:
        assertThat(fail).hasMessageContaining("undefinedVar")
    }

    private fun testScript(scriptFile: String) {
        val scriptPath = metaTestDir.resolve(scriptFile)
        val case = ScriptTestCase(scriptPath)
        val expectations = ScriptExpectations(scriptPath)
        expectations.check(case)
    }
}