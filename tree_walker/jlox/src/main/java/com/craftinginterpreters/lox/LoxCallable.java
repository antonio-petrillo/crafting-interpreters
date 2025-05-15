package com.craftinginterpreters.lox;

import java.util.List;

public non-sealed interface LoxCallable extends LoxValue {
    LoxValue call(Interpreter interpreter, List<LoxValue> arguments);
    int arity();
}
