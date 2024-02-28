package ca.kamper.klox.interpret

import ca.kamper.klox.Environment
import ca.kamper.klox.Stmt
import ca.kamper.klox.Token

class LoxClass(
    internal val name: String,
    private val superClass: LoxClass?,
    methods: List<LoxFunction>,
) : LoxCallable {
    internal val methods = methods.associateBy { it.name.lexeme }

    internal fun findMethod(name: String): LoxFunction? {
        methods[name]?.let { return it }
        return superClass?.findMethod(name)
    }

    override fun call(interpreter: (Environment, Stmt) -> Unit, token: Token, arguments: List<Any?>): Any {
        val obj = LoxObject(this)
        // What would we do with a return value from init?
        // Javascript gets *very* weird:
        // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Classes/Private_properties#returning_overriding_object
        // Kotlin simply dictates that constructors have a Unit return type
        // and the type system takes over from there
        methods["init"]?.bindTo(obj)?.call(interpreter, token, arguments)
        return obj
    }

    override fun toString(): String = "class $name"
}

class LoxObject(
    private val klass: LoxClass,
) {
    private val properties = mutableMapOf<String, Any?>()

    // TODO: print properties
    override fun toString(): String = "object ${klass.name}{}"

    /**
     * Get either a method or a property.
     * Behaviour not yet decided should there be a naming clash.
     */
    operator fun get(name: Token): Any? {
        // Here we're allowing properties to hide methods.
        // If someone put a function in as a property, it
        // would look like a method, but wouldn't bind to this.
        if (properties.containsKey(name.lexeme)) {
            return properties[name.lexeme]
        }

        // Not sure how this is going to work when we introduce
        // inheritance and `super`.  We'll need to know to skip
        // one class up the hierarchy.

        // We could probably pre-bind these for performance.
        return klass.findMethod(name.lexeme)?.bindTo(this)
    }

    operator fun set(name: Token, value: Any?) {
        properties[name.lexeme] = value
    }
}