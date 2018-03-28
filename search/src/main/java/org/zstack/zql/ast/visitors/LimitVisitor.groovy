package org.zstack.zql.ast.visitors

import org.zstack.zql.ast.ASTNode

class LimitVisitor implements ASTVisitor<String, ASTNode.Limit> {
    @Override
    String visit(ASTNode.Limit node) {
        return "LIMIT ${node.limit}"
    }
}
