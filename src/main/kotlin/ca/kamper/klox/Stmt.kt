package ca.kamper.klox

interface Stmt {
    interface Visitor<R> {
        fun visitExpressionStmt(stmt: Expression): R
        fun visitPrintStmt(stmt: Print): R
        fun visitVarStmt(stmt: Var): R
        fun visitBlockStmt(stmt: Block): R
    }

    fun <R> accept(visitor: Visitor<R>): R

    data class Expression(
        val expr: Expr,
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>) =
            visitor.visitExpressionStmt(this)
    }

    data class Print(
        val expr: Expr,
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>) =
            visitor.visitPrintStmt(this)
    }

    data class Var(
        val name: Token,
        val initializer: Expr?,
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>) =
            visitor.visitVarStmt(this)
    }

    data class Block(
        val stmts: List<Stmt>,
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>) =
            visitor.visitBlockStmt(this)
    }
}