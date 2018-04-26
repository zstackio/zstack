package org.zstack.zql.ast.visitors

import org.zstack.header.zql.ASTNode
import org.zstack.header.zql.ASTVisitor

class RestrictByVisitor implements ASTVisitor<String, ASTNode.RestrictBy> {
    @Override
    String visit(ASTNode.RestrictBy node) {
        List<String> conds = node.exprs.findResults { it.accept(new RestrictExprVisitor()) }
        return conds.isEmpty() ? null : "(${conds.join(" AND ")})"
    }
}
