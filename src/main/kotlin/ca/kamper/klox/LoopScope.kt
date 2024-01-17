package ca.kamper.klox

import java.util.*

/**
 * LoopScope helps the [Parser] keep track of the surrounding context
 * of loop and function bodies in order to determine where a break
 * or continue statement is legal.
 */
class LoopScope {
    private val functionStack = ArrayDeque<Int>()
    private var nestedLoops = 0

    fun <T> inLoop(block: () -> T): T {
        nestedLoops++
        return try {
            block()
        } finally {
            nestedLoops--
        }
    }

    fun jumpAllowed() = nestedLoops > 0

    fun <T> inFunction(block: () -> T): T {
        functionStack.push(nestedLoops)
        nestedLoops = 0
        return try {
            block()
        } finally {
            nestedLoops = functionStack.pop()
        }
    }
}