package org.zstack.zql1.ast.visitors

import org.zstack.header.zql.ASTNode
import org.zstack.header.zql.ASTVisitor

class OffsetVisitor implements ASTVisitor<String, ASTNode.Offset> {
    Integer offset

    @Override
    String visit(ASTNode.Offset node) {
        offset = node.offset
        return "OFFSET ${node.offset}"
    }
}
