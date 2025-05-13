package com.craftinginterpreters.lox;

public record Print(Expr expression) implements Stmt {  }
