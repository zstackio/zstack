package org.zstack.zql.ast.parser.visitors

import org.zstack.zql.antlr4.ZQLBaseVisitor
import org.zstack.zql.antlr4.ZQLParser
import org.zstack.zql.ast.ASTNode

class ExprVisitor extends ZQLBaseVisitor<ASTNode.Expr> {
    @Override
    ASTNode.Expr visitExpr(ZQLParser.ExprContext ctx) {
        return new ASTNode.Expr(
                left: ctx.field().ID().collect {it.getText()},
                operator: ctx.operator().getText(),
                right: ctx.complexValue() == null ? null : ctx.complexValue().accept(new ValueVisitor())
        )
    }
}
