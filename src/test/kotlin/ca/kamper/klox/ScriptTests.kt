package ca.kamper.klox

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class ScriptTests {
    @TestFactory
    fun `scripts in src_test_resources_lox`(): List<DynamicTest> = Paths.get(
        "src", "test", "resources", "lox"
    )
        .listDirectoryEntries("*.lox")
        .map { path ->
            DynamicTest.dynamicTest(path.name) {
                check(path)
            }
        }

    private fun check(scriptFile: Path) {
        val case = ScriptTestCase(scriptFile)
        val expectations = ScriptExpectations(scriptFile)
        expectations.check(case)
    }
}
