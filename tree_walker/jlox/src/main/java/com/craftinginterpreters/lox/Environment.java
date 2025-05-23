package com.craftinginterpreters.lox;

import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;


public class Environment {
    private final Map<String, LoxValue> values = new HashMap<>();
    private final Optional<Environment> enclosing;

    private static final Map<String, LoxCallable> builtins = Map.ofEntries(
            Map.<String,LoxCallable>entry("clock", LoxClockBuiltin.fn));

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

    public void assignAt(int distance, Token name, LoxValue value) throws EnvironmentException  {
        ancestor(distance).values.put(name.lexeme(), value);
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

    public LoxValue getAt(int distance, String name) throws EnvironmentException {
        return ancestor(distance).values.get(name);
    }

    private Environment ancestor(int distance) {
       Environment env = this;
       for (int i = 0; i < distance; i++) {
          env = env.enclosing.get();
       }
       return env;
    }
}
