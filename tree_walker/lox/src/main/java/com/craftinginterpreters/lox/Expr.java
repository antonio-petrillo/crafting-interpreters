package com.craftinginterpreters.lox;

public sealed interface Expr permits
	Expr.Binary,
	Expr.Grouping,
	Expr.Literal,
	Expr.Unary,
	Expr.Variable,
	Expr.Assign
{

	public static record Binary(Expr left, Token operator, Expr right) implements Expr { }
	public static record Grouping(Expr expression) implements Expr { }
	public static record Literal(LoxValue value) implements Expr { }
	public static record Unary(Token operator, Expr right) implements Expr { }
	public static record Variable(Token name) implements Expr { }
	public static record Assign(Token name, Expr value) implements Expr { }

	public interface Visitor<T> {
		public T visitBinaryExpr(Binary expr) throws VisitException;
		public T visitGroupingExpr(Grouping expr) throws VisitException;
		public T visitLiteralExpr(Literal expr) throws VisitException;
		public T visitUnaryExpr(Unary expr) throws VisitException;
		public T visitVariableExpr(Variable expr) throws VisitException;
		public T visitAssignExpr(Assign expr) throws VisitException;
	}

	public static <T> T accept(Expr expr, Visitor<T> v) throws VisitException {
		return switch(expr) {
			case Binary e -> v.visitBinaryExpr(e);
			case Grouping e -> v.visitGroupingExpr(e);
			case Literal e -> v.visitLiteralExpr(e);
			case Unary e -> v.visitUnaryExpr(e);
			case Variable e -> v.visitVariableExpr(e);
			case Assign e -> v.visitAssignExpr(e);
			default -> throw new VisitException("Unknown Expr");
		};
	}
}
