package ca.kamper.klox

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration.ofSeconds
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class ScriptTests {
    @TestFactory
    fun `scripts in src_test_resources_lox`(): List<DynamicTest> = Paths.get(
        "src", "test", "resources", "lox"
    )
        .listDirectoryEntries("*.lox")
        .map { path ->
            DynamicTest.dynamicTest(path.name, path.toUri()) {
                // bugs in the interpreter can lead to infinite loops:
                assertTimeoutPreemptively(ofSeconds(1)) {
                    check(path)
                }
            }
        }

    private fun check(scriptFile: Path) {
        val case = ScriptTestCase(scriptFile)
        val expectations = ScriptExpectations(scriptFile)
        expectations.check(case)
    }
}
