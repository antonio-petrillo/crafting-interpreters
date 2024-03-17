package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.Expr;
import com.craftinginterpreters.lox.Stmt;
import com.craftinginterpreters.lox.TokenType;

public class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
    public String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        // I.E (x == y ? A : B) => (?: (x == y) A B)
        return parenthesize(expr.leftOperator.lexeme + expr.rightOperator.lexeme,
                            expr.condition, expr.consequence, expr.alternative);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if(expr.value == null) {
            return "nil";
        }
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize2("=", expr.name.lexeme, expr.value);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        if (stmt.initializer == null) {
            return parenthesize2("var", stmt.name);
        }

        return parenthesize2("var", stmt.name, "=", stmt.initializer);
    }

    @Override
    public String visitBlockStmt(Stmt.Block block) {
        StringBuilder builder = new StringBuilder();

        builder.append("(block ");

        for (Stmt statement : block.statements) {
           builder.append(statement.accept(this));
        }

        builder.append(")");

        return builder.toString();
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return parenthesize(";", stmt.expression);
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return parenthesize("print", stmt.expression);
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);

        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }

    private String parenthesize2(String name, Object ...parts) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);

        for (Object part : parts){
            builder.append(" ");

            if (part instanceof Expr) {
                builder.append(((Expr) part).accept(this));
            } else if (part instanceof Stmt)  {
                builder.append(((Stmt) part).accept(this));
            } else if (part instanceof Token)  {
                builder.append(((Token) part).lexeme);
            } else {
                builder.append(part);
            }
        }

        builder.append(")");

        return builder.toString();
    }

    // public static void main(String[] args) {
    //     Expr expression = new Expr.Binary(
    //                                       new Expr.Unary(
    //                                                      new Token(TokenType.MINUS, "-", null, 1),
    //                                                      new Expr.Literal(123)),
    //                                       new Token(TokenType.STAR, "*", null, 1),
    //                                       new Expr.Grouping(
    //                                                         new Expr.Literal(45.67)));

    //     System.out.println(new AstPrinter().print(expression));
    // }
    // ;; => (* (- 123) (group 45.67))
}
