package com.craftinginterpreters.lox;

public class Interpreter implements Expr.Visitor<LoxValue> {
    private Lox lox;

    public Interpreter(Lox lox) {
        this.lox = lox;
    }

    public LoxValue evaluate(Expr expr) {
        return expr.accept(this);
    }

	@Override
    public LoxValue visitBinaryExpr(Binary expr) {
        LoxValue left = evaluate(expr.left());
        LoxValue right = evaluate(expr.right());

        return switch (expr.operator().type()) {
            case PLUS -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield new LoxNum(l.num() + r.num());
                } else if (left instanceof LoxStr l && right instanceof LoxStr r) {
                    yield new LoxStr(String.format("%s%s", l.str(), r.str()));
                }
                throw new RuntimeError(expr.operator(), "Mismatched type for PLUS operation.");
            }
            case MINUS -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield new LoxNum(l.num() - r.num());
                }
                throw new RuntimeError(expr.operator(), "Mismatched type for MINUS operation.");
            }
            case STAR -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield new LoxNum(l.num() * r.num());
                }
                throw new RuntimeError(expr.operator(), "Mismatched type for STAR operation.");
            }
            case SLASH -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield new LoxNum(l.num() / r.num());
                }
                throw new RuntimeError(expr.operator(), "Mismatched type for SLASH operation.");
            }
            case LESS -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield LoxValue.Intern.fromBool(l.num() < r.num());
                }
                throw new RuntimeError(expr.operator(), "Mismatched type for LESS operation.");
            }
            case LESS_EQUAL -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield LoxValue.Intern.fromBool(l.num() <= r.num());
                }
                throw new RuntimeError(expr.operator(), "Mismatched type for LESS_EQUAL operation.");
            }
            case GREATER -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield LoxValue.Intern.fromBool(l.num() > r.num());
                }
                throw new RuntimeError(expr.operator(), "Mismatched type for GREATER operation.");
            }
            case GREATER_EQUAL -> {
                if (left instanceof LoxNum l && right instanceof LoxNum r) {
                    yield LoxValue.Intern.fromBool(l.num() >= r.num());
                }
                throw new RuntimeError(expr.operator(), "Mismatched type for GREATER_EQUAL operation.");
            }
            case BANG_EQUAL -> {
                yield LoxValue.Intern.fromBool(!left.equals(right));
            }
            case EQUAL_EQUAL -> {
                yield LoxValue.Intern.fromBool(left.equals(right));
            }

            default -> throw new RuntimeError(expr.operator(), "Invalid Operator");
        };
 	}

	@Override
	public LoxValue visitGroupingExpr(Grouping expr) {
        return evaluate(expr.expression());
	}

	@Override
	public LoxValue visitLiteralExpr(Literal expr) {
        return expr.value();
	}

    @Override
    public LoxValue visitUnaryExpr(Unary expr) {
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

    public void interpret(Expr expression) {
        try {
            LoxValue value = evaluate(expression);
            System.out.println(value.toString());
        } catch (RuntimeError re) {
            lox.runtimeError(re);
        }
    }

}
