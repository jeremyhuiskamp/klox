package ca.kamper.klox

interface Stmt {
    interface Visitor<R> {
        fun visitExpressionStmt(stmt: Expression): R
        fun visitPrintStmt(stmt: Print): R
        fun visitVarStmt(stmt: Var): R
        fun visitBlockStmt(stmt: Block): R
        fun visitIfStmt(stmt: If): R
        fun visitWhileStmt(stmt: While): R
        fun visitForStmt(stmt: For): R
        fun visitFunctionDeclarationStmt(stmt: FunctionDeclaration): R
        fun visitReturn(stmt: Return): R
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

    data class If(
        val condition: Expr,
        val thenBranch: Stmt,
        val elseBranch: Stmt?,
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>) =
            visitor.visitIfStmt(this)
    }

    data class While(
        val condition: Expr,
        val body: Stmt,
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>) =
            visitor.visitWhileStmt(this)
    }

    data class For(
        val initializer: Stmt?,
        val condition: Expr?,
        val increment: Expr?,
        val body: Stmt,
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>) =
            visitor.visitForStmt(this)
    }

    data class FunctionDeclaration(
        val name: Token,
        val parameters: List<Token>,
        val body: Stmt,
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>) =
            visitor.visitFunctionDeclarationStmt(this)
    }

    data class Return(
        val token: Token,
        val value: Expr?,
    ) : Stmt {
        override fun <R> accept(visitor: Visitor<R>) =
            visitor.visitReturn(this)
    }
}