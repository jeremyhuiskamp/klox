package ca.kamper.klox

class AstPrinter : Expr.Visitor<String> {
    override fun visitAssignExpr(expr: Expr.Assign): String {
        TODO("Not yet implemented")
    }

    override fun visitBinaryExpr(expr: Expr.Binary) =
        parenthesize(expr.operator.lexeme, expr.left, expr.right)

    override fun visitGroupingExpr(expr: Expr.Grouping) =
        parenthesize("group", expr.expression)

    // TODO: might want to quote strings here?
    override fun visitLiteralExpr(expr: Expr.Literal) =
        if (expr.value == null) "nil" else expr.value.toString()

    override fun visitLogicalExpr(expr: Expr.Logical): String {
        TODO("Not yet implemented")
    }

    override fun visitUnaryExpr(expr: Expr.Unary) =
        parenthesize(expr.operator.lexeme, expr.right)

    override fun visitVariableExpr(expr: Expr.Variable): String {
        return parenthesize("var", expr)
    }

    override fun visitFunctionCall(expr: Expr.FunctionCall): String {
        TODO("Not yet implemented")
    }

    override fun visitDotExpr(expr: Expr.Dot): String {
        TODO("Not yet implemented")
    }

    override fun visitThisExpr(expr: Expr.This): String {
        TODO("Not yet implemented")
    }

    override fun visitSuperExpr(expr: Expr.Super): String {
        TODO("Not yet implemented")
    }

    private fun parenthesize(name: String, vararg exprs: Expr) =
        StringBuilder("($name")
            .let { builder ->
                exprs.joinTo(builder, separator = " ", prefix = " ") { expr ->
                    expr.accept(this)
                }
            }
            .append(")")
            .toString()

    fun print(expression: Expr): String = expression.accept(this)

    companion object {
        fun print(expression: Expr) = AstPrinter().print(expression)
    }
}