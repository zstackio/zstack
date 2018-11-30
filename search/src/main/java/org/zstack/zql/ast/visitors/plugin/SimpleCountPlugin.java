package org.zstack.zql.ast.visitors.plugin;

import org.zstack.header.zql.ASTNode;

public class SimpleCountPlugin extends AbstractQueryVisitorPlugin {
    public SimpleCountPlugin() {
    }

    public SimpleCountPlugin(ASTNode.Query node) {
        super(node);
    }

    @Override
    public ClauseType getClauseType() {
        return ClauseType.SIMPLE_COUNT;
    }

    @Override
    public String selectTarget() {
        return "count(*)";
    }
}
