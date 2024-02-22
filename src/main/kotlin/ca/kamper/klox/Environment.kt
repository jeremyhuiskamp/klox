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
            return enclosing[name]
        }
        throw RuntimeError(
            name,
            "Undefined variable '${name.lexeme}'.",
        )
    }

    fun getAt(distance: Int, name: Token): Any? =
        // go directly to `values` because we shouldn't need to look into parents:
        requireAncestor(distance, name).values[name.lexeme]

    private fun requireAncestor(distance: Int, name: Token): Environment =
        ancestor(distance) ?: throw RuntimeError(
            name,
            "Compiler error: reference non-existent ancestor environment $distance levels higher"
        )

    private fun ancestor(distance: Int): Environment? =
        if (distance <= 0) this
        else enclosing?.ancestor(distance - 1)

    fun assignAt(distance: Int, name: Token, value: Any?) {
        requireAncestor(distance, name).values[name.lexeme] = value
    }

    companion object {
        fun global(): Environment =
            Environment().apply {
                define("clock", ClockGlobal)
                define("toString", ToStringGlobal)
            }
    }
}