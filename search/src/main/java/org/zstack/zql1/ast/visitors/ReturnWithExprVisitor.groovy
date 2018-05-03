package org.zstack.zql1.ast.visitors

import org.zstack.header.zql.ASTNode
import org.zstack.header.zql.ASTVisitor
import org.zstack.zql1.ast.visitors.result.ReturnWithResult

class ReturnWithExprVisitor implements ASTVisitor<ReturnWithResult, ASTNode> {
    @Override
    ReturnWithResult visit(ASTNode node) {
        if (node instanceof ASTNode.ReturnWithIDExpr) {
            return new ReturnWithResult(name: node.names.join("."))
        } else if (node instanceof ASTNode.ReturnWithBlockExpr) {
            return new ReturnWithResult(name: node.name, expr: node.content)
        } else {
            assert false: "should not be here ${node}"
        }
    }
}
