package org.zstack.zql1.ast.parser.visitors1

import org.zstack.zql1.antlr4.ZQLBaseVisitor
import org.zstack.zql1.antlr4.ZQLParser
import org.zstack.header.zql.ASTNode

class ReturnWithVisitor extends ZQLBaseVisitor<ASTNode.ReturnWith> {
    @Override
    ASTNode.ReturnWith visitReturnWith(ZQLParser.ReturnWithContext ctx) {
        return new ASTNode.ReturnWith(exprs: ctx.returnWithExpr().collect {it.accept(new ReturnWithExprVisitor())})
    }
}
