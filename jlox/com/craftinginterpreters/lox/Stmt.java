package com.craftinginterpreters.lox;

import java.util.List;

public abstract class Stmt {

    interface Visitor<R> {
        public R visitExpressionStmt(Expression stmt);
        public R visitPrintStmt(Print stmt);
        public R visitVarStmt(Var stmt);
        public R visitBlockStmt(Block stmt);
        public R visitIfStmt(If stmt);
    }

    public abstract <R> R accept(Visitor<R> visitor);

    public static class Expression extends Stmt {
        final Expr expression;

        public Expression(Expr expression) {
            this.expression = expression;
        }

        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }
    }

    public static class Print extends Stmt {
        final Expr expression;

        public Print(Expr expression) {
            this.expression = expression;
        }

        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitPrintStmt(this);
        }
    }

    public static class Var extends Stmt {
        final Expr initializer;
        final Token name;

        public Var(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }

        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitVarStmt(this);
        }
    }

    public static class Block extends Stmt {
        final List<Stmt> statements;

        public Block(List<Stmt> statements) {
            this.statements = statements;
        }

        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBlockStmt(this);
        }
    }

    public static class If extends Stmt {
        final Expr condition;
		final Stmt thenBranch;
		final Stmt elseBranch;

		public If(Expr condition, Stmt thenBranch, Stmt elseBranch){
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
		}

        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitIfStmt(this);
        }
    }

}
