package com.craftinginterpreters.lox;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class AstPrinterTest {

    @Test
    public void thisDoesntFailButIWantToSeeTheOutput() {
        Expr expression = new Binary(
        new Unary(
            new Token(TokenType.MINUS, "-", null, 1),
            new Literal(new LoxNum(123))),
        new Token(TokenType.STAR, "*", null, 1),
        new Grouping(
            new Literal(new LoxNum(45.67))));

        System.out.println(new AstPrinter().print(expression));
    }
}
