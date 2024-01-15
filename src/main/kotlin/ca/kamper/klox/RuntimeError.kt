package ca.kamper.klox

class RuntimeError(val token: Token, msg: String) : RuntimeException(msg)
