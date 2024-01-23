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
    }

    fun <R> accept(visitor: Visitor<R>): R

    data class Assign(
        val name: Token,
        val value: Expr,
    ) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitAssignExpr(this)
    }

    data class Binary(
        val left: Expr,
        val operator: Token,
        val right: Expr,
    ) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitBinaryExpr(this)
    }

    data class Grouping(
        val expression: Expr,
    ) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitGroupingExpr(this)
    }

    data class Literal(
        val value: Any?,
    ) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitLiteralExpr(this)
    }

    data class Unary(
        val operator: Token,
        val right: Expr,
    ) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitUnaryExpr(this)
    }

    data class Variable(
        // only TokenType.IDENTIFIER would be legal here; could we enforce that?
        val name: Token,
    ) : Expr {
        override fun <R> accept(visitor: Visitor<R>) = visitor.visitVariableExpr(this)
    }

    data class Logical(
        val left: Expr,
        val operator: Token,
        val right: Expr,
    ) : Expr {
        override fun <R> accept(visitor: Visitor<R>) = visitor.visitLogicalExpr(this)
    }

    data class FunctionCall(
        val function: Expr,
        val token: Token,
        val arguments: List<Expr>,
    ) : Expr {
        override fun <R> accept(visitor: Visitor<R>) = visitor.visitFunctionCall(this)
    }
}
