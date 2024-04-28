package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.Environment;
import com.craftinginterpreters.lox.Expr;
import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private Environment environment = new Environment();

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            // horrible mutable state
            // concurrency in jlox will be really funny
            this.environment = environment;

            for (Stmt statement : statements) {
               execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt){
       Object value = evaluate(stmt.expression);
       System.out.println(stringify(value));
       return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr);

        switch (expr.operator.type) {
        case BANG:
            return !isTruthy(right);
        case MINUS:
            checkNumberOperand(expr.operator, right);
            return -(double)right;
        }

        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr){
        return environment.get(expr.name);
    }

    private void checkNumberOperand(Token operator, Object operand) {
       if (operand instanceof Double) {
           return;
       }
       throw new RuntimeError(operator, "Operand must be a number.");
    }

    private Boolean isTruthy(Object obj) {
        if(obj == null) {
           return false;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }

        return true;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;

            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;

            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;

            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;

            case BANG_EQUAL:
                return !isEqual(left, right);

            case EQUAL_EQUAL:
                return isEqual(left, right);

            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case PLUS:
                // checkNumberOperands(expr.operator, left, right);
                if (left instanceof Double && right instanceof Double) {
                    return (Double) left + (Double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers xor two strings.");
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double) left / (double) right;
        }

        return null;
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
       if (left instanceof Double && right instanceof Double) {
           return;
       }
       throw new RuntimeError(operator, "Operands must be a numbers.");
    }

    private Boolean isEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null) {
            return false;
        }

        return a.equals(b);
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        // will be implemented later, togheter with
        return null;
    }

    // public void interpret(Expr expression) {
    //     System.out.println("expr interpreted");
    //     try {
    //         Object value = evaluate(expression);
    //         System.out.println(stringify(value));
    //     } catch (RuntimeError re) {
    //        Lox.runtimeError(re);
    //     }
    // }

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
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError re) {
           Lox.runtimeError(re);
        }
    }

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

}
