package com.craftinginterpreters.lox;

public sealed interface Stmt permits Expression, Print {

    public interface Visitor<T> {
        T visitExpressionStmt(Expression stmt) throws VisitException;
        T visitPrintStmt(Print stmt) throws VisitException;
    }

    public static <T> T accept(Stmt stmt, Visitor<T> v) throws VisitException {
        return switch (stmt) {
            case Expression s -> v.visitExpressionStmt(s);
            case Print s -> v.visitPrintStmt(s);
            default -> throw new VisitException("Unknown Stmt");
        };
    }

}
