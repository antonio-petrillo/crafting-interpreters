package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.util.function.Supplier;
import java.util.function.Function;

public final class LoxClass implements LoxValue, LoxCallable {

    private final String name;
    private final Map<String, LoxFunction> methods;
    private final Optional<LoxClass> superclass;

    public LoxClass(String name, Optional<LoxClass> superclass, Map<String, LoxFunction> methods) {
        this.name = name;
        this.superclass = superclass;
        this.methods = methods;
    }

    public String getName() {
        return name;
    }

    // NOTE: Even with an Optional<? extends Obj> I let a NullPointerException to sneak into
    // That's because this Optional is bullshit!
    // It can be null itself!
    public Optional<LoxFunction> findMethod(String name) {
        return Optional.ofNullable(methods.get(name))
            .or(() -> {
                    var sup = superclass.map(cl -> cl.findMethod(name));
                    if (sup.isEmpty()) {
                        return Optional.empty();
                    }
                    return sup.get();
                });
    }

    @Override
    public String toString() {
        return String.format("<class %s>", name);
    }

    @Override
    public LoxValue call(Interpreter interpreter, List<LoxValue> arguments) {
        LoxInstance instance = new LoxInstance(this);
        Optional<LoxFunction> initializer = findMethod("init");
        if(!initializer.isEmpty())
            initializer.get().bind(instance).call(interpreter, arguments);

        return instance;
    }

    @Override
    public int arity() {
        Optional<LoxFunction> initializer = findMethod("init");
        if (initializer.isEmpty())
            return 0;
        return initializer.get().arity();
    }

}
