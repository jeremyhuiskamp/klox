package ca.kamper.klox

import ca.kamper.klox.interpret.ExprInterpreter
import ca.kamper.klox.interpret.StmtInterpreter

class Interpreter(
    private val printer: (String) -> Unit = ::println,
) {
    private val globals = Environment.global()
    private var environment = globals
    private val locals = mutableMapOf<Expr, Int>()

    internal fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

    private val exprInterpreter = object : ExprInterpreter() {
        override val globals: Environment
            get() = this@Interpreter.globals

        override val environment: Environment
            get() = this@Interpreter.environment

        override fun evaluate(expr: Expr): Any? =
            this@Interpreter.evaluate(expr)

        override fun execute(environment: Environment, stmt: Stmt) {
            withEnvironment(environment) {
                stmt.accept(stmtInterpreter)
            }
        }

        override val locals: Map<Expr, Int>
            get() = this@Interpreter.locals
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

    fun interpret(
        stmts: List<Stmt>,
        reportStaticError: (Token, String) -> Unit = ::error,
        reportRuntimeError: (RuntimeError) -> Unit = ::runtimeError,
    ) {
        val resolutionError = resolve(stmts, reportStaticError)
        if (resolutionError) return

        try {
            stmts.forEach { it.accept(this.stmtInterpreter) }
        } catch (e: RuntimeError) {
            reportRuntimeError(e)
        }
    }

    private fun resolve(
        stmts: List<Stmt>,
        reportStaticError: (Token, String) -> Unit,
    ): Boolean {
        var errors = false
        val doReport = { token: Token, msg: String ->
            errors = true
            reportStaticError(token, msg)
        }
        Resolver(doReport, ::resolve).resolve(stmts)
        return errors
    }
}