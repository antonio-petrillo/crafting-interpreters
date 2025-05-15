package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Optional;

public sealed interface Stmt permits
    Stmt.Expression,
    Stmt.Print,
    Stmt.Var,
    Stmt.Block,
    Stmt.If,
    Stmt.While,
    Stmt.Function,
    Stmt.Return,
    Stmt.Class
{

    public static record Expression(Expr expression) implements Stmt {  }
    public static record Var(Token name, Optional<Expr> initializer) implements Stmt {  }
    public static record Print(Expr expression) implements Stmt {  }
    public static record Block(List<Stmt> statements) implements Stmt {  }
    public static record If(Expr condition, Stmt thenBranch, Optional<Stmt> elseBranch) implements Stmt {  }
    public static record While(Expr condition, Stmt body) implements Stmt {  }
    public static record Function(Token name, List<Token> params, List<Stmt> body) implements Stmt {  }
    public static record Return(Token keyword, Optional<Expr> value) implements Stmt {
        public static class ReturnException extends VisitException {
            private final LoxValue value;
            public ReturnException(LoxValue value) {
                super("");
                this.value = value;
            }
            public LoxValue getValue() {
                return this.value;
            }
        }
    }
    public static record Class(Token name, List<Stmt.Function> methods) implements Stmt {  }

    public interface Visitor<T> {
        T visitExpressionStmt(Expression stmt) throws VisitException;
        T visitPrintStmt(Print stmt) throws VisitException;
        T visitVarStmt(Var stmt) throws VisitException;
        T visitBlockStmt(Block stmt) throws VisitException;
        T visitIfStmt(If stmt) throws VisitException;
        T visitWhileStmt(While stmt) throws VisitException;
        T visitFunctionStmt(Function stmt) throws VisitException;
        T visitReturnStmt(Return stmt) throws VisitException;
        T visitClassStmt(Stmt.Class stmt) throws VisitException;
    }

    public static <T> T accept(Stmt stmt, Visitor<T> v) throws VisitException {
        return switch (stmt) {
            case Expression s -> v.visitExpressionStmt(s);
            case Print s -> v.visitPrintStmt(s);
            case Var s -> v.visitVarStmt(s);
            case Block s -> v.visitBlockStmt(s);
            case If s -> v.visitIfStmt(s);
            case While s -> v.visitWhileStmt(s);
            case Function s -> v.visitFunctionStmt(s);
            case Return s -> v.visitReturnStmt(s);
            case Stmt.Class s -> v.visitClassStmt(s);
            default -> throw new VisitException("Unknown Stmt");
        };
    }

}
