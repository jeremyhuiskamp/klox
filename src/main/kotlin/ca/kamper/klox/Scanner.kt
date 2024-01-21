package ca.kamper.klox

import ca.kamper.klox.TokenType.*

class Scanner(
    private val code: String,
    private val reportError: (Int, String) -> Unit = ::error,
) {
    private val tokens = mutableListOf<Token>()
    private var start = 0
    private var current = 0
    private var line = 1

    fun scanTokens(): List<Token> {
        while (!atEnd()) {
            start = current
            scanToken()
        }
        tokens.add(Token(EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        when (val c = advance()) {
            '(' -> addToken(LEFT_PAREN)
            ')' -> addToken(RIGHT_PAREN)
            '{' -> addToken(LEFT_BRACE)
            '}' -> addToken(RIGHT_BRACE)
            ',' -> addToken(COMMA)
            '.' -> addToken(DOT)
            '-' -> addToken(MINUS)
            '+' -> addToken(PLUS)
            ';' -> addToken(SEMICOLON)
            '*' -> addToken(STAR)
            '!' -> addToken(if (match('=')) BANG_EQUAL else BANG)
            '=' -> addToken(if (match('=')) EQUAL_EQUAL else EQUAL)
            '<' -> addToken(if (match('=')) LESS_EQUAL else LESS)
            '>' -> addToken(if (match('=')) GREATER_EQUAL else GREATER)
            '/' ->
                if (match('/')) {
                    while (peek() != '\n' && !atEnd()) advance()
                } else {
                    addToken(SLASH)
                }

            ' ', '\r', '\t' -> {}
            '\n' -> line++
            '"' -> string()

            else ->
                if (isDigit(c)) {
                    number()
                } else if (isAlpha(c)) {
                    identifier()
                } else {
                    reportError(line, "Unexpected character.")
                }
        }
    }

    private fun identifier() {
        while (isAlphaNumeric(peek())) advance()
        val text = code.substring(start, current)
        val type = keywords.getOrDefault(text, IDENTIFIER)
        addToken(type)
    }

    // TODO: make this more unicode friendly?
    private fun isAlpha(c: Char): Boolean =
        c in 'a'..'z' ||
                c in 'A'..'Z' ||
                c == '|'

    private fun isAlphaNumeric(c: Char) = isAlpha(c) || isDigit(c)

    private fun isDigit(c: Char): Boolean = c in '0'..'9'

    private fun number() {
        while (isDigit(peek())) advance()

        if (peek() == '.' && isDigit(peekNext())) {
            advance() // consume '.'

            while (isDigit(peek())) advance()
        }

        addToken(NUMBER, code.substring(start, current).toDouble())
    }

    private fun peekNext(): Char =
        if (current + 1 < code.length) code[current + 1]
        else '\u0000'

    private fun string() {
        while (peek() != '"' && !atEnd()) {
            if (peek() == '\n') line++
            advance()
        }

        if (atEnd()) {
            reportError(line, "Unterminated string.")
            return
        }

        advance()

        val value = code.substring(start + 1, current - 1)
        addToken(STRING, value)
    }

    private fun peek(): Char {
        return if (atEnd()) '\u0000' else code[current]
    }

    private fun match(expected: Char): Boolean {
        if (atEnd()) return false
        if (code[current] != expected) return false
        current++
        return true
    }

    private fun addToken(type: TokenType, literal: Any? = null) {
        val text = code.substring(start, current)
        tokens.add(Token(type, text, literal, line))
    }

    private fun advance(): Char = code[current++]

    private fun atEnd(): Boolean {
        return current >= code.length
    }

    companion object {
        private val keywords = mapOf(
            "and" to AND,
            "class" to CLASS,
            "else" to ELSE,
            "false" to FALSE,
            "for" to FOR,
            "fun" to FUN,
            "if" to IF,
            "nil" to NIL,
            "or" to OR,
            "print" to PRINT,
            "return" to RETURN,
            "super" to SUPER,
            "this" to THIS,
            "true" to TRUE,
            "var" to VAR,
            "while" to WHILE,
        )
    }
}