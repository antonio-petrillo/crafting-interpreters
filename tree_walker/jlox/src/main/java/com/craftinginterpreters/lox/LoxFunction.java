package com.craftinginterpreters.lox;

import java.util.Iterator;
import java.util.List;

import static com.craftinginterpreters.lox.Stmt.*;

public record LoxFunction(Function declaration, Environment closure) implements LoxCallable {

    @Override
        public LoxValue call(Interpreter interpreter, List<LoxValue> arguments) {
            if(declaration.params().size() != arguments.size()) {
                throw new IllegalArgumentException(STR."Mismatched number of arguments in function call, expected \{declaration.params().size()}, got \{ arguments.size()}.");
            }
            Environment env = new Environment(closure);
            Iterator<LoxValue> iter = arguments.iterator();
            for(Token param : declaration.params()){
                env.define(param.lexeme(), iter.next());
            }

            try {
                interpreter.executeBlock(declaration.body(), env);
            } catch(Return.ReturnException ret) {
                return ret.getValue();
            } catch(VisitException ve) {
                interpreter.getLox().error(declaration.name(), STR."Error in \{declaration.name().lexeme()} function call.");
            }
            return LoxValue.Intern.NIL;
        }

    @Override
    public int arity() {
        return declaration.params().size();
    }

    @Override
        public String toString() {
            return STR."<fn \{declaration.name().lexeme()}>.";
        }
}
