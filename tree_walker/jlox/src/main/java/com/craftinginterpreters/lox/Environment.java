package com.craftinginterpreters.lox;

import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;


public class Environment {
    private final Map<String, LoxValue> values = new HashMap<>();
    private final Optional<Environment> enclosing;

    private static final Map<String, LoxCallable> builtins = Map.ofEntries(
            Map.<String,LoxCallable>entry("clock", new LoxCallable() {

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

            }));

    public Environment() {
        this.enclosing = Optional.empty();
        values.putAll(builtins);
    }

    public Environment(Environment enclosing) {
        if (enclosing == null)
            throw new IllegalArgumentException("The Enclosing env cannot be null.");
        this.enclosing = Optional.of(enclosing);
    }

    public void define(String name, LoxValue value) {
        values.put(name, value);
    }

    public void assign(Token name, LoxValue value) throws EnvironmentException  {
        if (values.containsKey(name.lexeme())) {
            values.put(name.lexeme(), value);
            return;
        }
        if (!enclosing.isEmpty()) {
            enclosing.get().assign(name, value);
            return;
        }

        throw new EnvironmentException();
    }

    public LoxValue get(Token name) throws EnvironmentException {
        if (values.containsKey(name.lexeme())) {
            return values.get(name.lexeme());
        }

        if (!enclosing.isEmpty()) {
            return enclosing
                .get()
                .get(name);
        }

        throw new EnvironmentException();
    }

}
