package org.zstack.zql.ast.visitors

import org.zstack.zql.ast.ASTNode

class ExprVisitor implements ASTVisitor<String, ASTNode.Expr> {
    @Override
    String visit(ASTNode.Expr node) {
        return null
    }
}
