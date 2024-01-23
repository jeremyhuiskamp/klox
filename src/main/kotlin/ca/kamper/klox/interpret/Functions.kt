package ca.kamper.klox.interpret

import ca.kamper.klox.Environment
import ca.kamper.klox.RuntimeError
import ca.kamper.klox.Stmt
import ca.kamper.klox.Token

fun interface LoxCallable {
    fun call(interpreter: (Environment, Stmt) -> Unit, token: Token, arguments: List<Any?>): Any?
}

val ClockGlobal = LoxCallable { _, _, _ ->
    // TODO: validate 0 arguments
    System.currentTimeMillis() / 1000.0
}

val ToStringGlobal = LoxCallable { _, token, arguments ->
    if (arguments.size != 1) {
        throw RuntimeError(token, "toString() takes exactly one parameter.")
    }
    stringify(arguments[0])
}

class LoxFunction(
    val name: Token,
    val parameters: List<Token>,
    val body: Stmt,
    val closure: Environment,
) : LoxCallable {
    override fun call(interpreter: (Environment, Stmt) -> Unit, token: Token, arguments: List<Any?>): Any? {
        if (arguments.size != parameters.size) {
            throw RuntimeError(token, "Function ${name.lexeme} takes ${parameters.size} arguments.")
        }

        val environment = Environment(closure)
        arguments.zip(parameters).forEach { (arg, param) ->
            environment.define(param.lexeme, arg)
        }

        try {
            interpreter(environment, body)
            return null
        } catch (r: Return) {
            return r.value
        }
    }
}

class LoxLambda(
    val parameters: List<Token>,
    val body: Stmt,
    val closure: Environment,
) : LoxCallable {
    // TODO: deduplicate with LoxFunction?
    override fun call(interpreter: (Environment, Stmt) -> Unit, token: Token, arguments: List<Any?>): Any? {
        if (arguments.size != parameters.size) {
            throw RuntimeError(token, "Lambda takes ${parameters.size} arguments.")
        }

        val environment = Environment(closure)
        arguments.zip(parameters).forEach { (arg, param) ->
            environment.define(param.lexeme, arg)
        }

        try {
            interpreter(environment, body)
            return null
        } catch (r: Return) {
            return r.value
        }
    }
}

class Return(val value: Any?) : RuntimeException(null, null, false, false)

fun triggerReturn(value: Any?) {
    throw Return(value)
}