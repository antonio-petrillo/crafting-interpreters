package com.craftinginterpreters.lox;

public record LoxNum(double num) implements Literal {
    public String toString() {
        return String.format("<LoxNum> %f", num);
    }
}
