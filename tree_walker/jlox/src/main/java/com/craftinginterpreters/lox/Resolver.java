package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

import com.craftinginterpreters.lox.Expr.Get;
import com.craftinginterpreters.lox.Expr.Set;
import com.craftinginterpreters.lox.Expr.Super;
import com.craftinginterpreters.lox.Expr.This;

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
    private ClassType currentClass = ClassType.NONE;

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE, FUNCTION, METHOD, INITIALIZER;
    }

    private enum ClassType {
        NONE, CLASS, SUBCLASS;
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
       Iterator<Map<String, Boolean>> iter = scopes.iterator();
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
        if (!scopes.isEmpty() && scopes.peek().get(expr.name().lexeme()) == Boolean.FALSE) {
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
            if(currentFunction == FunctionType.INITIALIZER) {
                interpreter.getLox().error(stmt.keyword(), "Can't return a value from an initializer.");
                return null;
            }

            resolve(stmt.value().get());
        }
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) throws VisitException {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;
        declare(stmt.name());
        define(stmt.name());

        if (!stmt.superclass().isEmpty() && stmt.name().lexeme().equals(stmt.superclass().get().name().lexeme())) {
            interpreter.getLox().error(stmt.superclass().get().name(), "A 'class' can't inherit from itself.");
            return null;
        }

        if (!stmt.superclass().isEmpty()) {
            currentClass = ClassType.SUBCLASS;
            resolve(stmt.superclass().get());
        }

        if (!stmt.superclass().isEmpty()) {
            beginScope();
            scopes.peek().put("super", true);
        }

        beginScope();
        scopes.peek().put("this", true);

        for (Function fn : stmt.methods()) {
            FunctionType declaration = FunctionType.METHOD;
            if (fn.name().lexeme().equals("init"))
                declaration = FunctionType.INITIALIZER;

            resolveFunction(fn, declaration);
        }
        endScope();
        if (!stmt.superclass().isEmpty()) {
            endScope();
        }

        currentClass = enclosingClass;
        return null;
    }

    @Override
    public Void visitGetExpr(Get expr) throws VisitException {
        resolve(expr.obj());
        return null;
    }

    @Override
    public Void visitSetExpr(Set expr) throws VisitException {
        resolve(expr.value());
        resolve(expr.obj());
        return null;
    }

    @Override
    public Void visitThisExpr(This expr) throws VisitException {
        if (currentClass == ClassType.NONE) {
            interpreter.getLox().error(expr.keyword(), "Can't use 'this' outside of a class.");
            return null;
        }

        resolveLocal(expr, expr.keyword());
        return null;
    }

    @Override
    public Void visitSuperExpr(Super expr) throws VisitException {
        if (currentClass == ClassType.NONE) {
            interpreter.getLox().error(expr.keyword(), "Can't use 'super' outside of a class.");
            return null;
        } else if(currentClass != ClassType.SUBCLASS) {
            interpreter.getLox().error(expr.keyword(), "Can't use 'super' in a class without superclass.");
            return null;
        }

        resolveLocal(expr, expr.keyword());
        return null;
    }

}
