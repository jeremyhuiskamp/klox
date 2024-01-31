package ca.kamper.klox

import ca.kamper.klox.TokenType.*

class Parser(
    private val tokens: List<Token>,
    private val reportError: (Token, String) -> Unit = ::error,
) {
    private var currentToken = 0
    private var nestedFunctionCount = 0

    fun parseExpr() = try {
        expression()
    } catch (e: ParseError) {
        null
    }

    fun parse(): List<Stmt> = declarations()

    private fun declarations(
        hasNext: () -> Boolean = { !isAtEnd() },
    ) =
        object : Iterator<Stmt?> {
            override fun hasNext() = hasNext()
            override fun next() = declaration()
        }
            .asSequence()
            .filterNotNull() // book doesn't have this?
            .toList()

    private fun declaration(): Stmt? {
        try {
            if (match(VAR)) return varDeclaration()

            return statement()
        } catch (e: ParseError) {
            synchronize()
            return null
        }
    }

    private fun varDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect variable name.")

        val initializer = if (match(EQUAL)) expression() else null

        consume(SEMICOLON, "Expect ';' after variable declaration.")

        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt {
        if (match(FOR)) {
            return forStatement()
        }

        if (match(WHILE)) {
            return whileStatement()
        }

        if (match(IF)) {
            return ifStatement()
        }

        if (match(PRINT)) {
            return printStatement()
        }

        if (match(LEFT_BRACE)) {
            return blockStatement()
        }

        if (match(FUN)) {
            return declareFunctionStatement()
        }

        if (match(RETURN)) {
            return returnStmt()
        }

        return expressionStatement()
    }

    private fun returnStmt(): Stmt {
        val returnToken = previous()
        if (nestedFunctionCount < 1) {
            error(returnToken, "Cannot 'return' outside of function body.")
        }
        val value =
            if (match(SEMICOLON)) null
            else expressionStatement().expr
        return Stmt.Return(returnToken, value)
    }

    private fun declareFunctionStatement(): Stmt {
        val name = consume(IDENTIFIER, "Expect function name.")
        consume(LEFT_PAREN, "Expect '(' after function name.")

        val parameters = mutableListOf<Token>()
        while (true) {
            if (match(RIGHT_PAREN)) {
                break
            }
            if (parameters.size >= 255) {
                error(peek(), "Can't have more than 255 parameters.")
            }
            parameters.add(consume(IDENTIFIER, "Expect parameter name."))
            if (match(COMMA)) {
                continue
            }
            consume(RIGHT_PAREN, "Expect ')' after parameter list.")
            break
        }
        val body = try {
            nestedFunctionCount++
            statement()
        } finally {
            nestedFunctionCount--
        }

        return Stmt.FunctionDeclaration(name, parameters, body)
    }

    private fun forStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'for'.")

        val initializer =
            if (match(VAR)) varDeclaration()
            else if (!match(SEMICOLON)) expressionStatement()
            else null

        // alternative: have the condition be non-null, and
        // fall back on `true` instead of null
        val condition =
            if (!match(SEMICOLON)) expressionStatement().expr
            else null

        val increment =
            if (!check(RIGHT_PAREN)) expression()
            else null
        consume(RIGHT_PAREN, "Expect ')' after for clauses.")

        val body = statement()

        return Stmt.For(initializer, condition, increment, body)
    }

    private fun whileStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after while condition.")

        val body = statement()

        return Stmt.While(condition, body)
    }

    private fun ifStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after if condition.")

        val thenBranch = statement()
        val elseBranch = if (match(ELSE)) statement() else null

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun blockStatement(): Stmt {
        val stmts = declarations {
            !check(RIGHT_BRACE) && !isAtEnd()
        }
        consume(RIGHT_BRACE, "Expect '}' after block.")
        return Stmt.Block(stmts)
    }

    private fun printStatement(): Stmt {
        val expr = expression()
        consume(SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(expr)
    }

    private fun expressionStatement(): Stmt.Expression {
        val expr = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun expression(): Expr = assignment()

    // The method names are a bit weird here.  Eg, we start by looking
    // for an assignment expression, but we don't actually care if there
    // is one, we're just checking for assignment operators after looking
    // for all the other, higher-precedence ones.
    // Not sure exactly how to name these better though 🤔, "assignmentOrHigherPrecedence()"?

    private fun assignment(): Expr {
        val expr = or()

        if (match(EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            }

            error(equals, "Invalid assignment target.")
        }

        return expr
    }

    private fun or(): Expr {
        var expr = and()
        while (match(OR)) {
            val token = previous()
            expr = Expr.Logical(expr, token, and())
        }
        return expr
    }

    private fun and(): Expr {
        var expr = equality()
        while (match(AND)) {
            val token = previous()
            expr = Expr.Logical(expr, token, equality())
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
        return functionCall()
    }

    private fun functionCall(): Expr {
        var expr = primary()
        while (match(LEFT_PAREN)) {
            expr = finishFunctionCall(expr)
        }
        return expr
    }

    private fun finishFunctionCall(expr: Expr): Expr {
        val arguments = mutableListOf<Expr>()

        while (true) {
            if (match(RIGHT_PAREN)) {
                break
            }
            if (arguments.size >= 255) {
                error(peek(), "Can't have more than 255 arguments.")
            }
            arguments.add(expression())
            if (match(COMMA)) {
                continue
            }
            consume(RIGHT_PAREN, "Expect ')' to close function call.")
            break
        }
        return Expr.FunctionCall(expr, previous(), arguments)
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
        } else if (match(IDENTIFIER)) {
            return Expr.Variable(previous())
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
        if (!isAtEnd()) currentToken++
        return previous()
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun error(token: Token, message: String): ParseError {
        reportError(token, message)
        return ParseError()
    }

    class ParseError : Exception()

    private fun isAtEnd(): Boolean = peek().type == EOF
    private fun peek(): Token = tokens[currentToken]
    private fun previous(): Token = tokens[currentToken - 1]

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