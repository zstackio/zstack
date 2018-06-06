package org.zstack.zql.ast.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;

public class LimitVisitor implements ASTVisitor<String, ASTNode.Limit> {
    public Integer limit;

    @Override
    public String visit(ASTNode.Limit node) {
        limit = node.getLimit();
        return String.format("LIMIT %s", node.getLimit());
    }
}
