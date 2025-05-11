package com.craftinginterpreters.lox;

public record LoxStr(String s) implements Literal {
    public String toString() {
        return String.format("<LoxStr> \"%s\"", s);
    }
}
