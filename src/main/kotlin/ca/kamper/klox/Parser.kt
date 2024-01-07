package ca.kamper.klox

import ca.kamper.klox.TokenType.*

class Parser(private val tokens: List<Token>) {
    private var current = 0

    fun parse() = try {
        expression()
    } catch (e: ParseError) {
        null
    }

    private fun expression(): Expr = ternary()

    // The method names are a bit weird here.  Eg, we start by looking
    // for an equality expression, but we don't actually care if there
    // is one, we're just checking for equality operators after looking
    // for all the other, higher-precedence ones.
    // Not sure exactly how to name these better though 🤔

    private fun ternary(): Expr {
        var expr = equality()

        while (match(QUESTION)) {
            val left = equality()
            consume(COLON, "Expect ':'")
            val right = ternary()
            expr = Expr.Ternary(expr, left, right)
        }

        return expr
    }

    private fun equality(): Expr {
        var expr = comparison()

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr = term()

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expr {
        var expr = factor()

        while (match(MINUS, PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (match(SLASH, STAR)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if (match(BANG, MINUS)) {
            val operator = previous()
            return Expr.Unary(operator, unary())
        }
        return primary()
    }

    private fun primary(): Expr {
        if (match(FALSE)) {
            return Expr.Literal(false)
        } else if (match(TRUE)) {
            return Expr.Literal(true)
        } else if (match(NIL)) {
            return Expr.Literal(null)
        } else if (match(NUMBER, STRING)) {
            val literal = previous()
            return Expr.Literal(literal.literal)
        } else if (match(LEFT_PAREN)) {
            val expr = expression()
            consume(RIGHT_PAREN, "Expect ')' after expression.")
            return Expr.Grouping(expr)
        }

        throw error(peek(), "Expect expression.")
    }

    // Seems like this could provide a better interface.  What about returning
    // Token? and the caller differentiates between null and not?
    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun error(token: Token, message: String): ParseError {
        ca.kamper.klox.error(token, message)
        return ParseError()
    }

    class ParseError : Exception()

    private fun isAtEnd(): Boolean = peek().type == EOF
    private fun peek(): Token = tokens[current]
    private fun previous(): Token = tokens[current - 1]

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return

            when (peek().type) {
                CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
                else -> advance()
            }
        }
    }
}