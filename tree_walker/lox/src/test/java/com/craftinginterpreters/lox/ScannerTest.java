package com.craftinginterpreters.lox;

import static org.junit.jupiter.api.Assertions.*;
import static com.craftinginterpreters.lox.TokenType.*;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class ScannerTest {

    private void testScanGivenSource(List<Token> expecteds, String source) {
        Lox lox = new Lox();
        Scanner scanner = new Scanner(lox, source);
        List<Token> actuals = scanner.scanTokens();

        assertTrue(actuals.size() == expecteds.size(),
                String.format("Expected %d tokens, got %d.", expecteds.size(), actuals.size()));

        Iterator<Token> iter = actuals.iterator();
        for (Token expected : expecteds) {
            assertEquals(expected, iter.next());
        }
    }

    @Test
    public void shouldThrowIllegalStateIfExhausted() {
        List<Token> expecteds = List.of(new Token(EOF, "", Optional.empty(), 1));
        String sourceCode = "";

        Lox lox = new Lox();
        Scanner scanner = new Scanner(lox, sourceCode);

        List<Token> actuals = scanner.scanTokens();
        assertTrue(actuals.size() == expecteds.size(),
                String.format("Expected %d tokens, got %d.", expecteds.size(), actuals.size()));

        Iterator<Token> iter = actuals.iterator();
        for (Token expected : expecteds) {
            assertEquals(expected, iter.next());
        }


        IllegalStateException illegalState = assertThrows(IllegalStateException.class, () -> scanner.scanTokens(), "Scanner should be in IllegalStateExcpetion if scanTokens is called two times on the same scanner.");
        assertTrue(illegalState.getMessage().equals( "[PANIC] tokens already lexed and returned, scanner consumed."));
    }

    @Test
    public void shouldProduceSingleCharactersTokensWithSpaces() {
        List<Token> expecteds = List.of(
                new Token(LEFT_PAREN, "(", Optional.empty(), 1),
                new Token(RIGHT_PAREN, ")", Optional.empty(), 1),
                new Token(LEFT_BRACE, "{", Optional.empty(), 1),
                new Token(RIGHT_BRACE, "}", Optional.empty(), 1),
                new Token(COMMA, ",", Optional.empty(), 1),
                new Token(DOT, ".", Optional.empty(), 1),
                new Token(PLUS, "+", Optional.empty(), 1),
                new Token(MINUS, "-", Optional.empty(), 1),
                new Token(SEMICOLON, ";", Optional.empty(), 1),
                new Token(SLASH, "/", Optional.empty(), 1),
                new Token(STAR, "*", Optional.empty(), 1),
                new Token(EOF, "", Optional.empty(), 1));

        String sourceCode = "( ) { } , . + - ; / *";
        testScanGivenSource(expecteds, sourceCode);
        sourceCode = "(){},.+-;/*";
        testScanGivenSource(expecteds, sourceCode);
    }

    @Test
    public void shouldProduceSingleOrTwoCharactersTokens() {
        List<Token> expecteds = List.of(
                new Token(BANG, "!", Optional.empty(), 1),
                new Token(BANG_EQUAL, "!=", Optional.empty(), 1),
                new Token(EQUAL, "=", Optional.empty(), 1),
                new Token(EQUAL_EQUAL, "==", Optional.empty(), 1),
                new Token(LESS, "<", Optional.empty(), 1),
                new Token(LESS_EQUAL, "<=", Optional.empty(), 1),
                new Token(GREATER, ">", Optional.empty(), 1),
                new Token(GREATER_EQUAL, ">=", Optional.empty(), 1),
                new Token(EOF, "", Optional.empty(), 1));

        String sourceCode = "! != = == < <= > >=";
        testScanGivenSource(expecteds, sourceCode);
    }

    @Test
    public void shouldProduceStringAndNumbers() {
        List<Token> expecteds = List.of(
                new Token(STRING, "\"source code\"", Optional.of(new LoxStr("source code")), 1),
                new Token(STRING, "\"asdf\"", Optional.of(new LoxStr("asdf")), 1),
                new Token(NUMBER, "42", Optional.of(new LoxNum(42)), 1),
                new Token(NUMBER, "42.42", Optional.of(new LoxNum(42.42)), 1),
                new Token(EOF, "", Optional.empty(), 1));
        String sourceCode = "\"source code\" \"asdf\" 42 42.42";
        testScanGivenSource(expecteds, sourceCode);
    }

    @Test
    public void shouldProduceKeywords() {
        List<Token> expecteds = List.of(
                new Token(AND, "and", Optional.empty(), 1),
                new Token(CLASS, "class", Optional.empty(), 1),
                new Token(ELSE, "else", Optional.empty(), 1),
                new Token(FALSE, "false", Optional.of(LiteralValue.Intern.FALSE), 1),
                new Token(FUN, "fun", Optional.empty(), 1),
                new Token(FOR, "for", Optional.empty(), 1),
                new Token(IF, "if", Optional.empty(), 1),
                new Token(NIL, "nil", Optional.of(LiteralValue.Intern.NIL), 1),
                new Token(OR, "or", Optional.empty(), 1),
                new Token(PRINT, "print", Optional.empty(), 1),
                new Token(RETURN, "return", Optional.empty(), 1),
                new Token(SUPER, "super", Optional.empty(), 1),
                new Token(THIS, "this", Optional.empty(), 1),
                new Token(TRUE, "true", Optional.of(LiteralValue.Intern.TRUE), 1),
                new Token(VAR, "var", Optional.empty(), 1),
                new Token(WHILE, "while", Optional.empty(), 1),
                new Token(EOF, "", Optional.empty(), 1));
        String sourceCode = "and class else false fun for if nil or print return super this true var while";
        testScanGivenSource(expecteds, sourceCode);
    }

    @Test
    public void shouldProduceIdentifiers() {
        List<Token> expecteds = List.of(
                new Token(IDENTIFIER, "javaSucks", Optional.empty(), 1),
                new Token(IDENTIFIER, "clojureRule", Optional.empty(), 1),
                new Token(IDENTIFIER, "cIsBetterThanCPlusPlus", Optional.empty(), 1),
                new Token(IDENTIFIER, "emacsRule", Optional.empty(), 1),
                new Token(EOF, "", Optional.empty(), 1));
        String sourceCode = "javaSucks clojureRule cIsBetterThanCPlusPlus emacsRule";
        testScanGivenSource(expecteds, sourceCode);
    }

    @Test
    public void shouldDetectLineChanges() {
        List<Token> expecteds = List.of(
                new Token(IDENTIFIER, "javaSucks", Optional.empty(), 1),
                new Token(IDENTIFIER, "clojureRule", Optional.empty(), 2),
                new Token(IDENTIFIER, "cIsBetterThanCPlusPlus", Optional.empty(), 3),
                new Token(IDENTIFIER, "emacsRule", Optional.empty(), 4),
                new Token(EOF, "", Optional.empty(), 4));
        String sourceCode = "javaSucks\nclojureRule\ncIsBetterThanCPlusPlus\nemacsRule";
        testScanGivenSource(expecteds, sourceCode);
    }

    @Test
    public void shouldSkipComments() {
        List<Token> expecteds = List.of(
                new Token(EOF, "", Optional.empty(), 1));
        String sourceCode = "//this is a comment";
        testScanGivenSource(expecteds, sourceCode);

        expecteds = List.of(
                new Token(IDENTIFIER, "emacsRule", Optional.empty(), 1),
                new Token(EOF, "", Optional.empty(), 1));
        sourceCode = "emacsRule //this is a comment";
        testScanGivenSource(expecteds, sourceCode);

        expecteds = List.of(
                new Token(IDENTIFIER, "emacsRule", Optional.empty(), 1),
                new Token(IDENTIFIER, "anotherLine", Optional.empty(), 2),
                new Token(AND, "and", Optional.empty(), 3),
                new Token(EOF, "", Optional.empty(), 3));
        sourceCode = "emacsRule //this is a comment\nanotherLine //comment //nested\nand";
        testScanGivenSource(expecteds, sourceCode);
    }

    @Test
    public void shouldProduceTokenCorrectlyInARealisticCase() {
        List<Token> expecteds = List.of(
                new Token(VAR, "var", Optional.empty(), 1),
                new Token(IDENTIFIER, "x", Optional.empty(), 1),
                new Token(EQUAL, "=", Optional.empty(), 1),
                new Token(NUMBER, "1", Optional.of(new LoxNum(1)), 1),
                new Token(PLUS, "+", Optional.empty(), 1),
                new Token(NUMBER, "2", Optional.of(new LoxNum(2)), 1),
                new Token(SEMICOLON, ";", Optional.empty(), 1),
                new Token(IDENTIFIER, "x", Optional.empty(), 2),
                new Token(EQUAL, "=", Optional.empty(), 2),
                new Token(IDENTIFIER, "x", Optional.empty(), 2),
                new Token(PLUS, "+", Optional.empty(), 2),
                new Token(NUMBER, "2", Optional.of(new LoxNum(2)), 2),
                new Token(SEMICOLON, ";", Optional.empty(), 2),
                new Token(TokenType.EOF, "", Optional.empty(), 2));

        String sourceCode = "var x = 1 + 2;//create var\nx = x + 2;//do computation";
        testScanGivenSource(expecteds, sourceCode);
    }
}
