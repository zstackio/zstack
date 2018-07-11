package org.zstack.zql.ast.visitors.plugin;

import org.zstack.header.zql.ASTNode;

public class CountPlugin extends AbstractQueryVisitorPlugin {
    public CountPlugin() {
    }

    public CountPlugin(ASTNode.Query node) {
        super(node);
    }

    @Override
    public ClauseType getClauseType() {
        return ClauseType.COUNT;
    }

    @Override
    public String selectTarget() {
        return "count(*)";
    }
}
