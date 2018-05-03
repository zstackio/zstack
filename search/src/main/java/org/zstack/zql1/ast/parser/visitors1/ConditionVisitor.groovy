package org.zstack.zql1.ast.parser.visitors1

import org.zstack.zql1.antlr4.ZQLBaseVisitor
import org.zstack.zql1.antlr4.ZQLParser
import org.zstack.header.zql.ASTNode


class ConditionVisitor extends ZQLBaseVisitor<ASTNode.Condition> {
    @Override
    ASTNode.Condition visitNestCondition(ZQLParser.NestConditionContext ctx) {
        return new ASTNode.LogicalOperator(
                left: ctx.left.accept(new ConditionVisitor()),
                operator: ctx.op == null ?: ctx.op.getText(),
                right: ctx.right == null ? null : ctx.right.accept(new ConditionVisitor())
        )
    }

    ASTNode.Condition visitSimpleCondition(ZQLParser.SimpleConditionContext ctx) {
        return ctx.expr().accept(new ExprVisitor())
    }

    @Override
    ASTNode.Condition visitParenthesisCondition(ZQLParser.ParenthesisConditionContext ctx) {
        return ctx.condition().accept(new ConditionVisitor())
    }
}
