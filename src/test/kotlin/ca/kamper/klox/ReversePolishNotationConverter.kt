package ca.kamper.klox

fun main() {
    val expr = Expr.Binary(
        left = Expr.Grouping(
            Expr.Binary(
                Expr.Literal(1),
                Token(TokenType.PLUS, "+", null, 1),
                Expr.Literal(2),
            )
        ),
        Token(TokenType.STAR, "*", null, 1),
        right = Expr.Grouping(
            Expr.Binary(
                Expr.Literal(4),
                Token(TokenType.MINUS, "-", null, 1),
                Expr.Literal(3),
            )
        ),
    )

    println(ReversePolishNotationConverter().print(expr))
}

class ReversePolishNotationConverter : Expr.Visitor<String> {

    fun print(expr: Expr) = expr.accept(this)

    override fun visitBinaryExpr(expr: Expr.Binary) =
        "${expr.left.accept(this)} ${expr.right.accept(this)} ${expr.operator.lexeme}"

    override fun visitGroupingExpr(expr: Expr.Grouping) = expr.expression.accept(this)

    override fun visitLiteralExpr(expr: Expr.Literal) = expr.value.toString()

    override fun visitLogicalExpr(expr: Expr.Logical): String {
        TODO("Not yet implemented")
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String {
        TODO("Not yet implemented")
    }

    override fun visitVariableExpr(expr: Expr.Variable): String {
        TODO("Not yet implemented")
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

    override fun visitAssignExpr(expr: Expr.Assign): String {
        TODO("Not yet implemented")
    }
}