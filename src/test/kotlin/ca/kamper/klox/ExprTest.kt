package ca.kamper.klox

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExprTest {
    @Test
    fun exprsAreUniqueMapKeys() {
        val t = Token(TokenType.VAR, "x", null, 1)

        // Imagine two references to the same variable name on the same line.
        // They have identical values, but should be able to refer to different
        // environment depths, as needed by Interpreter::resolve:
        val expr1 = Expr.Variable(t)
        val expr2 = Expr.Variable(t)

        val map = mutableMapOf<Expr, Int>()
        map[expr1] = 1
        map[expr2] = 2

        assertThat(map).containsEntry(expr1, 1)
        assertThat(map).containsEntry(expr2, 2)

        // This should apply to all Expr types, though only a few are used
        // as map keys in practise.  We're just testing one above to show
        // the example.
    }
}