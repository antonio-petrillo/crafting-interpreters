package com.craftinginterpreters.lox;

public record LoxNum(double num) implements LoxValue {
    public String toString() {
        return String.format("%f", num);
    }
}
