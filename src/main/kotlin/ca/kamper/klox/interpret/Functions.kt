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

    fun bindTo(obj: LoxObject): LoxFunction {
        // This is sort of python-style: the class members are not
        // in scope, but must be references through this/self.
        // Java-style would allow that, but also pump all the class
        // members into the environment.  But that might depend on
        // static declaration of the members?  Otherwise our resolver
        // won't know what to do with references.
        val classEnv = Environment(closure)
        classEnv.define("this", obj)
        return LoxFunction(name, parameters, body, classEnv)
    }
}

class Return(val value: Any?) : RuntimeException(null, null, false, false) {
    companion object {
        fun trigger(value: Any?) {
            throw Return(value)
        }
    }
}