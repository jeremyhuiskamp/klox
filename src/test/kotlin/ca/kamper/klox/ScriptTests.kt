package ca.kamper.klox

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readLines

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
        val expectedOutput = getAnticipatedOutput(scriptFile)
        if (expectedOutput != null) {
            case.assertNoErrors()
            case.assertOutput(expectedOutput)
        }
        // TODO: error assertions?
    }

    private fun getAnticipatedOutput(scriptFile: Path): List<String>? {
        val outFile = scriptFile.resolveSibling(scriptFile.name + ".out")
        if (outFile.exists()) {
            return outFile.readLines()
        }
        return null
    }
}