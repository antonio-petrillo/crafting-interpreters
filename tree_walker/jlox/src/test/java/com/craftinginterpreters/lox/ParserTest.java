package com.craftinginterpreters.lox;

import static org.junit.jupiter.api.Assertions.*;
import static com.craftinginterpreters.lox.TokenType.*;

import static com.craftinginterpreters.lox.Stmt.*;
import static com.craftinginterpreters.lox.Expr.*;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class ParserTest {

    private void testParserGivenSource(List<Stmt> expecteds, String source) {
        Lox lox = new Lox();
        Scanner scanner = new Scanner(lox, source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(lox, tokens);
        List<Stmt> actuals = parser.parse();
        assertTrue(actuals.size() == expecteds.size(),
                String.format("Expected Expr to have %d statements, got %d", expecteds.size(), actuals.size()));
        Iterator<Stmt> iter = expecteds.iterator();
        for (Stmt actual : actuals) {
            Stmt expected = iter.next();
            assertEquals(expected, actual, String.format("Expected <%s> got <%s>", expected, actual));
        }
    }

    @Test
    public void shouldProduceEmptyASTOnEmptySource() {
        String sourceCode = "";
        testParserGivenSource(Collections.emptyList(), sourceCode);
    }

    @Test
    public void shouldProduceSimpleBinaryExpr() {
        String sourceCode = "print 1 + 1;";
        Expr expected = new Binary(new Literal(new LoxNum(1)),
                                   new Token(PLUS, "+", Optional.empty(), 1),
                                   new Literal(new LoxNum(1)));
        testParserGivenSource(List.of(new Print(expected)), sourceCode);
    }

    @Test
    public void shouldThrowIllegalStateIfExhaustedSimpleCode() {
        String sourceCode = "";
        Lox lox = new Lox();
        Scanner scanner = new Scanner(lox, sourceCode);
        Parser parser = new Parser(lox, scanner.scanTokens());

        parser.parse();

        IllegalStateException illegalState = assertThrows(IllegalStateException.class, () -> parser.parse(), "Parser should be in IllegalStateExcpetion if parser is called two times on the same Parser.");
        assertTrue(illegalState.getMessage().contains("consumed"));
    }

    @Test
    public void shouldThrowIllegalStateIfExhaustedLessSimpleCode() {
        String sourceCode = "1 + 1;";
        Lox lox = new Lox();
        Scanner scanner = new Scanner(lox, sourceCode);
        Parser parser = new Parser(lox, scanner.scanTokens());

        parser.parse();

        IllegalStateException illegalState = assertThrows(IllegalStateException.class, () -> parser.parse(), "Parser should be in IllegalStateExcpetion if parser is called two times on the same Parser.");
        assertTrue(illegalState.getMessage().contains("consumed"));
    }

    @Test
    public void shouldReturnEmptyOnSyntaxError() {
        String sourceCode = "1 + @";
        testParserGivenSource(Collections.emptyList(), sourceCode);
    }

    @Test
    public void shouldParsePrecedenceCorrectly() {
        String sourceCode = "1 + 1 - -1 * 2 / 3;";
        Expr expected = new Binary(
                new Binary(new Literal(new LoxNum(1)),
                        new Token(PLUS, "+", Optional.empty(), 1),
                        new Literal(new LoxNum(1))),
                new Token(MINUS, "-", Optional.empty(), 1),
                new Binary(new Binary(new Unary(new Token(MINUS, "-", Optional.empty(), 1),
                                                new Literal(new LoxNum(1))),
                                      new Token(STAR, "*", Optional.empty(), 1),
                                      new Literal(new LoxNum(2))),
                           new Token(SLASH, "/", Optional.empty(), 1),
                           new Literal(new LoxNum(3))));

        testParserGivenSource(List.<Stmt>of(new Expression(expected)), sourceCode);
    }

    @Test
    public void shouldParsePrecedenceCorrectlyWithGrouping() {
        String sourceCode = "1 + 1 - -1 * (2 / 3);";
        Expr expected = new Binary(
                new Binary(new Literal(new LoxNum(1)),
                        new Token(PLUS, "+", Optional.empty(), 1),
                        new Literal(new LoxNum(1))),
                new Token(MINUS, "-", Optional.empty(), 1),
                new Binary(new Unary(new Token(MINUS, "-", Optional.empty(), 1), new Literal(new LoxNum(1))),
                        new Token(STAR, "*", Optional.empty(), 1),
                        new Grouping(
                                new Binary(new Literal(new LoxNum(2)),
                                        new Token(SLASH, "/", Optional.empty(), 1),
                                        new Literal(new LoxNum(3))))));

        testParserGivenSource(List.<Stmt>of(new Expression(expected)), sourceCode);
    }

    @Test
    public void shouldParsePrintStmt() {
        String sourceCode = "print 1 + 1 - -1 * 2 / 3;";
        Expr expected = new Binary(
                new Binary(new Literal(new LoxNum(1)),
                        new Token(PLUS, "+", Optional.empty(), 1),
                        new Literal(new LoxNum(1))),
                new Token(MINUS, "-", Optional.empty(), 1),
                new Binary(new Binary(new Unary(new Token(MINUS, "-", Optional.empty(), 1),
                                                new Literal(new LoxNum(1))),
                                      new Token(STAR, "*", Optional.empty(), 1),
                                      new Literal(new LoxNum(2))),
                           new Token(SLASH, "/", Optional.empty(), 1),
                           new Literal(new LoxNum(3))));

        testParserGivenSource(List.<Stmt>of(new Print(expected)), sourceCode);
    }

    @Test
    public void shouldParseTwoStmts() {
        String sourceCode = "print 1 + 1 - -1 * 2 / 3; 1 + 1;";
        Expr expected1 = new Binary(
                new Binary(new Literal(new LoxNum(1)),
                        new Token(PLUS, "+", Optional.empty(), 1),
                        new Literal(new LoxNum(1))),
                new Token(MINUS, "-", Optional.empty(), 1),
                new Binary(new Binary(new Unary(new Token(MINUS, "-", Optional.empty(), 1),
                                                new Literal(new LoxNum(1))),
                                      new Token(STAR, "*", Optional.empty(), 1),
                                      new Literal(new LoxNum(2))),
                           new Token(SLASH, "/", Optional.empty(), 1),
                           new Literal(new LoxNum(3))));

        Expr expected2 = new Binary(new Literal(new LoxNum(1)),
                new Token(PLUS, "+", Optional.empty(), 1),
                new Literal(new LoxNum(1)));

        testParserGivenSource(List.<Stmt>of(new Print(expected1), new Expression(expected2)), sourceCode);
    }

    @Test
    public void shouldParseBlock() {
        String sourceCode = "{1; 1; 1;}";
        Stmt one = new Expression(new Literal(new LoxNum(1)));

        testParserGivenSource(List.<Stmt>of(new Block(List.<Stmt>of(one, one, one))), sourceCode);
    }

    @Test
    public void shouldParseVar() {
        String sourceCode = "var a = 1;";
        Expr one = new Literal(new LoxNum(1));
        Token a = new Token(IDENTIFIER, "a",Optional.empty(), 1);
        List<Stmt> expected = List.<Stmt>of(new Var(a, one));

        testParserGivenSource(expected, sourceCode);
    }

    @Test
    public void shouldParseVarAndAssign() {
        String sourceCode = "var a = 1; a = 2;";
        Expr one = new Literal(new LoxNum(1));
        Expr two = new Literal(new LoxNum(2));
        Token a = new Token(IDENTIFIER, "a",Optional.empty(), 1);
        Assign assign = new Assign(a, two);
        List<Stmt> expected = List.<Stmt>of(new Var(a, one), new Expression(assign));

        testParserGivenSource(expected, sourceCode);
    }
}
