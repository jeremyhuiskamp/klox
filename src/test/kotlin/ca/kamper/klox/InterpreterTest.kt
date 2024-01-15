package ca.kamper.klox

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class InterpreterTest {
    @Test
    fun basicExpression() {
        assertEquals(1.0, interpret("2 - 1"))
    }

    @Test
    fun addInts() {
        assertEquals(2.0, interpret("1 + 1"))
    }

    @Test
    fun catStrings() {
        assertEquals(
            "hello, world",
            interpret("\"hello, \" + \"world\""),
        )
    }

    @Test
    fun nanInequality() {
        assertEquals(false, interpret("0/0 == 0/0"))
    }

    private fun interpret(input: String): Any? {
        val scanner = Scanner(input)
        val parser = Parser(scanner.scanTokens())
        val expr = parser.parseExpr()
        return Interpreter().evaluate(expr!!)
    }
}