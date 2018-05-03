package org.zstack.zql1.ast.visitors

import org.zstack.header.zql.ASTNode
import org.zstack.header.zql.ASTVisitor

class ConditionVisitor implements ASTVisitor<String, ASTNode> {
    @Override
    String visit(ASTNode node) {
        assert node instanceof ASTNode.Condition

        if (node instanceof ASTNode.Expr) {
            return node.accept(new ExprVisitor())
        } else if (node instanceof ASTNode.LogicalOperator) {
            return node.accept(new LogicalOperatorVisitor())
        } else {
            assert false : "should not be here"
        }
    }
}
