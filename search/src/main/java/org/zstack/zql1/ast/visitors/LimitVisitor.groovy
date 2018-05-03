package org.zstack.zql1.ast.visitors

import org.zstack.header.zql.ASTNode
import org.zstack.header.zql.ASTVisitor

class LimitVisitor implements ASTVisitor<String, ASTNode.Limit> {
    Integer limit

    @Override
    String visit(ASTNode.Limit node) {
        limit = node.limit
        return "LIMIT ${node.limit}"
    }
}
