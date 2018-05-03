package org.zstack.zql1.ast.parser.visitors1

import org.zstack.zql1.antlr4.ZQLBaseVisitor
import org.zstack.zql1.antlr4.ZQLParser
import org.zstack.header.zql.ASTNode

class FilterByExprVisitor extends ZQLBaseVisitor<ASTNode.FilterByExpr> {
    @Override
    ASTNode.FilterByExpr visitFilterByExpr(ZQLParser.FilterByExprContext ctx) {
        return new ASTNode.FilterByExpr(
                filterName: ctx.ID().getText(),
                content: ctx.filterByExprBlock().getText()
        )
    }
}
