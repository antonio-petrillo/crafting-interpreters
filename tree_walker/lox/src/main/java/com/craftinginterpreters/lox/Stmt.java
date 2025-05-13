package com.craftinginterpreters.lox;

import java.util.List;

public sealed interface Stmt permits
    Stmt.Expression,
    Stmt.Print,
    Stmt.Var,
    Stmt.Block
{

    public static record Expression(Expr expression) implements Stmt {  }
    public static record Var(Token name, Expr initializer) implements Stmt {  }
    public static record Print(Expr expression) implements Stmt {  }
    public static record Block(List<Stmt> statements) implements Stmt {  }

    public interface Visitor<T> {
        T visitExpressionStmt(Expression stmt) throws VisitException;
        T visitPrintStmt(Print stmt) throws VisitException;
        T visitVarStmt(Var stmt) throws VisitException;
        T visitBlockStmt(Block stmt) throws VisitException;
    }

    public static <T> T accept(Stmt stmt, Visitor<T> v) throws VisitException {
        return switch (stmt) {
            case Expression s -> v.visitExpressionStmt(s);
            case Print s -> v.visitPrintStmt(s);
            case Var s -> v.visitVarStmt(s);
            case Block s -> v.visitBlockStmt(s);
            default -> throw new VisitException("Unknown Stmt");
        };
    }

}
