package org.zstack.zql.ast.visitors

import org.zstack.zql.ast.ASTNode

class OffsetVisitor implements ASTVisitor<String, ASTNode.Offset> {
    @Override
    String visit(ASTNode.Offset node) {
        return "OFFSET ${node.offset}"
    }
}
