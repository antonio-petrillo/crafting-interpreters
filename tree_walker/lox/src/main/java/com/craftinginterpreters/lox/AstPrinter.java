package com.craftinginterpreters.lox;

public class AstPrinter implements Expr.Visitor<String> {

    private int nesting = 0;
    private String ident = "  ";

    public AstPrinter() {}
    public AstPrinter(String identStr) {
        this.ident = identStr;
    }

    public String print(Expr expr) {
        return String.format("AST:\n%s\n\n", expr.accept(this));
    }

    @Override
    public String visitBinaryExpr(Binary expr) {
        nesting++;
        String str = parenthesize(expr.operator().lexeme(), expr.left(), expr.right());
        nesting--;
        String nest = nesting > 0 ? ident.repeat(nesting - 1) : "";
        return String.format("%s%s", nest, str);
    }

    @Override
    public String visitGroupingExpr(Grouping expr) {
        nesting++;
        String str = parenthesize("group", expr.expression());
        nesting--;
        String nest = nesting > 0 ? ident.repeat(nesting - 1) : "";
        return String.format("%s%s", nest, str);
    }

    @Override
    public String visitLiteralExpr(Literal expr) {
        nesting++;
        String str = expr.value().toString();
        nesting--;
        String nest = nesting > 0 ? ident.repeat(nesting - 1) : "";
        return String.format("%s%s", nest, str);
    }

    @Override
    public String visitUnaryExpr(Unary expr) {
        nesting++;
        String str = parenthesize(expr.operator().lexeme(), expr.right());
        nesting--;
        String nest = nesting > 0 ? ident.repeat(nesting - 1) : "";
        return String.format("%s%s", nest, str);

    }

    private String parenthesize(String name, Expr... exprs) {
        String nest = ident.repeat(nesting);
        StringBuilder builder = new StringBuilder(String.format("(%s\n%s", name, nest));

        for (int i = 0; i < exprs.length; i++) {
            nesting++;
            builder.append(exprs[i].accept(this));
            nesting--;
            if (i == exprs.length - 1) {
                builder.append(")");
            } else {
                builder.append(String.format("\n%s", nest));
            }
        }

        return builder.toString();
    }

}
