package org.zstack.zql.ast.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;

public class OffsetVisitor implements ASTVisitor<String, ASTNode.Offset> {
    public Integer offset;

    @Override
    public String visit(ASTNode.Offset node) {
        offset = node.getOffset();
        return String.format("OFFSET %s", offset);
    }
}
