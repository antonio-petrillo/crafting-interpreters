package com.craftinginterpreters.lox;

public sealed interface LiteralValue permits LoxStr, LoxNum, LiteralValue.Nil {
    public static final LiteralValue.Nil nil = new LiteralValue.Nil();

    public static final class Nil implements LiteralValue {

        private Nil() {};

        public String toString() {
            return "<LoxNil>";
        }

    }
    
}
