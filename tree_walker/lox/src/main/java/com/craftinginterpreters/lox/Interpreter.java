package com.craftinginterpreters.lox;

import java.util.List;

public class Interpreter implements Expr.Visitor<LoxValue>, Stmt.Visitor<Void> {
    private Lox lox;

    public Interpreter(Lox lox) {
        this.lox = lox;
    }

    public LoxValue evaluate(Expr expr) throws VisitException {
        return Expr.accept(expr, this);
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

}
