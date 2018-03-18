// Generated from ZQL.g4 by ANTLR 4.7

package org.zstack.zql.antlr4;

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
	 * Visit a parse tree produced by {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitZql(ZQLParser.ZqlContext ctx);
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
	 * Visit a parse tree produced by {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCondition(ZQLParser.ConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#conditions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditions(ZQLParser.ConditionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#queryTarget}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryTarget(ZQLParser.QueryTargetContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery(ZQLParser.QueryContext ctx);
}