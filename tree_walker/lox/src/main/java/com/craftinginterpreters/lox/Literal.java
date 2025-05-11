package com.craftinginterpreters.lox;

public sealed interface Literal permits LoxStr, LoxNum, Literal.Nil {
    public static final Literal.Nil nil = new Literal.Nil();

    public static final class Nil implements Literal {

        private Nil() {};

        public String toString() {
            return "nil";
        }

    }
    
}
