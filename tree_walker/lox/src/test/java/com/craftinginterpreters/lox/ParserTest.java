package com.craftinginterpreters.lox;

import static org.junit.jupiter.api.Assertions.*;
import static com.craftinginterpreters.lox.TokenType.*;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class ParserTest {

    private void testParserGivenSource(Optional<Expr> expected, String source) {
        Lox lox = new Lox();
        Scanner scanner = new Scanner(lox, source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(lox, tokens);
        Optional<Expr> actual = parser.parse();
        if (expected.isEmpty()) {
            assertTrue(actual.isEmpty(), String.format("Expected Expr to be %s, got %s", Optional.empty(), actual));
        } else {
            assertFalse(actual.isEmpty(), String.format("Expected <%s>, got <%s>.", expected, Optional.empty()));
            assertEquals(expected.get(), actual.get(), String.format("Expected <%s>, got <%s>.", expected, actual));
        }
    }

    @Test
    public void shouldProduceEmptyASTOnEmptySource() {
        String sourceCode = "";
        testParserGivenSource(Optional.empty(), sourceCode);
    }

    @Test
    public void shouldProduceSimpleBinaryExpr() {
        String sourceCode = "1 + 1";
        Expr expected = new Binary(new Literal(new LoxNum(1)),
                                   new Token(PLUS, "+", Optional.empty(), 1),
                                   new Literal(new LoxNum(1)));
        testParserGivenSource(Optional.of(expected), sourceCode);
    }

    @Test
    public void shouldThrowIllegalStateIfExhaustedSimpleCode() {
        String sourceCode = "";
        Lox lox = new Lox();
        Scanner scanner = new Scanner(lox, sourceCode);
        Parser parser = new Parser(lox, scanner.scanTokens());

        parser.parse();

        IllegalStateException illegalState = assertThrows(IllegalStateException.class, () -> parser.parse(), "Parser should be in IllegalStateExcpetion if parser is called two times on the same Parser.");
        assertTrue(illegalState.getMessage().equals( "[PANIC] AST already generated, parser consumed."));
    }

    @Test
    public void shouldThrowIllegalStateIfExhaustedLessSimpleCode() {
        String sourceCode = "1 + 1;";
        Lox lox = new Lox();
        Scanner scanner = new Scanner(lox, sourceCode);
        Parser parser = new Parser(lox, scanner.scanTokens());

        parser.parse();

        IllegalStateException illegalState = assertThrows(IllegalStateException.class, () -> parser.parse(), "Parser should be in IllegalStateExcpetion if parser is called two times on the same Parser.");
        assertTrue(illegalState.getMessage().equals( "[PANIC] AST already generated, parser consumed."));
    }

    @Test
    public void shouldReturnEmptyOnSyntaxError() {
        String sourceCode = "1 + @";
        testParserGivenSource(Optional.empty(), sourceCode);
    }

    @Test
    public void shouldParsePrecedenceCorrectly() {
        String sourceCode = "1 + 1 - -1 * 2 / 3";
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

        testParserGivenSource(Optional.of(expected), sourceCode);
    }

    @Test
    public void shouldParsePrecedenceCorrectlyWithGrouping() {
        String sourceCode = "1 + 1 - -1 * (2 / 3)";
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

        testParserGivenSource(Optional.of(expected), sourceCode);
    }
}
