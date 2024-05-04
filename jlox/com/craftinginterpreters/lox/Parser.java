package com.craftinginterpreters.lox;

import java.util.List;
import java.util.ArrayList;

import com.craftinginterpreters.lox.*;

public class Parser {

    private static class ParseError extends RuntimeException {

    }

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // public Expr parse() {
    //     try {
    //         return expression();
    //     } catch (ParseError error) {
    //         return null;
    //     }
    // }

    // program -> statement* EOF ;
    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();

        while (!isAtEnd()) {
            // statements.add(statement());
            statements.add(declaration());
        }

        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.VAR)) {
                return varDeclaration();
            }
            return statement();
        } catch(ParseError error) {
            synchronize();
            return null;
        }
    }

    // statement -> exprStmt | printStmt | block | ifStmt;
    private Stmt statement() {
		if (match(TokenType.IF)) {
			return ifStatement();
		}
        if (match(TokenType.PRINT)) {
            return printStatement();
        }
        if (match(TokenType.LEFT_BRACE)) {
            return new Stmt.Block(block());
        }

        return expressionStatement();
    }

    // ifStmt -> "if" "(" condition ")" statement ("else" statement) ;
	private Stmt ifStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after if.");
		Expr condition = expression();
		consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");
		Stmt ifBranch = statement();
		Stmt elseBranch = null;
		if (match(TokenType.ELSE)) {
			elseBranch = statement();
		}
		return new Stmt.If(condition, ifBranch, elseBranch);
	}

    // printStmt -> "print" expression ";" ;
    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expression ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    // exprStmt -> expression ";" ;
    private Stmt expressionStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expression ';' after expression.");
        return new Stmt.Expression(value);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    // // expression -> comma
    // expression -> assignment
    private Expr expression() {
        return assignment();
    }

    // in the book it is:
    // assignment -> IDENTIFIER "=" assignment | equality ;
    // but with comma and ternary become:
    // assignment -> IDENTIFIER "=" assignment | comma ;
    private Expr assignment() {
        Expr expr = comma();
        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    // comma -> ternary ("," ternary)*
    private Expr comma() {
        Expr expr = ternary();

        while(match(TokenType.COMMA)) {
            Token operator = previous();
            Expr right = ternary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // ternary -> logic_or | equality ( "?" expression ":" expression )*
    private Expr ternary() {
        Expr expr = or();

        while(match(TokenType.QUESTION_MARK)) {
            Token leftOperator = previous();
            Expr consequence = expression();

            Token rightOperator = consume(TokenType.COLON, "Expect ':' after expression.");

            Expr alternative = expression();

            expr = new Expr.Ternary(expr, leftOperator, consequence, rightOperator, alternative);
        }

        return expr;
    }

	// logic_or -> logic_and ("or" logic_and)*;
	private Expr or() {
		Expr expr = and();

		while(match(TokenType.OR)) {
			Token operator = previous();
			Expr right = and();
			expr = new Expr.Logical(expr, operator, right);
		}

		return expr;
	}

	// logic_and -> equality ("and" equality)*;
	private Expr and() {
		Expr expr = equality();

		while(match(TokenType.AND)){
			Token operator = previous();
			Expr right = equality();
			expr = new Expr.Logical(expr, operator, right);
		}

		return expr;
	}

    // equality -> comparison (( != | ==) comparison)*
    private Expr equality() {
        Expr expr = comparison();

        while(match(TokenType.BANG_EQUAL, TokenType.EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // comparison -> term (( <= | < | > | >=) term)*
    private Expr comparison() {
        Expr expr = term();
        while(match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // term -> factor (( + | - ) factor)*
    private Expr term() {
        Expr expr = factor();

        while(match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // factor -> unary (( * | / ) unary)*
    private Expr factor() {
        Expr expr = unary();

        while(match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // unary -> ( ! | - ) unary  | primary
    private Expr unary() {
        if(match(TokenType.MINUS, TokenType.BANG)) {
           Token operator = previous();
           Expr right = unary();
           return new Expr.Unary(operator, right);
        }

        return primary();
    }

    // primary -> NUMBER | STRING | true | false | nil | ( expression )
    private Expr primary() {
        if (match(TokenType.FALSE)) {
           return new Expr.Literal(false);
        }
        if (match(TokenType.TRUE)) {
           return new Expr.Literal(true);
        }
        if (match(TokenType.NIL)) {
           return new Expr.Literal(null);
        }

        if (match(TokenType.NUMBER, TokenType.STRING)) {
           return new Expr.Literal(previous().literal);
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if(check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if(check(type)) {
            return advance();
        }

        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        return isAtEnd() ? false : peek().type == type;
    }

    private Token advance() {
        if(!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
       advance();

       while (!isAtEnd()) {
           if (previous().type == TokenType.SEMICOLON) {
               return;
           }

           switch (peek().type) {
               case CLASS:
               case FUN:
               case VAR:
               case FOR:
               case IF:
               case WHILE:
               case PRINT:
               case RETURN:
                   return;
           }

           advance();
       }
    }

    // That's bad, O(n^2) on worst case
    private Token peek() {
        return tokens.get(current);
    }

    // That's bad, O(n^2) on worst case
    private Token previous() {
        return tokens.get(current - 1);
    }
}
