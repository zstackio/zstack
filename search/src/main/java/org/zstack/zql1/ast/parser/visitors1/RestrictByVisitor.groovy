package org.zstack.zql1.ast.parser.visitors1

import org.zstack.zql1.antlr4.ZQLBaseVisitor
import org.zstack.zql1.antlr4.ZQLParser
import org.zstack.header.zql.ASTNode

class RestrictByVisitor extends ZQLBaseVisitor<ASTNode.RestrictBy> {
    @Override
    ASTNode.RestrictBy visitRestrictBy(ZQLParser.RestrictByContext ctx) {
        return new ASTNode.RestrictBy(exprs: ctx.restrictByExpr().collect {it.accept(new RestrictExprVisitor())})
    }
}
