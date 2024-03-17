package com.craftinginterpreters.lox;

import java.util.List;

public abstract class Stmt {

    interface Visitor<R> {
        public R visitExpressionStmt(Expression stmt);
        public R visitPrintStmt(Print stmt);
    }

    public abstract <R> R accept(Visitor<R> visitor);

    public static class Expression extends Stmt {
        final Expr expression;

        public Expression(Expr expression) {
            this.expression = expression;
        }

        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }
    }

    public static class Print extends Stmt {
        final Expr expression;

        public Print(Expr expression) {
            this.expression = expression;
        }

        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitPrintStmt(this);
        }
    }

}
