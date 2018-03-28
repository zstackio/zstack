package org.zstack.zql.ast.visitors

import org.zstack.zql.ast.ASTNode

class ComplexValueVisitor implements ASTVisitor<String, ASTNode.ComplexValue> {
    @Override
    String visit(ASTNode.ComplexValue node) {
        return node.subQuery.accept(new SubQueryVisitor())
    }
}
