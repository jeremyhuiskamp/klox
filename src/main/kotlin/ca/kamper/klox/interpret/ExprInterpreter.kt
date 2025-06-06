package ca.kamper.klox.interpret

import ca.kamper.klox.*

abstract class ExprInterpreter : Expr.Visitor<Any?> {
    // TODO: there are too many hooks back into Interpreter here
    // need a better abstraction
    abstract val globals: Environment
    abstract val environment: Environment
    abstract fun evaluate(expr: Expr): Any?
    abstract fun execute(environment: Environment, stmt: Stmt)
    abstract val locals: Map<Expr, Int>

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

    override fun visitVariableExpr(expr: Expr.Variable) = lookupVariable(expr.name, expr)

    private fun lookupVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr]
        return if (distance != null) {
            environment.getAt(distance, name)
        } else {
            globals[name]
        }
    }

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

        // TODO: the allocation of an object here seems excessive
        // For the small number of target types, it might be faster to
        // seal the interface and use a when block?
        // This would also make it easier to avoid conflicts with
        // the multiple levels of named expressions.
        expr.target.accept(object : Expr.AssignmentTarget.Visitor<Unit> {
            override fun visitVariableExpr(variable: Expr.Variable) {
                val distance = locals[variable]
                if (distance != null) {
                    environment.assignAt(distance, expr.name, value)
                } else {
                    globals.assign(expr.name, value)
                }
            }

            override fun visitDotExpr(dot: Expr.Dot) {
                val targetObject = evaluate(dot.left)
                if (targetObject !is LoxObject) {
                    // would be helpful to log what the targetObject actually is?
                    throw RuntimeError(expr.name, "Cannot assign property to non-object")
                }

                targetObject[dot.right] = value
            }
        })

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

    override fun visitDotExpr(expr: Expr.Dot): Any? {
        val left = evaluate(expr.left)
                as? LoxObject
            ?: throw RuntimeError(expr.right, "Can only access members of a class")

        return left[expr.right]
    }

    override fun visitThisExpr(expr: Expr.This) = lookupVariable(expr.keyword, expr)

    override fun visitSuperExpr(expr: Expr.Super): Any? {
        // - do we need the resolved versions of super and this?
        // - can we just look up the env stack?
        // - for type safety, could/should we have a separate stack for this and super?
        val distance = locals[expr] ?: throw RuntimeError(
            expr.keyword, "Reference to 'super' in a class with no parent."
        )
        val superClass = environment.getAt(distance, expr.keyword)
                as? LoxClass
            ?: throw RuntimeError(
                expr.keyword, "Interpreter bug: 'super' doesn't resolve to a class"
            )

        // Hmm, here we don't have a `this` token, we're just trying to get a
        // reference to bind methods to.  We want the token for error messages
        // in case the thing isn't found, which is possible for user code, but
        // would be an interpreter bug in this case.
        // Major hax, repurpose the super token for this:
        val thisToken = expr.keyword.copy(lexeme = "this")
        val instance = environment.getAt(distance - 1, thisToken)
                as? LoxObject
            ?: throw RuntimeError(
                thisToken, "Interpreter bug: 'this' doesn't resolve to an object"
            )

        val method = superClass.findMethod(expr.method.lexeme) ?: throw RuntimeError(
            expr.method,
            "Undefined property '${expr.method.lexeme}'."
        )

        return method.bindTo(instance)
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