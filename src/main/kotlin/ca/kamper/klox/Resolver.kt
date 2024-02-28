package ca.kamper.klox

import java.util.*

/*
 * Analysis:
 * - we traverse the AST
 * - we keep track of a stack of environments defining where each name is defined
 * - every time we run into a reference to a name, we check where it is defined and report the distance up the stack
 * - we can find and report semantic errors as well
 */
class Resolver(
    private val reportError: (Token, msg: String) -> Unit,
    private val resolveExpr: (Expr, depth: Int) -> Unit, // Interpreter
) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private val scopes: Stack<MutableMap<String, Boolean>> = Stack()

    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolve(expr.target)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {}

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (scopes.isNotEmpty() && scopes.peek()[expr.name.lexeme] == false) {
            reportError(expr.name, "Can't read local variable in its own initialiser.")
        }

        resolveLocal(expr, expr.name)
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        scopes
            .indexOfLast { it.containsKey(name.lexeme) }
            .takeUnless { it < 0 }
            ?.also { idx ->
                val depth = scopes.lastIndex - idx
                resolveExpr(expr, depth)
            }
        // else: it's global
    }

    override fun visitFunctionCall(expr: Expr.FunctionCall) {
        resolve(expr.function)
        expr.arguments.forEach { resolve(it) }
    }

    override fun visitDotExpr(expr: Expr.Dot) {
        resolve(expr.left)
        // Anything on the right doesn't need to be resolved because it's a property of
        // the thing on the left?
    }

    override fun visitThisExpr(expr: Expr.This) {
        // Requires the reference to be within the scope of a class definition.
        // This wouldn't work for, eg, javascript, where a `this` reference in
        // a free-standing function can be rebound to an object at runtime, but
        // falls through to the global scope if there is no object binding.
        resolveLocal(expr, expr.keyword)
    }

    override fun visitSuperExpr(expr: Expr.Super) {
        resolveLocal(expr, expr.keyword)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        resolve(stmt.expr)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expr)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        // If a var is declared but not defined, it's in the process of being
        // introduced (syntactically speaking) and you can't reference it.
        // So I guess any lookup that happens during the resolution of the
        // initialiser needs to make sure that the identifier in question
        // is defined.
        declare(stmt.name)
        if (stmt.initializer != null) {
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    private fun declare(name: Token) {
        if (scopes.empty()) return
        val scope = scopes.peek()
        if (scope.containsKey(name.lexeme)) {
            reportError(name, "Already a variable with this name in this scope.")
        }
        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.empty()) return
        scopes.peek()[name.lexeme] = true
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        inNewScope {
            resolve(stmt.stmts)
        }
    }

    fun resolve(stmts: List<Stmt>) {
        stmts.forEach { resolve(it) }
    }

    private fun resolve(stmt: Stmt?) {
        stmt?.accept(this)
    }

    private fun resolve(expr: Expr?) {
        expr?.accept(this)
    }

    private fun inNewScope(block: () -> Unit) {
        scopes.push(mutableMapOf())
        try {
            block()
        } finally {
            scopes.pop()
        }
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        resolve(stmt.elseBranch)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    override fun visitForStmt(stmt: Stmt.For) {
        inNewScope {
            resolve(stmt.initializer)
            resolve(stmt.condition)
            resolve(stmt.body)
            resolve(stmt.increment)
        }
    }

    override fun visitFunctionDeclarationStmt(stmt: Stmt.FunctionDeclaration) {
        declare(stmt.name)
        // we can call recursively because we define the name before
        // analysing the body:
        define(stmt.name)
        resolveFunction(stmt)
    }

    private fun resolveFunction(stmt: Stmt.FunctionDeclaration) {
        // I guess we need to be pretty in sync with how the Interpreter defines scope
        // while traversing the same ast.  Actually, that's done in LoxFunction, where
        // it creates a new Environment to contain the parameters and then evaluates
        // the body.  That seems to be the same as what we're doing here.
        inNewScope {
            stmt.parameters.forEach {
                declare(it)
                define(it)
            }
            resolve(stmt.body)
        }
    }

    override fun visitReturn(stmt: Stmt.Return) {
        resolve(stmt.value)
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        declare(stmt.name)
        define(stmt.name)

        if (stmt.name.lexeme == stmt.superName?.name?.lexeme) {
            // Is this a bit like `var a = a`?  If we try to resolve the
            // superclass name first, could we shadow the name in the subclass?
            // Well, not a useful idea anyway...
            reportError(stmt.superName.name, "A class can't inherit from itself.")
        }
        stmt.superName?.let { resolve(it) }

        // TODO: only start new scope if superName is defined
        inNewScope {
            stmt.superName?.name?.copy(lexeme = "super")?.let {
                declare(it)
                define(it)
            }

            inNewScope {
                stmt.name.copy(lexeme = "this").let {
                    declare(it)
                    define(it)
                }
                stmt.methods.forEach { resolveFunction(it) }
            }
        }
    }
}