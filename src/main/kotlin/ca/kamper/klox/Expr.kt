package ca.kamper.klox

interface Expr {
    // In Kotlin, we could also seal Expr and then
    // use a when (expr) {} block instead of implementing
    // Visitor.  This is syntactically nice, but is
    // still a chained series of type checks, so it
    // has the same performance problems mentioned
    // in the book.
    interface Visitor<R> {
        fun visitAssignExpr(expr: Assign): R
        fun visitBinaryExpr(expr: Binary): R
        fun visitGroupingExpr(expr: Grouping): R
        fun visitLiteralExpr(expr: Literal): R
        fun visitLogicalExpr(expr: Logical): R
        fun visitUnaryExpr(expr: Unary): R
        fun visitVariableExpr(expr: Variable): R
        fun visitFunctionCall(expr: FunctionCall): R
        fun visitDotExpr(expr: Dot): R
        fun visitThisExpr(expr: This): R
    }

    fun <R> accept(visitor: Visitor<R>): R


    interface AssignmentTarget : Expr {
        interface Visitor<R> {
            fun visitVariableExpr(variable: Variable): R
            fun visitDotExpr(dot: Dot): R
        }

        fun <R> accept(visitor: Visitor<R>): R
    }

    // Implementations should *not* be data classes because we use
    // them as map keys and want them to be unique identity, not value.

    class Assign(
        val name: Token, // could we have AssignmentTarget provide this?
        val target: AssignmentTarget,
        val value: Expr,
    ) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitAssignExpr(this)
    }

    class Binary(
        val left: Expr,
        val operator: Token,
        val right: Expr,
    ) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitBinaryExpr(this)
    }

    class Grouping(
        val expression: Expr,
    ) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitGroupingExpr(this)
    }

    class Literal(
        val value: Any?,
    ) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitLiteralExpr(this)
    }

    class Unary(
        val operator: Token,
        val right: Expr,
    ) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitUnaryExpr(this)
    }

    class Variable(
        // only TokenType.IDENTIFIER would be legal here; could we enforce that?
        val name: Token,
    ) : Expr, AssignmentTarget {
        override fun <R> accept(visitor: AssignmentTarget.Visitor<R>) =
            visitor.visitVariableExpr(this)

        override fun <R> accept(visitor: Visitor<R>) = visitor.visitVariableExpr(this)
    }

    class Logical(
        val left: Expr,
        val operator: Token,
        val right: Expr,
    ) : Expr {
        override fun <R> accept(visitor: Visitor<R>) = visitor.visitLogicalExpr(this)
    }

    class FunctionCall(
        val function: Expr,
        val token: Token,
        val arguments: List<Expr>,
    ) : Expr {
        override fun <R> accept(visitor: Visitor<R>) = visitor.visitFunctionCall(this)
    }

    class Dot(
        val left: Expr,
        val right: Token, // must be an IDENTIFIER
    ) : Expr, AssignmentTarget {
        override fun <R> accept(visitor: AssignmentTarget.Visitor<R>) =
            visitor.visitDotExpr(this)

        override fun <R> accept(visitor: Visitor<R>) = visitor.visitDotExpr(this)
    }

    // should this also support `super`?
    class This(val token: Token) : Expr {
        override fun <R> accept(visitor: Visitor<R>) = visitor.visitThisExpr(this)
    }
}
