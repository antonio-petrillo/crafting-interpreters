package com.craftinginterpreters.lox;

import java.util.List;

public class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {

    private int nesting = 0;
    private String ident = "  ";

    public AstPrinter() {}
    public AstPrinter(String identStr) {
        this.ident = identStr;
    }

    public void print(List<Stmt> program) {
        try {
            for(Stmt stmt : program) {
                StringBuilder sb = new StringBuilder("STMT:\n");
                sb.append(Stmt.accept(stmt, this));
                sb.append("\n\n");
                System.out.println(sb.toString());
            }
        } catch (VisitException ve) {
            System.err.printf("Error during AST printing: look for error in your syntax\n");
        }
    }

    @Override
    public String visitBinaryExpr(Binary expr) throws VisitException {
        nesting++;
        String str = parenthesize(expr.operator().lexeme(), expr.left(), expr.right());
        nesting--;
        String nest = nesting > 0 ? ident.repeat(nesting - 1) : "";
        return String.format("%s%s", nest, str);
    }

    @Override
    public String visitGroupingExpr(Grouping expr) throws VisitException {
        nesting++;
        String str = parenthesize("group", expr.expression());
        nesting--;
        return String.format("%s%s", ident.repeat(nesting), str);
    }

    @Override
    public String visitLiteralExpr(Literal expr) throws VisitException {
        return String.format("%s%s", ident.repeat(nesting), expr.value().toString());
    }

    @Override
    public String visitUnaryExpr(Unary expr) throws VisitException {
        nesting++;
        String str = parenthesize(expr.operator().lexeme(), expr.right());
        nesting--;
        return String.format("%s%s", ident.repeat(nesting), str);

    }

    private String parenthesize(String name, Expr... exprs) throws VisitException {
        String nest = ident.repeat(nesting);
        StringBuilder builder = new StringBuilder(String.format("%s(%s\n", nest, name));

        nesting++;
        for (int i = 0; i < exprs.length; i++) {
            builder.append(Expr.accept(exprs[i], this));
            if (i == exprs.length - 1) {
                builder.append(")\n");
            } else {
                builder.append("\n");
            }
        }
        nesting--;

        return builder.toString();
    }
    @Override
    public String visitExpressionStmt(Expression stmt) throws VisitException {
        nesting++;
        String str =  String.format("%s(expr-stmt %s)", ident.repeat(nesting), stmt.toString());
        nesting--;
        return str;
    }
    @Override
    public String visitPrintStmt(Print stmt) throws VisitException {
        nesting++;
        String str = String.format("%s(print %s)", ident.repeat(nesting), stmt.toString());
        nesting--;
        return str;
    }

}
