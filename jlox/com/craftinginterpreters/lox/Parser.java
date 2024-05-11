package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;
	private int nesteLoopCount = 0;
	private Random rng;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
		rng = new Random();
    }

    public List<Stmt> parse() {
        try {
            List<Stmt> statements = new ArrayList<>();
            while (!isAtEnd()) {
                statements.add(declaration());
            }
            return statements;
        } catch (ParseError error) {
            //TODO: parse error handling
            return null;
        }
    }

    private Stmt declaration() {
        try {
			if (match(TokenType.CLASS)) {
				return classDeclaration();
			}
			if (match(TokenType.FUN)) {
				return function("function");
			}
            if (match(TokenType.VAR)) {
                return varDeclaration();
            }
            // fall-through to next grammar rule
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

	private Stmt classDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expect class name.");

		Expr.Variable superclass = null;
		if (match(TokenType.LESS)) {
			consume(TokenType.IDENTIFIER, "Expect superclass name.");
			superclass = new Expr.Variable(previous());
		}

		consume(TokenType.LEFT_BRACE, "Expect '{' before class body.");

		List<Stmt.Function> methods = new ArrayList<>();
		while(!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			methods.add(function("method"));
		}

		consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");

		return new Stmt.Class(name, superclass, methods);
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

    private Stmt statement() {
        if (match(TokenType.LEFT_BRACE)) return blockStatement();
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.RETURN)) return returnStatement();
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.FOR)) return forStatement();
        if (match(TokenType.BREAK)) return breakStatement();
        return expressionStatement();
    }

    private Stmt blockStatement() {
        return new Stmt.Block(block());
    }

    /**
     * A util fxn to parse statements within a single block.
     *
     * @return A list of statements between a pair of opening and closing braces.
     */
    private List<Stmt> block() {
        var statements = new ArrayList<Stmt>();
        while (!isAtEnd() && !check(TokenType.RIGHT_BRACE)) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

	private Stmt returnStatement() {
		Token keyword = previous();
		Expr value = null;
		if (!check(TokenType.SEMICOLON)) {
			value = expression();
		}
		consume(TokenType.SEMICOLON, "Expect ';' after return value");
		return new Stmt.Return(keyword, value);
	}

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after if.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition");
		nesteLoopCount++;
        Stmt body = statement();
		nesteLoopCount--;
		Stmt s =  new Stmt.While(condition, body);
		nesteLoopCount--;
        return s;
    }

    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        Expr condition = null;
        Expr increment = null;

        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition");

        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for loop clauses.");

        Stmt body = statement();

        // --------- start desugaring -----------//
        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        if (condition != null) {
            body = new Stmt.While(condition, body);
        } else {
            body = new Stmt.While(new Expr.Literal(true), body);
        }

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }
        // --------- end desugaring -----------//

        return body;
    }

    private Stmt breakStatement() {
		if (nesteLoopCount <= 0) {
            throw error(previous(), "'break' must be called inside loop (while of for).");
		}
		consume(TokenType.SEMICOLON, "Expect ';' after break.");
        return new Stmt.Break();
    }

	private Stmt.Function function(String kind) {
		Token name = null;
		if (check(TokenType.IDENTIFIER)) {
			name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
		} else {
			String fakeLexeme = "@-fn-" + Integer.valueOf(rng.nextInt()).toString() + Float.valueOf(rng.nextFloat()).toString();
			name = new Token(TokenType.IDENTIFIER, fakeLexeme, null, -1);
		}
		consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
		List<Token> parameters = new ArrayList<>();
		if (!check(TokenType.RIGHT_PAREN)) {
			do {
				if (parameters.size() >= 0xFF) {
					error(peek(), "Can't have more than 0xFF parameters.");
				}
				parameters.add(consume(TokenType.IDENTIFIER, "Expect paramter name."));
			} while (match(TokenType.COMMA));
		}
		consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");

		consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
		List<Stmt> body = block();
		return new Stmt.Function(name, parameters, body);
	}

    private Stmt expressionStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(value);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();

        if (match(TokenType.EQUAL)) {
            Token previous = previous();
            Expr right = assignment();

            if (expr instanceof Expr.Variable) {
                return new Expr.Assign(((Expr.Variable) expr).name, right);
            } else if (expr instanceof Expr.Get) {
				Expr.Get get = (Expr.Get)expr;
				return new Expr.Set(get.object, get.name, right);
			}

            throw error(previous, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(TokenType.GREATER,
                     TokenType.GREATER_EQUAL,
                     TokenType.LESS,
                     TokenType.LESS_EQUAL
                     )) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        // return primary();
        return call();
    }

	private Expr finishCall(Expr callee) {
		List<Expr> arguments = new ArrayList<>();

		if(!check(TokenType.RIGHT_PAREN)) {
			do {
				if (arguments.size() >= 0xFF) {
					error(peek(), "Can't have more than 0xFF arguments.");
				}
				arguments.add(expression());
			} while(match(TokenType.COMMA));
		}

		Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");

		return new Expr.Call(callee, paren, arguments);
	}

	private Expr call() {
		Expr expr = primary();

		while(true) {
			if(match(TokenType.LEFT_PAREN)) {
				expr = finishCall(expr);
			} else if(match(TokenType.DOT)) {
				Token name = consume(TokenType.IDENTIFIER, "Expect property name afer '.'.");
				expr = new Expr.Get(expr, name);
			} else {
				break;
			}
		}

		return expr;
	}

    private Expr primary() {
        if (match(TokenType.TRUE)){
			return new Expr.Literal(true);
		}

        if (match(TokenType.FALSE)) {
			return new Expr.Literal(false);
		}

        if (match(TokenType.NIL)){
			return new Expr.Literal(null);
		}

        if (match(TokenType.STRING, TokenType.NUMBER)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(TokenType.THIS)) {
            return new Expr.This(previous());
        }

        if (match(TokenType.SUPER)) {
			Token keyword = previous();
			consume(TokenType.DOT, "Expected '.' after super.");
			Token method = consume(TokenType.IDENTIFIER, "Expected superclass method name.");
            return new Expr.Super(keyword, method);
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

    private boolean match(TokenType ...types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            switch(peek().type) {
            case SEMICOLON:
                // if currently at semicolon, consume it, then return
                advance();
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

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return type == peek().type;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
