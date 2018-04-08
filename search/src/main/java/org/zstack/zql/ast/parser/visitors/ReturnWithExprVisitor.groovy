package org.zstack.zql.ast.parser.visitors

import org.zstack.zql.antlr4.ZQLBaseVisitor
import org.zstack.zql.antlr4.ZQLParser
import org.zstack.zql.ast.ASTNode

class ReturnWithExprVisitor extends ZQLBaseVisitor<ASTNode.ReturnWithExpr> {
    @Override
    ASTNode.ReturnWithIDExpr visitReturnWithExprId(ZQLParser.ReturnWithExprIdContext ctx) {
        return new ASTNode.ReturnWithIDExpr(names: ctx.ID().collect { it.getText() })
    }

    @Override
    ASTNode.ReturnWithBlockExpr visitReturnWithExprFunction(ZQLParser.ReturnWithExprFunctionContext ctx) {
        return new ASTNode.ReturnWithBlockExpr(name: ctx.ID().getText(), content: ctx.returnWithExprBlock().getText())
    }
}
