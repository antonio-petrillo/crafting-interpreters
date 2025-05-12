package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Scanner {
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", TokenType.AND);
        keywords.put("class", TokenType.CLASS);
        keywords.put("else", TokenType.ELSE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("for", TokenType.FOR);
        keywords.put("fun", TokenType.FUN);
        keywords.put("if", TokenType.IF);
        keywords.put("nil", TokenType.NIL);
        keywords.put("or", TokenType.OR);
        keywords.put("print", TokenType.PRINT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("super", TokenType.SUPER);
        keywords.put("this", TokenType.THIS);
        keywords.put("true", TokenType.TRUE);
        keywords.put("var", TokenType.VAR);
        keywords.put("while", TokenType.WHILE);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private boolean exhausted = false;
    private final Lox context;

    private int start = 0;
    private int current = 0;
    private int line = 0;

    public Scanner(Lox lox, String source) {
        this.source = source;
        context = lox;
    }

    public List<Token> scanTokens() {
        if (exhausted) {
            throw new IllegalStateException("[PANIC] tokens already lexed and returned, scanner consumed");
        }
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        exhausted = true;
        return tokens;
    }

    // Tokenizer
    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(':
                addToken(TokenType.LEFT_PAREN);
                break;
            case ')':
                addToken(TokenType.RIGHT_PAREN);
                break;
            case '{':
                addToken(TokenType.LEFT_BRACE);
                break;
            case '}':
                addToken(TokenType.RIGHT_BRACE);
                break;
            case ',':
                addToken(TokenType.COMMA);
                break;
            case '.':
                addToken(TokenType.DOT);
                break;
            case '-':
                addToken(TokenType.MINUS);
                break;
            case '+':
                addToken(TokenType.PLUS);
                break;
            case ';':
                addToken(TokenType.SEMICOLON);
                break;
            case '*':
                addToken(TokenType.STAR);
                break;

            case '!':
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '=':
                addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;
            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;

            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd())
                        advance();
                } else {
                    addToken(TokenType.SLASH);
                }
                break;

            case ' ':
            case '\r':
            case '\t':
                break;

            case '\n':
                line++;
                break;

            case '"':
                string();
                break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                } else {
                    context.error(line, String.format("Unexpected character [%c].", c));
                }
                break;
        }
    }

    ///////////////////////////
    // Lexing Special Tokens //
    ///////////////////////////
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n')
                line++;
            advance();
        }

        if (isAtEnd()) {
            context.error(line, "Unterminated string.");
            return;
        }

        advance();

        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, new LoxStr(value));
    }

    private void number() {
        while (isDigit(peek()))
            advance();

        if (peek() == '.' && isDigit(peekNext())) {
            advance();

            while (isDigit(peek()))
                advance();
        }

        Double num = Double.parseDouble(source.substring(start, current));
        addToken(TokenType.NUMBER, new LoxNum(num));
    }

    private void identifier() {
        while (isAlphaNumeric(peek()))
            advance();

        String text = source.substring(start, current);
        addToken(keywords.getOrDefault(text, TokenType.IDENTIFIER));
    }

    ////////////////////////////////////////////
    // Character Lookup Methods and Utilities //
    ////////////////////////////////////////////

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected)
            return false;
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd())
            return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length())
            return '\0';
        return source.charAt(current + 1);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAlpha(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    private static boolean isAlphaNumeric(char c) {
        return isDigit(c) || isAlpha(c);
    }

    //////////////////////////////
    // Collect tokens utilities //
    //////////////////////////////

    private void addToken(TokenType type) {
        addToken(type, LiteralValue.nil);
    }

    private void addToken(TokenType type, LiteralValue literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
