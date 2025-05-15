package com.craftinginterpreters.lox;

import java.util.Optional;

public record Token(TokenType type, String lexeme, Optional<LoxValue> literal, int line) {

  public String toString() {
    StringBuilder sb = new StringBuilder("Token{ type: ");
    sb.append(type.toString());
    sb.append(String.format(", lexeme: %s", lexeme));
    if (!literal.isEmpty()) {
      sb.append(String.format(", literal: %s", literal.get()));
    }
    sb.append(String.format(", line: %d }", line));
    return sb.toString();
  }

}
