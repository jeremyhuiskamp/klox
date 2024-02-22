package ca.kamper.klox.interpret

import ca.kamper.klox.Environment
import ca.kamper.klox.Expr
import ca.kamper.klox.Stmt

abstract class StmtInterpreter(
    private val printer: (String) -> Unit = ::println,
) : Stmt.Visitor<Unit> {
    protected abstract fun evaluate(expr: Expr): Any?
    protected abstract val environment: Environment
    protected abstract fun withNewEnvironment(block: () -> Unit)

    private fun execute(stmt: Stmt) = stmt.accept(this)

    private fun executeBlock(stmts: List<Stmt>) {
        withNewEnvironment {
            stmts.forEach(::execute)
        }
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expr)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expr)
        printer(stringify(value))
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

    override fun visitFunctionDeclarationStmt(stmt: Stmt.FunctionDeclaration) {
        environment.define(stmt.name.lexeme, LoxFunction(stmt.name, stmt.parameters, stmt.body, environment))
    }

    override fun visitReturn(stmt: Stmt.Return) {
        // Might be interesting to differentiate between returning
        // no value and returning a lox null?
        val value = stmt.value?.let { evaluate(it) }
        Return.trigger(value)
    }
}