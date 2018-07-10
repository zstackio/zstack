package org.zstack.zql.ast.visitors.plugin;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.zql.ASTNode;
import org.zstack.zql.ast.ZQLMetadata;
import org.zstack.zql.ast.visitors.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractQueryVisitorPlugin extends QueryVisitorPlugin {
    protected ZQLMetadata.InventoryMetadata inventory;
    protected String entityAlias;
    protected String entityVOName;

    public AbstractQueryVisitorPlugin() {
    }

    public AbstractQueryVisitorPlugin(ASTNode.Query node) {
        super(node);
        inventory = ZQLMetadata.findInventoryMetadata(node.getTarget().getEntity());
        entityAlias = inventory.simpleInventoryName();
        entityVOName = inventory.inventoryAnnotation.mappingVOClass().getSimpleName();
    }

    @Override
    public List<String> targetFields() {
        ZQLMetadata.InventoryMetadata inventory = ZQLMetadata.findInventoryMetadata(node.getTarget().getEntity());
        List<String> fieldNames = node.getTarget().getFields() == null ? new ArrayList<>() : node.getTarget().getFields();
        fieldNames.forEach(inventory::errorIfNoField);
        return fieldNames;
    }

    @Override
    public String tableName() {
        return String.format("%s %s", entityVOName, entityAlias);
    }

    @Override
    public String conditions() {
        if (node.getConditions() == null || node.getConditions().isEmpty()) {
            return "";
        }

        List<String> conds = node.getConditions().stream().map(it->(String)((ASTNode)it).accept(new ConditionVisitor())).collect(Collectors.toList());
        return StringUtils.join(conds, " ");
    }

    @Override
    public String restrictBy() {
        return node.getRestrictBy() == null ? null : (String) node.getRestrictBy().accept(new RestrictByVisitor());
    }

    @Override
    public String orderBy() {
        return node.getOrderBy() == null ? null : (String) node.getOrderBy().accept(new OrderByVisitor());
    }

    @Override
    public Integer limit() {
        return node.getLimit() == null ? null : (Integer) node.getLimit().accept(new LimitVisitor());
    }

    @Override
    public Integer offset() {
        return node.getOffset() == null ? null : (Integer) node.getOffset().accept(new OffsetVisitor());
    }
}
