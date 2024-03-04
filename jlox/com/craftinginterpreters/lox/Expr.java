package com.craftinginterpreters.lox;

import java.util.List;

public abstract class Expr {

    interface Visitor<R> {
        public R visitBinaryExpr(Binary expr);

        public R visitTernaryExpr(Ternary expr);

        public R visitGroupingExpr(Grouping expr);

        public R visitLiteralExpr(Literal expr);

        public R visitUnaryExpr(Unary expr);
    }

    public abstract <R> R accept(Visitor<R> visitor);

    static class Binary extends Expr {
        public Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }

        final Expr left;
        final Token operator;
        final Expr right;
    }

    static class Ternary extends Expr {
        public Ternary(Expr condition, Token leftOperator, Expr consequence, Token rightOperator, Expr alternative) {
            this.condition = condition;
            this.leftOperator = leftOperator;
            this.consequence = consequence;
            this.rightOperator = rightOperator;
            this.alternative = alternative;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitTernaryExpr(this);
        }

        final Expr condition;
        final Token leftOperator;
        final Expr consequence;
        final Token rightOperator;
        final Expr alternative;
    }

    static class Grouping extends Expr {
        public Grouping(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }

        final Expr expression;
    }

    static class Literal extends Expr {
        public Literal(Object value) {
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }

        final Object value;
    }

    static class Unary extends Expr {
        public Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }

        final Token operator;
        final Expr right;
    }
}
