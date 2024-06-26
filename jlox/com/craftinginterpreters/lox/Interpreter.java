package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	final Environment globals = new Environment();
    Environment environment = globals;

	private final Map<Expr, Integer> locals = new HashMap<>();

	public Interpreter() {
		globals.define("clock", new LoxCallable() {
				@Override
				public int arity() {
					return 0;
				}

				@Override
				public Object call(Interpreter interpreter, List<Object> arguments) {
					return (double)System.currentTimeMillis() / 1000.0;
				}

				@Override
				public String toString() {
					return "<native fn>";
				}
			});
	}

    @Override
    public Object visitLiteralExpr(Expr.Literal literal) {
        return literal.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping grouping) {
        return evaluate(grouping.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary unary) {
        Object right = evaluate(unary.right);

        switch (unary.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(unary.operator, right);
                return - (double) right;
        default:
            return null;
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary binary) {
        Object left = evaluate(binary.left);
        Object right = evaluate(binary.right);

        switch (binary.operator.type) {
        // arithmetic operators
        case MINUS:
            checkNumberOperands(binary.operator, left, right);
            return (double) left - (double) right;
        case PLUS:
            // string concatenation
            if (left instanceof String && right instanceof String) {
                return (String) left + (String) right;
            }
            if (left instanceof Double && right instanceof Double) {
                return (double) left + (double) right;
            }

            throw new RuntimeError(binary.operator, "Operands must be either numbers or strings.");
        case STAR:
            checkNumberOperands(binary.operator, left, right);
            return (double) left * (double) right;
        case SLASH:
            checkNumberOperands(binary.operator, left, right);
            return (double) left / (double) right;
        // comparison operators
        case GREATER:
            checkNumberOperands(binary.operator, left, right);
            return (double) left > (double) right;
        case GREATER_EQUAL:
            checkNumberOperands(binary.operator, left, right);
            return (double) left >= (double) right;
        case LESS:
            checkNumberOperands(binary.operator, left, right);
            return (double) left < (double) right;
        case LESS_EQUAL:
            checkNumberOperands(binary.operator, left, right);
            return (double) left <= (double) right;
        // equality operators
        case EQUAL_EQUAL:
            return isEqual(left, right);
        case BANG_EQUAL:
            return !isEqual(left, right);
        default:
            return null;
        }
    }

	@Override
	public Object visitCallExpr(Expr.Call expr) {
		Object callee = evaluate(expr.callee);

		List<Object> arguments = new ArrayList<>();
		for (Expr argument : expr.arguments) {
			arguments.add(evaluate(argument));
		}

		if (!(callee instanceof LoxCallable)) {
			throw new RuntimeError(expr.paren, "Can only call functions and classes.");
		}

		LoxCallable function = (LoxCallable)callee;
		if (arguments.size() != function.arity()) {
			throw new RuntimeError(expr.paren, "Expected " +
								   function.arity() + " arguments but goot " +
								   arguments.size() + ".");
		}

		return function.call(this, arguments);
	}

	@Override
	public Object visitGetExpr(Expr.Get expr) {
		Object object = evaluate(expr.object);
		if (object instanceof LoxInstance) {
			return ((LoxInstance) object).get(expr.name);
		}

		throw new RuntimeError(expr.name, "Only instances have properties.");
	}

    @Override
    public Object visitVariableExpr(Expr.Variable variable) {
        return environment.get(variable.name);
    }

	private Object lookUpVariable(Token name, Expr expr) {
		Integer distance = locals.get(expr);
		if (distance != null) {
			return environment.getAt(distance, name.lexeme);
		} else {
			return globals.get(name);
		}
	}

    @Override
    public Object visitAssignExpr(Expr.Assign assign) {
        Object value = evaluate(assign.value);
        environment.assign(assign.name, value);
        return value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical logical) {
        Object left = evaluate(logical.left);
        boolean isLeftTruthy = isTruthy(left);

        if (logical.operator.type == TokenType.OR)  {
            return isLeftTruthy ? left : evaluate(logical.right);
        } else {
            return isLeftTruthy ? evaluate(logical.right) : left;
        }
    }

    @Override
	public Object visitSetExpr(Expr.Set expr) {
		Object object = evaluate(expr.object);

		if(!(object instanceof LoxInstance)) {
			throw new RuntimeError(expr.name, "Only instances have fields.");
		}

		Object value = evaluate(expr.value);
		((LoxInstance)object).set(expr.name, value);
		return value;
	}

    @Override
	public Object visitSuperExpr(Expr.Super expr) {
		int distance = locals.get(expr);
		LoxClass superclass = (LoxClass) environment.getAt(distance, "super");
		LoxInstance object = (LoxInstance)environment.getAt(distance - 1, "this");
		LoxFunction method = superclass.findMethod(expr.method.lexeme);

		if (method == null) {
			throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
		}

		return method.bind(object);
	}

    @Override
	public Object visitThisExpr(Expr.This expr) {
		return lookUpVariable(expr.keyword, expr);
	}

    @Override
    public Void visitExpressionStmt(Stmt.Expression expressionStmt) {
        evaluate(expressionStmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
		LoxFunction function = new LoxFunction(stmt, environment, false);
		environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print printStmt) {
        Object result = evaluate(printStmt.expression);
        System.out.println(stringify(result));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
		Object value = null;
		if (stmt.value != null) {
			value = evaluate(stmt.value);
		}
		throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var varStmt) {
        Object value = null;
        if (varStmt.initializer != null) {
            value = evaluate(varStmt.initializer);
        }

        environment.define(varStmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block blockStmt) {
        executeBlock(blockStmt.statements, new Environment(environment));
        return null;
    }

	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		Object superclass = null;
		if (stmt.superclass != null) {
			superclass = evaluate(stmt.superclass);
			if (!(superclass instanceof LoxClass)) {
				throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
			}

		}

		environment.define(stmt.name.lexeme, null);

		if (stmt.superclass != null) {
			environment.define("super", superclass);
		}

		Map<String, LoxFunction> methods = new HashMap<>();
		for (Stmt.Function method : stmt.methods) {
			LoxFunction function = new LoxFunction(method, environment, method.name.lexeme.equals("init"));
			methods.put(method.name.lexeme, function);
		}

		LoxClass klass = new LoxClass(stmt.name.lexeme, (LoxClass)superclass, methods);


		if (stmt.superclass != null) {
			environment = environment.enclosing;
		}

		environment.assign(stmt.name, klass);
		return null;
	}

    public void executeBlock(List<Stmt> statements, Environment environment) {
        // retain a copy of the outer environment to restore after the block
        Environment original = this.environment;

        try {
            // statements within a block are interpreted in a new local env
            this.environment = environment;
            interpret(statements);
        } finally {
            this.environment = original;
        }
    }

    @Override
    public Void visitIfStmt(Stmt.If ifStmt) {
        boolean condition = isTruthy(evaluate(ifStmt.condition));
        if (condition) {
            execute(ifStmt.thenBranch);
        } else if (ifStmt.elseBranch != null) {
            execute(ifStmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While whileStmt) {
		try{
			while (isTruthy(evaluate(whileStmt.condition))) {
				execute(whileStmt.body);
			}
		} catch (BreakException e) {
			//do nothing, just break the loop;
		}
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break breakStmt) {
		throw new BreakException();
    }

	private static class BreakException extends RuntimeException {

	}

    public void interpret(List<Stmt> statements) {
		if (statements.size() == 1) {
			Stmt stmt = statements.get(0);
			if (stmt instanceof Stmt.Expression) {
				Object value = evaluate(((Stmt.Expression)stmt).expression);
				System.out.println(stringify(value));
				return;
			}
		}
        try {
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
		}
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

	void resolve(Expr expr, int depth) {
		locals.put(expr, depth);
	}

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private String stringify(Object obj) {
        if (obj == null) return "nil";

        if (obj instanceof Double) {
            String strRepresentation = obj.toString();
            if (strRepresentation.endsWith(".0")) {
                // trim and display as integer
                return strRepresentation.substring(0, strRepresentation.length() - 2);
            }
        }

        return obj.toString();
    }

    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (boolean) value;
        return true;
    }

    private boolean isEqual(Object o1, Object o2) {
        if (o1 == null && o2 == null) return true;
        if (o1 == null) return false;
        return o1.equals(o2);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object operand1, Object operand2) {
        if (operand1 instanceof Double && operand2 instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be a number.");
    }
}
