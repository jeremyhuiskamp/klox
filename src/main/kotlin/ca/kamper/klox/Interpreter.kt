package ca.kamper.klox

import ca.kamper.klox.TokenType.*

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    private var environment = Environment()

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            GREATER -> withDoubles(expr.operator, left, right) { (l, r) ->
                l > r
            }

            GREATER_EQUAL -> withDoubles(expr.operator, left, right) { (l, r) ->
                l >= r
            }

            LESS -> withDoubles(expr.operator, left, right) { (l, r) ->
                l < r
            }

            LESS_EQUAL -> withDoubles(expr.operator, left, right) { (l, r) ->
                l <= r
            }

            MINUS -> withDoubles(expr.operator, left, right) { (l, r) ->
                l - r
            }

            PLUS ->
                when {
                    left is Double && right is Double -> left + right
                    left is String && right is String -> left + right
                    else -> throw RuntimeError(
                        expr.operator,
                        "Operands must be two numbers or two strings.",
                    )
                }

            SLASH -> withDoubles(expr.operator, left, right) { (l, r) ->
                l / r
            }

            STAR -> withDoubles(expr.operator, left, right) { (l, r) ->
                l * r
            }

            BANG_EQUAL -> !isEqual(left, right)

            EQUAL_EQUAL -> isEqual(left, right)

            else -> null // unreachable?
        }
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) = evaluate(expr.expression)

    override fun visitLiteralExpr(expr: Expr.Literal) = expr.value

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)
        return when (expr.operator.type) {
            MINUS -> withDoubles(expr.operator, right) { (r) ->
                -r
            }

            BANG -> !isTruthy(right)
            else -> null // unreachable?
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable) = environment[expr.name]

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)
        environment.assign(expr.name, value)
        return value
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)
        when (expr.operator.type) {
            OR -> if (isTruthy(left)) return left
            else -> if (!isTruthy(left)) return left
        }
        return evaluate(expr.right)
    }

    private fun isEqual(a: Any?, b: Any?): Boolean {
        // special case to properly compare NaN!
        // https://kotlinlang.org/docs/equality.html#floating-point-numbers-equality
        // This looks the same as the non-special case,
        // but because both are known to be Doubles, the
        // comparison is done differently!
        if (a is Double && b is Double) {
            return a == b
        }
        return a == b
    }

    private fun isTruthy(value: Any?) =
        when (value) {
            null -> false
            is Boolean -> value
            else -> true
        }

    private fun withDoubles(
        operator: Token,
        vararg operands: Any?,
        block: (List<Double>) -> Any?,
    ): Any? {
        if (!operands.all { it is Double }) {
            throw RuntimeError(operator, "Operands must be numbers.")
        }
        return block(operands.map { it as Double })
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expr)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expr)
        println(stringify(value))
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        val value = stmt.initializer?.let { evaluate(it) }
        environment.define(stmt.name.lexeme, value)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.stmts)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
    }

    override fun visitForStmt(stmt: Stmt.For) {
        // pretty sure the loop var is supposed to be scoped to the loop only
        withNewEnvironment {
            stmt.initializer?.let { execute(it) }
            // given that we don't support a `break` keyword, how would we ever
            // exit a loop with no condition?
            while (stmt.condition == null || isTruthy(evaluate(stmt.condition))) {
                execute(stmt.body)
                stmt.increment?.let { evaluate(it) }
            }
        }
    }

    private fun executeBlock(stmts: List<Stmt>) {
        // Book has this method accept the new environment
        // but we're hard coding it to a new layer below.
        // Do we ever need to support anything different?
        withNewEnvironment {
            stmts.forEach(::execute)
        }
    }

    private fun withNewEnvironment(block: () -> Unit) {
        // Alternatives to this approach:
        // - have Environment manage a stack itself!
        //   - but probably still requires push/pop commands from here?
        // - create a new Interpreter for the new Environment
        // - pass the Environment on the stack
        val previousEnvironment = this.environment
        try {
            this.environment = Environment(this.environment)
            block()
        } finally {
            this.environment = previousEnvironment
        }
    }

    fun evaluate(expr: Expr) = expr.accept(this)

    private fun execute(stmt: Stmt) = stmt.accept(this)

    fun interpret(stmts: List<Stmt>, report: (RuntimeError) -> Unit) {
        try {
            stmts.forEach { execute(it) }
        } catch (e: RuntimeError) {
            report(e)
        }
    }

    fun interpret(expr: Expr, report: (RuntimeError) -> Unit): String? {
        try {
            val v = evaluate(expr)
            return stringify(v)
        } catch (e: RuntimeError) {
            report(e)
        }
        return null
    }

    private fun stringify(value: Any?): String {
        if (value == null) return "nil"
        val s = value.toString()
        if (value is Double && s.endsWith(".0")) {
            return s.dropLast(2)
        }
        return s
    }

    override fun visitCallExpr(expr: Expr.Call) = TODO("Not yet implemented")
    override fun visitGetExpr(expr: Expr.Get) = TODO("Not yet implemented")
}