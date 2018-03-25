package org.zstack.zql.ast.parser.visitors

import org.zstack.zql.antlr4.ZQLBaseVisitor
import org.zstack.zql.antlr4.ZQLParser
import org.zstack.zql.ast.ASTNode

class ReturnWithVisitor extends ZQLBaseVisitor<ASTNode.ReturnWith> {
    @Override
    ASTNode.ReturnWith visitReturnWith(ZQLParser.ReturnWithContext ctx) {
        return new ASTNode.ReturnWith(exprs: ctx.returnWithExpr().collect {it.accept(new ReturnWithExprVisitor())})
    }
}
