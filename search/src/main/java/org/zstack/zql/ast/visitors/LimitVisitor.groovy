package org.zstack.zql.ast.visitors

import org.zstack.zql.ast.ASTNode

class LimitVisitor implements ASTVisitor<String, ASTNode.Limit> {
    Integer limit

    @Override
    String visit(ASTNode.Limit node) {
        limit = node.limit
        return "LIMIT ${node.limit}"
    }
}
