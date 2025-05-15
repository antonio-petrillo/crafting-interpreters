package com.craftinginterpreters.lox;

import java.util.Iterator;
import java.util.List;

import static com.craftinginterpreters.lox.Stmt.*;

public record LoxFunction(Function declaration, Environment closure, boolean isInitializer) implements LoxCallable {

    public LoxFunction bind(LoxInstance instance) {
        Environment env = new Environment(closure);
        env.define("this", instance);
        return new LoxFunction(declaration, env, isInitializer);
    }

    @Override
        public LoxValue call(Interpreter interpreter, List<LoxValue> arguments) {
            if(declaration.params().size() != arguments.size()) {
                String msg = String.format("Mismatched number of arguments in function call, expected %d, got %d.", declaration.params().size(), arguments.size());
                throw new IllegalArgumentException(msg);
            }
            Environment env = new Environment(closure);
            Iterator<LoxValue> iter = arguments.iterator();
            for(Token param : declaration.params()){
                env.define(param.lexeme(), iter.next());
            }

            try {
                interpreter.executeBlock(declaration.body(), env);
            } catch(Return.ReturnException ret) {
                if(isInitializer)
                    try {
                        return closure.getAt(0, "this");
                    } catch (EnvironmentException ee) {
                        return LoxValue.Intern.NIL;
                    }

                return ret.getValue();
            } catch(VisitException ve) {
                String msg = String.format("Error in %s function call.", declaration.name().lexeme());
                interpreter.getLox().error(declaration.name(), msg);
            }

            try {
                if(isInitializer)
                    return closure.getAt(0, "this");
            } catch (EnvironmentException ee) {
               // do nothing
            }

            return LoxValue.Intern.NIL;
    }

    @Override
    public int arity() {
        return declaration.params().size();
    }

    @Override
        public String toString() {
            return String.format("<fn %s>.", declaration.name().lexeme());
        }
}
