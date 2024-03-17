package com.craftinginterpreters.lox;

import java.util.Map;
import java.util.HashMap;

public class Environment {
    public final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    public Environment() {
        enclosing = null;
    }

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    public void define(String name, Object value) {
        values.put(name, value);
    }

    public Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            Object value = values.get(name.lexeme);
            if (value == null) {
                throw new RuntimeError(name, "Unitialized variable '" + name.lexeme + "'.");
            }
            return value;
        }

        if (enclosing != null) {
            return enclosing.get(name);
       }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    public void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
