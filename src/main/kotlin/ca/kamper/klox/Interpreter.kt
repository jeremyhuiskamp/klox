package ca.kamper.klox

import ca.kamper.klox.interpret.ExprInterpreter
import ca.kamper.klox.interpret.StmtInterpreter

class Interpreter(
    private val printer: (String) -> Unit = ::println,
) {
    private var environment = Environment.global()

    private val exprInterpreter = object : ExprInterpreter() {
        override val environment: Environment
            get() = this@Interpreter.environment

        override fun evaluate(expr: Expr): Any? =
            this@Interpreter.evaluate(expr)

        override fun execute(environment: Environment, stmt: Stmt) {
            withEnvironment(environment) {
                stmt.accept(stmtInterpreter)
            }
        }
    }

    private val stmtInterpreter = object : StmtInterpreter(printer) {
        override val environment: Environment
            get() = this@Interpreter.environment

        override fun evaluate(expr: Expr): Any? =
            this@Interpreter.evaluate(expr)

        override fun withNewEnvironment(block: () -> Unit) {
            withEnvironment(Environment(environment), block)
        }
    }

    private fun withEnvironment(environment: Environment, block: () -> Unit) {
        val previousEnvironment = this.environment
        try {
            this.environment = environment
            block()
        } finally {
            this.environment = previousEnvironment
        }
    }

    fun evaluate(expr: Expr): Any? = expr.accept(exprInterpreter)

    fun interpret(stmts: List<Stmt>, report: (RuntimeError) -> Unit) {
        try {
            stmts.forEach { it.accept(this.stmtInterpreter) }
        } catch (e: RuntimeError) {
            report(e)
        }
    }
}