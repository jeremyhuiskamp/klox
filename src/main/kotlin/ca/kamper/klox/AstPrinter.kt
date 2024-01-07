package ca.kamper.klox

class AstPrinter : Expr.Visitor<String> {
    override fun visitAssignExpr(expr: Expr.Assign): String {
        TODO("Not yet implemented")
    }

    override fun visitBinaryExpr(expr: Expr.Binary) =
        parenthesize(expr.operator.lexeme, expr.left, expr.right)

    override fun visitCallExpr(expr: Expr.Call): String {
        TODO("Not yet implemented")
    }

    override fun visitGetExpr(expr: Expr.Get): String {
        TODO("Not yet implemented")
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) =
        parenthesize("group", expr.expression)

    override fun visitLiteralExpr(expr: Expr.Literal) =
        if (expr.value == null) "nil" else expr.value.toString()

    override fun visitUnaryExpr(expr: Expr.Unary) =
        parenthesize(expr.operator.lexeme, expr.right)

    override fun visitTernaryExpr(expr: Expr.Ternary) =
        parenthesize("?:", expr.condition, expr.left, expr.right)

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