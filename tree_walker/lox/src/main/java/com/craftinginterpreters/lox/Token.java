package com.craftinginterpreters.lox;

public class Token {
  private final TokenType type;
  private final String lexeme;
  private final Literal literal;
  private final int line; 

  public Token(TokenType type, String lexeme, Literal literal, int line) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  public String toString() {
    return type + " " + lexeme + " " + literal;
  }
}
