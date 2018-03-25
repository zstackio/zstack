package org.zstack.zql.ast.parser.visitors

import org.zstack.zql.antlr4.ZQLBaseVisitor
import org.zstack.zql.antlr4.ZQLParser
import org.zstack.zql.ast.ASTNode

class FilterByVisitor extends ZQLBaseVisitor<ASTNode.FilterBy> {
    @Override
    ASTNode.FilterBy visitFilterBy(ZQLParser.FilterByContext ctx) {
        return new ASTNode.FilterBy(exprs: ctx.filterByExpr().collect {it.accept(new FilterByExprVisitor())})
    }
}
