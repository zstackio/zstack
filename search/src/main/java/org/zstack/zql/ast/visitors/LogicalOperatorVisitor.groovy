package org.zstack.zql.ast.visitors

import org.zstack.zql.ast.ASTNode

class LogicalOperatorVisitor implements ASTVisitor<String, ASTNode.LogicalOperator> {
    @Override
    String visit(ASTNode.LogicalOperator node) {
        return null
    }
}
