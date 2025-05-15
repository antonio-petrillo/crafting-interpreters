package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.craftinginterpreters.lox.Expr.Get;
import com.craftinginterpreters.lox.Expr.Set;
import com.craftinginterpreters.lox.Expr.This;

import static com.craftinginterpreters.lox.TokenType.*;
import static com.craftinginterpreters.lox.Expr.*;
import static com.craftinginterpreters.lox.Stmt.*;

public class Interpreter implements Expr.Visitor<LoxValue>, Stmt.Visitor<Void> {

    private final Lox lox;

    private final Environment globals = new Environment();
    private Environment environment = globals;

    private final Map<Expr, Integer> locals = new HashMap<>();

    public Interpreter(Lox lox) {
        this.lox = lox;
    }

    public Lox getLox() {
        return lox;
    }

    public LoxValue evaluate(Expr expr) throws VisitException {
        return Expr.accept(expr, this);
    }

    public void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    private LoxValue lookUpVariable(Token name, Expr expr) throws  EnvironmentException {
       Optional<Integer> distance = Optional.ofNullable(locals.get(expr));
       if (!distance.isEmpty()) {
           return environment.getAt(distance.get(), name.lexeme());
       } else {
           return globals.get(name);
       }
    }

	@Override
    public LoxValue visitBinaryExpr(Binary expr) throws VisitException {
        LoxValue left = evaluate(expr.left());
        LoxValue right = evaluate(expr.right());

        return switch (expr.operator().type()) {
            case PLUS -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield new LoxNum(l.num() + r.num());
                } else if (left instanceof LoxStr l && right instanceof LoxStr r) {
                    yield new LoxStr(String.format("%s%s", l.str(), r.str()));
                } else if (left instanceof LoxStr l && right instanceof LoxNum r) {
                    yield new LoxStr(String.format("%s%f", l.str(), r.num()));
                }
                throw new VisitException("Mismateched type, in PLUS both operand must be both str or both num");
            }
            case MINUS -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield new LoxNum(l.num() - r.num());
                }
                throw new VisitException("Mismateched type, in MINUS both operand must be both num");
            }
            case STAR -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield new LoxNum(l.num() * r.num());
                }
                throw new VisitException("Mismateched type, in STAR both operand must be both num");
            }
            case SLASH -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield new LoxNum(l.num() / r.num());
                }
                throw new VisitException("Mismateched type, in SLASH both operand must be both num");
            }
            case LESS -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield LoxValue.Intern.fromBool(l.num() < r.num());
                }
                throw new VisitException("Mismateched type, in LESS both operand must be both num");
            }
            case LESS_EQUAL -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield LoxValue.Intern.fromBool(l.num() <= r.num());
                }
                throw new VisitException("Mismateched type, in LESS_EQUAL both operand must be both num");
            }
            case GREATER -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield LoxValue.Intern.fromBool(l.num() > r.num());
                }
                throw new VisitException("Mismateched type, in GREATER both operand must be both num");
            }
            case GREATER_EQUAL -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield LoxValue.Intern.fromBool(l.num() >= r.num());
                }
                throw new VisitException("Mismateched type, in GREATER_EQUAL both operand must be both num");
            }
            case BANG_EQUAL -> {
                yield LoxValue.Intern.fromBool(!left.equals(right));
            }
            case EQUAL_EQUAL -> {
                yield LoxValue.Intern.fromBool(left.equals(right));
            }

            default -> throw new VisitException(String.format("Unsupporte Operation: %s", expr.operator().toString()));
        };
 	}

	@Override
	public LoxValue visitGroupingExpr(Grouping expr) throws VisitException {
        return evaluate(expr.expression());
	}

	@Override
	public LoxValue visitLiteralExpr(Literal expr) throws VisitException {
        return expr.value();
	}

    @Override
    public LoxValue visitUnaryExpr(Unary expr) throws VisitException {
        LoxValue right = evaluate(expr.right());
        return switch (expr.operator().type()) {
            case MINUS -> {
                yield switch (right) {
                    case LoxNum n -> new LoxNum(-n.num());
                    default -> throw new IllegalStateException("Invalid arguments for MINUS '-'");
                };
            }
            case BANG -> {
                yield isTruthy(right).negate();
            }
            default -> throw new IllegalStateException("Invalid Operator");
        };
    }

    private LoxValue.Intern isTruthy(LoxValue value) {
        return switch (value) {
            case LoxValue.Intern intern -> switch (intern) {
                case LoxValue.Intern.FALSE -> LoxValue.Intern.FALSE;
                case LoxValue.Intern.TRUE -> LoxValue.Intern.TRUE;
                case LoxValue.Intern.NIL -> LoxValue.Intern.FALSE;
            };
            default -> LoxValue.Intern.TRUE;
        };
    }

    private boolean isTruthyValue(LoxValue value) {
        return switch (isTruthy(value)) {
            case LoxValue.Intern.FALSE -> false;
            case LoxValue.Intern.TRUE -> true;
            case LoxValue.Intern.NIL -> false;
        };
    }

    public void interpret(List<Stmt> statements) throws VisitException {
        for (Stmt stmt : statements) {
            execute(stmt);
        }
    }

    private void execute(Stmt stmt) throws VisitException {
        Stmt.accept(stmt, this);
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) throws VisitException {
        evaluate(stmt.expression());
        return null;
    }

    @Override
    public Void visitPrintStmt(Print stmt) throws VisitException {
        LoxValue value = evaluate(stmt.expression());
        System.out.println(value.toString());
        return null;
    }

    @Override
    public Void visitVarStmt(Var stmt) throws VisitException {
        LoxValue value = LoxValue.Intern.NIL;
        if (!stmt.initializer().isEmpty()) {
           value = evaluate(stmt.initializer().get());
        }
        environment.define(stmt.name().lexeme(), value);
        return null;
    }

    @Override
    public LoxValue visitVariableExpr(Variable expr) throws VisitException {
        try {
            return lookUpVariable(expr.name(), expr);
        } catch (EnvironmentException ee) {
            throw new VisitException(String.format("Undefined variable: %s.", expr.name()));
        }
    }

    @Override
    public LoxValue visitAssignExpr(Assign expr) throws VisitException {
        LoxValue value = evaluate(expr.value());
        try {
            Optional<Integer> distance = Optional.ofNullable(locals.get(expr));
            if (distance.isEmpty()) {
                environment.assign(expr.name(), value);
            } else {
                environment.assignAt(distance.get(), expr.name(), value);
            }
        } catch (EnvironmentException ee) {
            throw new VisitException(String.format("Undefined variable name: %s.", expr.name()));
        }
        return value;
    }

   @Override
   public Void visitBlockStmt(Block stmt) throws VisitException {
       executeBlock(stmt.statements(), new Environment(environment));
       return null;
   }

    public void executeBlock(List<Stmt> statements, Environment env) throws VisitException {
        Environment prev = this.environment;
        try {
            this.environment = env;
            for (Stmt stmt : statements) {
               execute(stmt);
            }
        } finally {
            this.environment = prev;
        }
    }

    @Override
    public Void visitIfStmt(If stmt) throws VisitException {
        if(isTruthyValue(evaluate(stmt.condition()))) {
            execute(stmt.thenBranch());
        } else if (!stmt.elseBranch().isEmpty()) {
            execute(stmt.elseBranch().get());
        }
        return null;
    }

    @Override
    public LoxValue visitLogicalExpr(Logical expr) throws VisitException {
        LoxValue left = evaluate(expr.left());


        if (expr.operator().type() == OR) {
            if(isTruthyValue(left)) return left;
        } else {
            if(!isTruthyValue(left)) return left;
        }

        return evaluate(expr.right());
    }

    @Override
    public Void visitWhileStmt(While stmt) throws VisitException {
        while (isTruthyValue(evaluate(stmt.condition()))) {
            execute(stmt.body());
        }
        return null;
    }

    @Override
    public LoxValue visitCallExpr(Call expr) throws VisitException {
        LoxValue callee = evaluate(expr.callee());
        // I fucking hate the fact that I can't use the stream.map because my lambda throws.
        // I understand the why, but still, fuck you java.
        // List<LoxValue> arguments = expr.arguments()
        //     .stream()
        //     .map(e -> evaluate(e))
        //     .toList();
        List<LoxValue> arguments = new ArrayList<>();
        for(Expr e : expr.arguments()) {
            arguments.add(evaluate(e));
        }

        Optional<LoxCallable> maybeFunction = Optional.empty();
        if (callee instanceof LoxCallable lc) {
            maybeFunction = Optional.of(lc);
        }
        if (maybeFunction.isEmpty()) {
            throw new VisitException(String.format("ERR: %s is not callable.", callee.toString()));
        }
        LoxCallable function = maybeFunction.get();
        if (function.arity() != arguments.size()) {
            throw new VisitException(String.format("ERR: %s require %d arguments, received %d.", callee.toString(), function.arity(), arguments.size()));
        }

        return function.call(this, List.<LoxValue>copyOf(arguments));
    }

    @Override
    public Void visitFunctionStmt(Function stmt) throws VisitException {
        LoxFunction function = new LoxFunction(stmt, environment, false);
        environment.define(stmt.name().lexeme(), function);
        return null;
    }

    @Override
    public Void visitReturnStmt(Return stmt) throws VisitException {
        LoxValue retValue = LoxValue.Intern.NIL;
        if (!stmt.value().isEmpty())
            retValue = evaluate(stmt.value().get());

        throw new Return.ReturnException(retValue);
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) throws VisitException {
        environment.define(stmt.name().lexeme(), LoxValue.Intern.NIL);
        Map<String, LoxFunction> methods = new HashMap<>();
        for(Function method : stmt.methods()) {
            LoxFunction fn = new LoxFunction(method, environment, method.name().lexeme().equals("init"));
            methods.put(method.name().lexeme(), fn);
        }
        LoxClass clazz = new LoxClass(stmt.name().lexeme(), methods);
        try {
            environment.assign(stmt.name(), clazz);
        } catch (EnvironmentException ee) {
            throw new VisitException(String.format("Error defining %s, maybe it's already declared.", stmt.name().lexeme()));
        }
        return null;
    }

    @Override
    public LoxValue visitGetExpr(Get expr) throws VisitException {
        LoxValue obj = evaluate(expr.obj());
        if (obj instanceof LoxInstance instance) {
            try {
                return instance.get(expr.name());
            } catch (LoxInstance.InstanceException ie) {
                throw new VisitException(ie.getMessage());
            }
        }
        throw new VisitException(String.format("The %s is not applicable to given object.", expr.name()));
    }

    @Override
    public LoxValue visitSetExpr(Set expr) throws VisitException {
        LoxValue obj = evaluate(expr.obj());

        if(obj instanceof LoxInstance instance) {
            LoxValue value = evaluate(expr.value());
            instance.set(expr.name(), value);
            return value;
        }

        throw new VisitException("Setting property on non object instance value.");
    }

    @Override
    public LoxValue visitThisExpr(This expr) throws VisitException {
        try {
            return lookUpVariable(expr.keyword(), expr);
        } catch(EnvironmentException ee) {
            throw new VisitException("Keyword 'this' refer to a NIL object.");
        }
    }

}
