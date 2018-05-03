// Generated from ZQL.g4 by ANTLR 4.7

package org.zstack.zql1.antlr4;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ZQLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ZQLVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by the {@code queryGrammar}
	 * labeled alternative in {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryGrammar(ZQLParser.QueryGrammarContext ctx);
	/**
	 * Visit a parse tree produced by the {@code countGrammar}
	 * labeled alternative in {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCountGrammar(ZQLParser.CountGrammarContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#entity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntity(ZQLParser.EntityContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#field}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField(ZQLParser.FieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator(ZQLParser.OperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(ZQLParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#logicalOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalOperator(ZQLParser.LogicalOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code simpleValue}
	 * labeled alternative in {@link ZQLParser#complexValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleValue(ZQLParser.SimpleValueContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subQueryValue}
	 * labeled alternative in {@link ZQLParser#complexValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubQueryValue(ZQLParser.SubQueryValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(ZQLParser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code nestCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNestCondition(ZQLParser.NestConditionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code simpleCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleCondition(ZQLParser.SimpleConditionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenthesisCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesisCondition(ZQLParser.ParenthesisConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#queryTarget}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryTarget(ZQLParser.QueryTargetContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#orderBy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderBy(ZQLParser.OrderByContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#limit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLimit(ZQLParser.LimitContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#offset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOffset(ZQLParser.OffsetContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#restrictByExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRestrictByExpr(ZQLParser.RestrictByExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#restrictBy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRestrictBy(ZQLParser.RestrictByContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#returnWithExprBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnWithExprBlock(ZQLParser.ReturnWithExprBlockContext ctx);
	/**
	 * Visit a parse tree produced by the {@code returnWithExprId}
	 * labeled alternative in {@link ZQLParser#returnWithExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnWithExprId(ZQLParser.ReturnWithExprIdContext ctx);
	/**
	 * Visit a parse tree produced by the {@code returnWithExprFunction}
	 * labeled alternative in {@link ZQLParser#returnWithExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnWithExprFunction(ZQLParser.ReturnWithExprFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#returnWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnWith(ZQLParser.ReturnWithContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#subQueryTarget}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubQueryTarget(ZQLParser.SubQueryTargetContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#subQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubQuery(ZQLParser.SubQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#filterByExprBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterByExprBlock(ZQLParser.FilterByExprBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#filterByExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterByExpr(ZQLParser.FilterByExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#filterBy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterBy(ZQLParser.FilterByContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery(ZQLParser.QueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#count}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCount(ZQLParser.CountContext ctx);
}