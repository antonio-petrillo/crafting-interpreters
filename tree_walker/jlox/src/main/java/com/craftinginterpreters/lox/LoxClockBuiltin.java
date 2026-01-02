package com.craftinginterpreters.lox;

import java.util.Iterator;
import java.util.List;

import static com.craftinginterpreters.lox.Stmt.*;

public final class LoxClockBuiltin implements LoxCallable {
    public static final LoxClockBuiltin fn = new LoxClockBuiltin();

    private LoxClockBuiltin() {};

    @Override
    public LoxValue call(Interpreter interpreter, List<LoxValue> arguments) {
        return new LoxNum(System.currentTimeMillis() / 1000.);
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public String toString() {
        return "<native fn: clock>";
    }
}
