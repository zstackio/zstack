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
	 * Enter a parse tree produced by {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterCondition(ZQLParser.ConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitCondition(ZQLParser.ConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#conditions}.
	 * @param ctx the parse tree
	 */
	void enterConditions(ZQLParser.ConditionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#conditions}.
	 * @param ctx the parse tree
	 */
	void exitConditions(ZQLParser.ConditionsContext ctx);
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