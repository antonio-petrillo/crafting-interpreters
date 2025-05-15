package com.craftinginterpreters.lox;

import java.util.Iterator;
import java.util.List;

import static com.craftinginterpreters.lox.Stmt.*;

public sealed interface LoxCallable extends LoxValue permits LoxClockBuiltin, LoxFunction, LoxClass {

    LoxValue call(Interpreter interpreter, List<LoxValue> arguments);
    int arity();
}
