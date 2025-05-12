package com.craftinginterpreters.lox;

public record LoxNum(double num) implements LiteralValue {
    public String toString() {
        return String.format("<LoxNum: %f>", num);
    }
}
