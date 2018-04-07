package org.zstack.zql.ast.visitors

import org.zstack.zql.ast.ASTNode

class OffsetVisitor implements ASTVisitor<String, ASTNode.Offset> {
    Integer offset

    @Override
    String visit(ASTNode.Offset node) {
        offset = node.offset
        return "OFFSET ${node.offset}"
    }
}
