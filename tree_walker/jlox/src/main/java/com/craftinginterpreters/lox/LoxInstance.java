package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class LoxInstance implements LoxValue {
    private final LoxClass clazz;
    private final Map<String, LoxValue> fields = new HashMap<>();

    public static class InstanceException extends Exception {
        public InstanceException(String msg) { super(msg); }
    }

    public LoxInstance(LoxClass clazz){
        this.clazz = clazz;
    }

    public LoxValue get(Token name) throws InstanceException {
        if (fields.containsKey(name.lexeme())) {
            return fields.get(name.lexeme());
        }

        Optional<LoxFunction> method = clazz.findMethod(name.lexeme());

        if(!method.isEmpty()) {
            return method.get().bind(this);
        }

        throw new InstanceException(String.format("The %s instance doesn't have field %s.", clazz.toString(), name.lexeme()));
    }

    public void set(Token name, LoxValue value) {
        fields.put(name.lexeme(), value);
    }

    @Override
    public String toString() {
        return String.format("<%s instance>", clazz.getName());
    }

}
