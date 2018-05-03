package org.zstack.zql1.ast.visitors

import org.zstack.header.zql.ASTNode
import org.zstack.header.zql.ASTVisitor

class LogicalOperatorVisitor implements ASTVisitor<String, ASTNode.LogicalOperator> {
    @Override
    String visit(ASTNode.LogicalOperator node) {
        String left = (node.left as ASTNode).accept(new ConditionVisitor())
        String right = (node.right as ASTNode).accept(new ConditionVisitor())
        return "(${left} ${node.operator.toUpperCase()} ${right})"
    }
}
