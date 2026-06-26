package org.zstack.zql.ast.visitors.plugin;

import org.zstack.header.zql.ASTNode;

import java.util.List;

import static org.zstack.zql.ast.visitors.constants.MySqlKeyword.isDistinct;

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
        String queryTarget;
        List<String> fieldNames = super.targetFields();

        if (getClauseType() == ClauseType.COUNT || isDistinct(node.getTarget().getFunction())) {
            // TODO: Compatibility changes: hql count do not support multiple fields, even if distinct modified.
            queryTarget = fieldNames.isEmpty() ? entityAlias : String.format("%s.%s", inventory.simpleInventoryName(), fieldNames.get(0));
            return String.format("count(%s)", String.format(functions(), queryTarget));
        } else {
            queryTarget = entityAlias;
            return String.format("count(%s)", queryTarget);
        }
    }
}
