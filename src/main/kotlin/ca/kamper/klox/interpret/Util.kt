package ca.kamper.klox.interpret

fun isEqual(a: Any?, b: Any?): Boolean {
    // special case to properly compare NaN!
    // https://kotlinlang.org/docs/equality.html#floating-point-numbers-equality
    // This looks the same as the non-special case,
    // but because both are known to be Doubles, the
    // comparison is done differently!
    if (a is Double && b is Double) {
        return a == b
    }
    return a == b
}

fun isTruthy(value: Any?) =
    when (value) {
        null -> false
        is Boolean -> value
        else -> true
    }

fun stringify(value: Any?): String {
    if (value == null) return "nil"
    val s = value.toString()
    if (value is Double && s.endsWith(".0")) {
        return s.dropLast(2)
    }
    return s
}
