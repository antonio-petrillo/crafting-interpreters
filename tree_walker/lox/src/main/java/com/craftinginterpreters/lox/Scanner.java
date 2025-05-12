package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.craftinginterpreters.lox.TokenType.*;

public class Scanner {
    private static final Map<String, TokenType> keywords = new HashMap<>();;

    static {
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private boolean exhausted = false;
    private final Lox context;

    private int start = 0;
    private int current = 0;
    private int line = 1;

    public Scanner(Lox lox, String source) {
        this.source = source;
        context = lox;
    }

    public List<Token> scanTokens() {
        if (exhausted) {
            throw new IllegalStateException("[PANIC] tokens already lexed and returned, scanner consumed.");
        }
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", Optional.empty(), line));
        exhausted = true;
        return tokens;
    }

    // Tokenizer
    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(':
                addToken(LEFT_PAREN);
                break;
            case ')':
                addToken(RIGHT_PAREN);
                break;
            case '{':
                addToken(LEFT_BRACE);
                break;
            case '}':
                addToken(RIGHT_BRACE);
                break;
            case ',':
                addToken(COMMA);
                break;
            case '.':
                addToken(DOT);
                break;
            case '-':
                addToken(MINUS);
                break;
            case '+':
                addToken(PLUS);
                break;
            case ';':
                addToken(SEMICOLON);
                break;
            case '*':
                addToken(STAR);
                break;

            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;

            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd())
                        advance();
                } else {
                    addToken(SLASH);
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
                    identifier();
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
        addToken(STRING, Optional.of(new LoxStr(value)));
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
        addToken(NUMBER, Optional.of(new LoxNum(num)));
    }

    private void identifier() {
        while (isAlphaNumeric(peek()))
            advance();

        String text = source.substring(start, current);
        TokenType type = keywords.getOrDefault(text, IDENTIFIER);
        switch(type) {
            case NIL:
                addToken(type, Optional.of(LiteralValue.Intern.NIL));
                break;
            case FALSE:
                addToken(type, Optional.of(LiteralValue.Intern.FALSE));
                break;
            case TRUE:
                addToken(type, Optional.of(LiteralValue.Intern.TRUE));
                break;
            default:
                addToken(type);
                break;
        }
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
        addToken(type, Optional.empty());
    }

    private void addToken(TokenType type, Optional<LiteralValue> literal) {
        String lexeme = source.substring(start, current);
        tokens.add(new Token(type, lexeme, literal, line));
    }
}
