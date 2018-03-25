package org.zstack.zql.ast.parser.visitors

import org.zstack.zql.antlr4.ZQLBaseVisitor
import org.zstack.zql.antlr4.ZQLParser
import org.zstack.zql.ast.ASTNode

class FilterByExprVisitor extends ZQLBaseVisitor<ASTNode.FilterByExpr> {
    @Override
    ASTNode.FilterByExpr visitFilterByExpr(ZQLParser.FilterByExprContext ctx) {
        return new ASTNode.FilterByExpr(
                filterName: ctx.ID().getText(),
                content: ctx.filterByExprBlock().getText()
        )
    }
}
