package com.craftinginterpreters.lox;

public sealed interface LiteralValue permits LoxStr, LoxNum, LiteralValue.Intern {

    public static enum Intern implements LiteralValue {
        NIL, FALSE, TRUE;

        public String toString() {
            return switch(this) {
                case NIL -> "<LoxIntern: Nil>";
                case FALSE -> "<LoxIntern: False>";
                case TRUE -> "<LoxIntern: True>";
            };
        }

    }
    
}
