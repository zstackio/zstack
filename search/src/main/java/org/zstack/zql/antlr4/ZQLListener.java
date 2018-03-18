// Generated from ZQL.g4 by ANTLR 4.7

package org.zstack.zql.antlr4;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ZQLParser}.
 */
public interface ZQLListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 */
	void enterZql(ZQLParser.ZqlContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 */
	void exitZql(ZQLParser.ZqlContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#entity}.
	 * @param ctx the parse tree
	 */
	void enterEntity(ZQLParser.EntityContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#entity}.
	 * @param ctx the parse tree
	 */
	void exitEntity(ZQLParser.EntityContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#field}.
	 * @param ctx the parse tree
	 */
	void enterField(ZQLParser.FieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#field}.
	 * @param ctx the parse tree
	 */
	void exitField(ZQLParser.FieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#operator}.
	 * @param ctx the parse tree
	 */
	void enterOperator(ZQLParser.OperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#operator}.
	 * @param ctx the parse tree
	 */
	void exitOperator(ZQLParser.OperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(ZQLParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(ZQLParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#logicalOperator}.
	 * @param ctx the parse tree
	 */
	void enterLogicalOperator(ZQLParser.LogicalOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#logicalOperator}.
	 * @param ctx the parse tree
	 */
	void exitLogicalOperator(ZQLParser.LogicalOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(ZQLParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(ZQLParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nestCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterNestCondition(ZQLParser.NestConditionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nestCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitNestCondition(ZQLParser.NestConditionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterSimpleCondition(ZQLParser.SimpleConditionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitSimpleCondition(ZQLParser.SimpleConditionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesisCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterParenthesisCondition(ZQLParser.ParenthesisConditionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesisCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitParenthesisCondition(ZQLParser.ParenthesisConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#queryTarget}.
	 * @param ctx the parse tree
	 */
	void enterQueryTarget(ZQLParser.QueryTargetContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#queryTarget}.
	 * @param ctx the parse tree
	 */
	void exitQueryTarget(ZQLParser.QueryTargetContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#orderBy}.
	 * @param ctx the parse tree
	 */
	void enterOrderBy(ZQLParser.OrderByContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#orderBy}.
	 * @param ctx the parse tree
	 */
	void exitOrderBy(ZQLParser.OrderByContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#limit}.
	 * @param ctx the parse tree
	 */
	void enterLimit(ZQLParser.LimitContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#limit}.
	 * @param ctx the parse tree
	 */
	void exitLimit(ZQLParser.LimitContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#offset}.
	 * @param ctx the parse tree
	 */
	void enterOffset(ZQLParser.OffsetContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#offset}.
	 * @param ctx the parse tree
	 */
	void exitOffset(ZQLParser.OffsetContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#restrictByExpr}.
	 * @param ctx the parse tree
	 */
	void enterRestrictByExpr(ZQLParser.RestrictByExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#restrictByExpr}.
	 * @param ctx the parse tree
	 */
	void exitRestrictByExpr(ZQLParser.RestrictByExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#restrictBy}.
	 * @param ctx the parse tree
	 */
	void enterRestrictBy(ZQLParser.RestrictByContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#restrictBy}.
	 * @param ctx the parse tree
	 */
	void exitRestrictBy(ZQLParser.RestrictByContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#returnWithExpr}.
	 * @param ctx the parse tree
	 */
	void enterReturnWithExpr(ZQLParser.ReturnWithExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#returnWithExpr}.
	 * @param ctx the parse tree
	 */
	void exitReturnWithExpr(ZQLParser.ReturnWithExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#returnWith}.
	 * @param ctx the parse tree
	 */
	void enterReturnWith(ZQLParser.ReturnWithContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#returnWith}.
	 * @param ctx the parse tree
	 */
	void exitReturnWith(ZQLParser.ReturnWithContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#query}.
	 * @param ctx the parse tree
	 */
	void enterQuery(ZQLParser.QueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#query}.
	 * @param ctx the parse tree
	 */
	void exitQuery(ZQLParser.QueryContext ctx);
}