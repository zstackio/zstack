package org.zstack.zql1.ast.parser.visitors1

import org.zstack.zql1.antlr4.ZQLBaseVisitor
import org.zstack.zql1.antlr4.ZQLParser
import org.zstack.header.zql.ASTNode

class OrderByVisitor extends ZQLBaseVisitor<ASTNode.OrderBy> {
    @Override
    ASTNode.OrderBy visitOrderBy(ZQLParser.OrderByContext ctx) {
        return new ASTNode.OrderBy(
                direction: ctx.ORDER_BY_VALUE().getText(),
                field: ctx.ID().getText()
        )
    }
}
