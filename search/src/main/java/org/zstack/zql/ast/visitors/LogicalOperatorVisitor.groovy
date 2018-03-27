package org.zstack.zql.ast.visitors

import org.zstack.zql.ast.ASTNode

class LogicalOperatorVisitor implements ASTVisitor<String, ASTNode.LogicalOperator> {
    @Override
    String visit(ASTNode.LogicalOperator node) {
        String left = (node.left as ASTNode).accept(new ConditionVisitor())
        String right = (node.right as ASTNode).accept(new ConditionVisitor())
        return "(${left} ${node.operator.toUpperCase()} ${right})"
    }
}
