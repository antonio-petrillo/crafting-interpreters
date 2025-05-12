package com.craftinginterpreters.lox;

public record LoxStr(String str) implements LiteralValue {
    public String toString() {
        return String.format("<LoxStr: \"%s\">", str);
    }
}
