package com.craftinginterpreters.lox;

public record LoxStr(String str) implements LoxValue {
    public String toString() {
        return str;
    }
}
