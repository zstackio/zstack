package org.zstack.zql.ast.visitors.plugin;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.zql.ASTNode;
import org.zstack.zql.ast.ZQLMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CountPlugin extends SimpleCountPlugin {
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
        if (node.getGroupBy() == null) {
            return super.selectTarget();
        } else {
            String queryTarget;
            List<String> fieldNames = targetFields();

            List<String> qt = fieldNames.stream().map(f->String.format("%s.%s", inventory.simpleInventoryName(), f)).collect(Collectors.toList());
            qt.add(super.selectTarget());
            queryTarget = StringUtils.join(qt, ",");

            return queryTarget;
        }
    }

    @Override
    public List<String> targetFields() {
        ZQLMetadata.InventoryMetadata inventory = ZQLMetadata.findInventoryMetadata(node.getTarget().getEntity());
        List<String> fieldNames = node.getGroupBy() == null ? new ArrayList<>() : node.getGroupBy().getFields();
        fieldNames.forEach(inventory::errorIfNoField);
        return fieldNames;
    }
}
