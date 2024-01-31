package ca.kamper.klox

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readLines

/**
 * Identifies the expected results of running a script and
 * collaborates with [ScriptTestCase] to ensure that they
 * match what actually resulted.
 */
class ScriptExpectations(private val scriptFile: Path) {
    fun check(case: ScriptTestCase) {
        val expectedPrints = getAnticipatedOutput(scriptFile)
        val expectedErrors = findExpectedErrors(scriptFile)
        if (expectedErrors.isNotEmpty()) {
            // every expected error should have happened
            for ((line, error) in expectedErrors) {
                case.assertError(line, error)
            }
        } else {
            case.assertNoErrors()
        }

        case.assertOutput(expectedPrints)
    }

    private fun getAnticipatedOutput(scriptFile: Path): List<String> {
        // backwards compatibility: support a complete .out file
        val outFile = scriptFile.resolveSibling(scriptFile.name + ".out")
        if (outFile.exists()) {
            return outFile.readLines()
        }

        // better solution: read from specific comments in the script itself:
        return scriptFile.readLines().mapNotNull { lineOfCode ->
            val codeAndComment = lineOfCode.split("//print: ")
            if (codeAndComment.size < 2) {
                null
            } else {
                codeAndComment[1]
            }
        }
    }

    /**
     * Parse the script for comments indicating an expected error.
     * The comment must occur on the line that will trigger the error.
     * Example:
     * ```
     * bleepBloop(); //error: unknown function bleepBloop
     * ```
     * The error may occur at any stage (scanning, parsing, execution).
     * The trailing message isn't currently used but may as well be informative.
     */
    private fun findExpectedErrors(scriptFile: Path): List<Pair<Int, String>> {
        return scriptFile.readLines().mapIndexedNotNull { index, lineOfCode ->
            val lineNo = index + 1
            val codeAndComment = lineOfCode.split("//error:", limit = 2)
            if (codeAndComment.size < 2) {
                null
            } else {
                val comment = codeAndComment[1].trim()
                lineNo to comment
            }
        }
    }
}