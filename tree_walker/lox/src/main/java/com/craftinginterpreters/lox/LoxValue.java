package com.craftinginterpreters.lox;

public sealed interface LoxValue permits LoxStr, LoxNum, LoxValue.Intern {

    public static enum Intern implements LoxValue {
        NIL, FALSE, TRUE;

        public String toString() {
            return switch(this) {
                case NIL -> "<LoxIntern: Nil>";
                case FALSE -> "<LoxIntern: False>";
                case TRUE -> "<LoxIntern: True>";
            };
        }

        public static LoxValue.Intern fromBool(boolean b) {
            return b ? TRUE : FALSE;
        }

        public LoxValue.Intern negate() {
            return switch(this) {
                case NIL -> TRUE;
                case TRUE -> FALSE;
                case FALSE -> TRUE;
            };
        }

    }
    
}
