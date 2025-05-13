package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;
import static com.craftinginterpreters.lox.Expr.*;
import static com.craftinginterpreters.lox.Stmt.*;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class Parser {
    private static class ParseException extends Exception {}

    private final Lox lox;
    private final Iterator<Token> tokens;
    private Token current;
    private Token previous;
    private boolean exhausted = false;

    public Parser(Lox lox, List<Token> tokens) {
        this.lox = lox;
        this.tokens = tokens.iterator();
        current = this.tokens.next();
    }

    //////////////////////////
    // Parsing Verbs - LALR //
    //////////////////////////
    public List<Stmt> parse() {
        if (exhausted) {
            throw new IllegalStateException("Parser already consumed");
        }
        List<Stmt> statements = new ArrayList<>();
        try {
            while (!isAtEnd()) {
                statements.add(declaration());
            }
        } catch(ParseException pe) {
            synchronize();
            return Collections.emptyList();
        } finally {
            exhausted = true;
        }
        return List.<Stmt>copyOf(statements);
    }

    private Stmt declaration() throws ParseException {
        if(match(VAR))
            return varDeclaration();
        return statement();
    }

    private Stmt varDeclaration() throws ParseException {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr initializer = new Literal(LoxValue.Intern.NIL);
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Var(name, initializer);
    }

    private Stmt statement() throws ParseException {
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Block(block());

        return expressionStatement();
    }

    private Stmt ifStatement() throws ParseException {
        consume(LEFT_PAREN, "Expect '(' after 'if'. ");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after 'if' condition. ");
        Stmt thenBranch = statement();
        Optional<Stmt> elseBranch = Optional.empty();
        if (match(ELSE)) {
            elseBranch = Optional.of(statement());
        }

        return new If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() throws ParseException {
       Expr value = expression();
       consume(SEMICOLON, "Expect ';' after value.");
       return new Print(value);
    }

    private Stmt expressionStatement() throws ParseException {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Expression(expr);
    }

    private List<Stmt> block() throws ParseException {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return List.<Stmt>copyOf(statements);

    }

    private Expr expression() throws ParseException {
        return assignment();
    }

    private Expr assignment() throws ParseException {
        Expr expr = equality();
        if (match(EQUAL)) {
            Token equals = previous;
            Expr value = assignment();

            if(expr instanceof Variable v) {
                return new Assign(v.name(), value);
            }

            throw error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr equality() throws ParseException {
        Expr expr = comparison();

        while(match(BANG_EQUAL, BANG_EQUAL)) {
            Token operator = previous;
            Expr right = comparison();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() throws ParseException {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous;
            Expr right = term();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() throws ParseException {
        Expr expr = factor();

        while (match(PLUS, MINUS)) {
            Token operator = previous;
            Expr right = factor();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() throws ParseException {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous;
            Expr right = unary();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() throws ParseException {
        if (match(BANG, MINUS)) {
            Token operator = previous;
            Expr right = unary();
            return new Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() throws ParseException {
        if (match(FALSE))
            return new Literal(LoxValue.Intern.FALSE);
        if (match(TRUE))
            return new Literal(LoxValue.Intern.TRUE);
        if (match(NIL))
            return new Literal(LoxValue.Intern.NIL);

        if (match(NUMBER, STRING))
            return new Literal(previous.literal().get());

        if (match(IDENTIFIER))
            return new Variable(previous);

        if (match(LEFT_PAREN)) {
           Expr expr = expression();
           consume(RIGHT_PAREN, "Expecte ')' after expression.");
           return new Grouping(expr);
        }

        throw error(peek(), "Expected expression.");
    }

    ////////////////////////////////////////
    // Token Lookup Methods and Utilities //
    ////////////////////////////////////////

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    private Token advance() {
        if (!tokens.hasNext())
            throw new IllegalStateException("Nothing to consume, parse goes out of bound.");
        if (!isAtEnd()) {
            previous = current;
            current = tokens.next();
        }
        return previous;
    }

    private boolean isAtEnd() {
        return current.type() == EOF;
    }

    private Token peek() {
        return current;
    }

    private Token consume(TokenType type, String message) throws ParseException {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    /////////////////////////////
    // Parsing Exception Utils //
    /////////////////////////////
    private ParseException error(Token token, String message) {
        lox.error(token, message);
        return new ParseException();
    }

    private void synchronize() {
        try {
            advance();

            while (!isAtEnd()) {
                if (previous.type() == SEMICOLON)
                    return;

                switch (peek().type()) {
                    case CLASS:
                    case FUN:
                    case VAR:
                    case FOR:
                    case IF:
                    case WHILE:
                    case PRINT:
                    case RETURN:
                        return;
                    default:
                }

                advance();
            }
        } catch (IllegalStateException e) {
            if (!e.getMessage().equals("Nothing to consume, parse goes out of bound.")) {
                throw e;
            }
        }
    }
}
