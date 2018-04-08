package org.zstack.zql.ast.visitors

import org.zstack.header.zql.ASTNode
import org.zstack.header.zql.ASTVisitor

class ComplexValueVisitor implements ASTVisitor<String, ASTNode.ComplexValue> {
    @Override
    String visit(ASTNode.ComplexValue node) {
        return node.subQuery.accept(new SubQueryVisitor())
    }
}
