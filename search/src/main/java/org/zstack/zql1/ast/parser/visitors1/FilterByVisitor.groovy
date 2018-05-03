package org.zstack.zql1.ast.parser.visitors1

import org.zstack.zql1.antlr4.ZQLBaseVisitor
import org.zstack.zql1.antlr4.ZQLParser
import org.zstack.header.zql.ASTNode

class FilterByVisitor extends ZQLBaseVisitor<ASTNode.FilterBy> {
    @Override
    ASTNode.FilterBy visitFilterBy(ZQLParser.FilterByContext ctx) {
        return new ASTNode.FilterBy(exprs: ctx.filterByExpr().collect {it.accept(new FilterByExprVisitor())})
    }
}
