package org.zstack.zql.ast.visitors

import org.zstack.zql.ast.ASTNode

class ConditionVisitor implements ASTVisitor<String, ASTNode> {
    @Override
    String visit(ASTNode node) {
        assert node instanceof ASTNode.Condition

        return null
    }
}
