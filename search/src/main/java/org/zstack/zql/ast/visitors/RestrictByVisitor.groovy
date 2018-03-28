package org.zstack.zql.ast.visitors

import org.zstack.zql.ast.ASTNode

class RestrictByVisitor implements ASTVisitor<String, ASTNode.RestrictBy> {
    @Override
    String visit(ASTNode.RestrictBy node) {
        List<String> conds = node.exprs.collect { it.accept(new RestrictExprVisitor()) }
        return "(${conds.join(" AND ")})"
    }
}
