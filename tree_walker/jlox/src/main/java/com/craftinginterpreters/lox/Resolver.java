package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Deque;
import java.util.ArrayDeque;

import static com.craftinginterpreters.lox.Expr.*;
import static com.craftinginterpreters.lox.Stmt.*;

public class Resolver implements Stmt.Visitor<Void>, Expr.Visitor<Void> {
    private final Interpreter interpreter;
    private final Deque<Map<String, Boolean>> scopes = new ArrayDeque<>();
    private FunctionType currentFunction = FunctionType.NONE;

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE, FUNCTION;
    }

    public void resolve(List<Stmt> stmts) {
        for (Stmt stmt : stmts) {
            resolve(stmt);
        }
    }

    public void resolve(Stmt stmt) {
        try {
            Stmt.accept(stmt, this);
        } catch (VisitException e) {
            // Error can't happen in this phase
        }
    }

    public void resolve(Expr expr) {
        try {
            Expr.accept(expr, this);
        } catch (VisitException e) {
            // Error can't happen in this phase
        }
    }

    private void resolveLocal(Expr expr, Token name) {
       Iterator<Map<String, Boolean>> iter = scopes.descendingIterator();
       for (int i = scopes.size() - 1; i >= 0; i--) {
           // no need to check for iter.hasNext()
           Map<String, Boolean> scope = iter.next();
           if(scope.containsKey(name.lexeme())) {
               interpreter.resolve(expr, scopes.size() - 1 - i);
               return;
           }
       }
    }

    private void resolveFunction(Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();
        for(Token param : function.params()){
            declare(param);
            define(param);
        }
        resolve(function.body());
        endScope();
        currentFunction = enclosingFunction;
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void declare(Token name) {
        if(scopes.isEmpty())
            return;

        Map<String, Boolean> scope = scopes.peek();
        if(scope.containsKey(name.lexeme()))
            interpreter.getLox().error(name, String.format("Already a variable with this name <%s> in this scope.", name.lexeme()));
        scope.put(name.lexeme(), false);
    }

    private void define(Token name) {
        if(scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme(), true);

    }

    @Override
    public Void visitBinaryExpr(Binary expr) throws VisitException {
        resolve(expr.left());
        resolve(expr.right());
        return null;
    }

    @Override
    public Void visitGroupingExpr(Grouping expr) throws VisitException {
        resolve(expr.expression());
        return null;
    }

    @Override
    public Void visitLiteralExpr(Literal expr) throws VisitException {
        return null;
    }

    @Override
    public Void visitUnaryExpr(Unary expr) throws VisitException {
        resolve(expr.right());
        return null;
    }

    @Override
    public Void visitVariableExpr(Variable expr) throws VisitException {
        if (!scopes.isEmpty() && !scopes.peek().getOrDefault(expr.name().lexeme(), false)) {
           interpreter.getLox().error(expr.name(), "Can't read local variable in its own initializer.");
        }
        resolveLocal(expr, expr.name());

        return null;
    }

    @Override
    public Void visitAssignExpr(Assign expr) throws VisitException {
        resolve(expr.value());
        resolveLocal(expr, expr.name());
        return null;
    }

    @Override
    public Void visitLogicalExpr(Logical expr) throws VisitException {
        resolve(expr.left());
        resolve(expr.right());
        return null;
    }

    @Override
    public Void visitCallExpr(Call expr) throws VisitException {
        resolve(expr.callee());
        for (Expr arg : expr.arguments()) {
            resolve(arg);
        }
        return null;
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) throws VisitException {
        resolve(stmt.expression());
        return null;
    }

    @Override
    public Void visitPrintStmt(Print stmt) throws VisitException {
        resolve(stmt.expression());
        return null;
    }

    @Override
    public Void visitVarStmt(Var stmt) throws VisitException {
        declare(stmt.name());
        if(!stmt.initializer().isEmpty()) {
            resolve(stmt.initializer().get());
        }
        define(stmt.name());
        return null;
    }

    @Override
    public Void visitBlockStmt(Block stmt) throws VisitException {
        beginScope();
        resolve(stmt.statements());
        endScope();
        return null;
    }

    @Override
    public Void visitIfStmt(If stmt) throws VisitException {
        resolve(stmt.condition());
        resolve(stmt.thenBranch());
        if (!stmt.elseBranch().isEmpty()) {
            resolve(stmt.elseBranch().get());
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(While stmt) throws VisitException {
        resolve(stmt.condition());
        resolve(stmt.body());
        return null;
    }

    @Override
    public Void visitFunctionStmt(Function stmt) throws VisitException {
        declare(stmt.name());
        define(stmt.name());
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitReturnStmt(Return stmt) throws VisitException {
        if(currentFunction == FunctionType.NONE) {
           interpreter.getLox().error(stmt.keyword(), "Can't return from top-level code.");
        }

        if(!stmt.value().isEmpty()) {
            resolve(stmt.value().get());
        }
        return null;
    }

}
