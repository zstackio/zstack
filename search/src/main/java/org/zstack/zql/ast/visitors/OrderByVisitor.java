package org.zstack.zql.ast.visitors;

import org.zstack.core.Platform;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ast.ZQLMetadata;

public class OrderByVisitor implements ASTVisitor<String, ASTNode.OrderBy> {
    @Override
    public String visit(ASTNode.OrderBy node) {
        if (!node.getDirection().equalsIgnoreCase("asc") && !node.getDirection().equalsIgnoreCase("desc")) {
            throw new ZQLError(Platform.i18n("invalid order by clause, expect direction[asc,desc] but got %s", node.getDirection()));
        }

        String inventoryName = ZQLContext.peekQueryTargetInventoryName();
        ZQLMetadata.InventoryMetadata m = ZQLMetadata.getInventoryMetadataByName(inventoryName);
        if (!m.hasInventoryField(node.getField())) {
            throw new ZQLError(Platform.i18n("invalid order by clause, inventory[%s] doesn't have field[%s]", m.simpleInventoryName(), node.getField()));
        }

        return String.format("ORDER BY %s %s", node.getField(), node.getDirection().toUpperCase());
    }
}
