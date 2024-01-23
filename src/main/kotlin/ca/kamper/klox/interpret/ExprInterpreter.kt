package ca.kamper.klox.interpret

import ca.kamper.klox.*

abstract class ExprInterpreter : Expr.Visitor<Any?> {
    abstract val environment: Environment
    abstract fun evaluate(expr: Expr): Any?
    abstract fun execute(environment: Environment, stmt: Stmt)

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.GREATER -> withDoubles(expr.operator, left, right) { (l, r) ->
                l > r
            }

            TokenType.GREATER_EQUAL -> withDoubles(expr.operator, left, right) { (l, r) ->
                l >= r
            }

            TokenType.LESS -> withDoubles(expr.operator, left, right) { (l, r) ->
                l < r
            }

            TokenType.LESS_EQUAL -> withDoubles(expr.operator, left, right) { (l, r) ->
                l <= r
            }

            TokenType.MINUS -> withDoubles(expr.operator, left, right) { (l, r) ->
                l - r
            }

            TokenType.PLUS ->
                when {
                    left is Double && right is Double -> left + right
                    left is String && right is String -> left + right
                    else -> throw RuntimeError(
                        expr.operator,
                        "Operands must be two numbers or two strings.",
                    )
                }

            TokenType.SLASH -> withDoubles(expr.operator, left, right) { (l, r) ->
                l / r
            }

            TokenType.STAR -> withDoubles(expr.operator, left, right) { (l, r) ->
                l * r
            }

            TokenType.BANG_EQUAL -> !isEqual(left, right)

            TokenType.EQUAL_EQUAL -> isEqual(left, right)

            else -> null // unreachable?
        }
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) = evaluate(expr.expression)

    override fun visitLiteralExpr(expr: Expr.Literal) = expr.value

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)
        return when (expr.operator.type) {
            TokenType.MINUS -> withDoubles(expr.operator, right) { (r) ->
                -r
            }

            TokenType.BANG -> !isTruthy(right)
            else -> null // unreachable?
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable) = environment[expr.name]

    override fun visitFunctionCall(expr: Expr.FunctionCall): Any? {
        val callable = evaluate(expr.function)
        if (callable !is LoxCallable) {
            throw RuntimeError(expr.token, "Can only call functions and classes.")
        }
        val argumentValues = expr.arguments.map { evaluate(it) }
        return callable.call(::execute, expr.token, argumentValues)
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)
        environment.assign(expr.name, value)
        return value
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)
        when (expr.operator.type) {
            TokenType.OR -> if (isTruthy(left)) return left
            else -> if (!isTruthy(left)) return left
        }
        return evaluate(expr.right)
    }

    override fun visitLambdaExpr(expr: Expr.Lambda): Any? {
        return LoxLambda(expr.parameters, expr.body, environment)
    }

    companion object {
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
    }
}