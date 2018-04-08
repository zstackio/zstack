package org.zstack.zql.ast.parser.visitors

import org.zstack.zql.antlr4.ZQLBaseVisitor
import org.zstack.zql.antlr4.ZQLParser
import org.zstack.header.zql.ASTNode

class RestrictExprVisitor extends ZQLBaseVisitor<ASTNode.RestrictExpr> {
    @Override
    ASTNode.RestrictExpr visitRestrictByExpr(ZQLParser.RestrictByExprContext ctx) {
        return new ASTNode.RestrictExpr(
                entity: ctx.entity().getText(),
                field: ctx.ID().getText(),
                operator: ctx.operator().getText(),
                value: ctx.value() == null ? null : ctx.value().accept(new ValueVisitor())
        )
    }
}
