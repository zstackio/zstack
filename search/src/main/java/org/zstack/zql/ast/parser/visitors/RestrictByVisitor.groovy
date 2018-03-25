package org.zstack.zql.ast.parser.visitors

import org.zstack.zql.antlr4.ZQLBaseVisitor
import org.zstack.zql.antlr4.ZQLParser
import org.zstack.zql.ast.ASTNode

class RestrictByVisitor extends ZQLBaseVisitor<ASTNode.RestrictBy> {
    @Override
    ASTNode.RestrictBy visitRestrictBy(ZQLParser.RestrictByContext ctx) {
        return new ASTNode.RestrictBy(exprs: ctx.restrictByExpr().collect {it.accept(new RestrictExprVisitor())})
    }
}
