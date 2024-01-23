package ca.kamper.klox

import ca.kamper.klox.interpret.ClockGlobal
import ca.kamper.klox.interpret.ToStringGlobal

class Environment(private val enclosing: Environment? = null) {
    private val values = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
        } else if (enclosing != null) {
            enclosing.assign(name, value)
        } else {
            throw RuntimeError(
                name,
                "Undefined variable '${name.lexeme}'.",
            )
        }
    }

    operator fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
            return values[name.lexeme]
        }
        if (enclosing != null) {
            return enclosing.get(name)
        }
        throw RuntimeError(
            name,
            "Undefined variable '${name.lexeme}'.",
        )
    }

    companion object {
        fun global(): Environment =
            Environment().apply {
                define("clock", ClockGlobal)
                define("toString", ToStringGlobal)
            }
    }
}