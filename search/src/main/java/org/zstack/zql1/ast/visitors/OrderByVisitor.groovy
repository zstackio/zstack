package org.zstack.zql1.ast.visitors

import org.zstack.core.Platform
import org.zstack.header.zql.ASTVisitor
import org.zstack.zql1.ZQLContext
import org.zstack.header.zql.ASTNode
import org.zstack.zql1.ast.ZQLError
import org.zstack.zql1.ast.ZQLMetadata

class OrderByVisitor implements ASTVisitor<String, ASTNode.OrderBy> {
    @Override
    String visit(ASTNode.OrderBy node) {
        if (!node.direction.equalsIgnoreCase("asc") && !node.direction.equalsIgnoreCase("desc")) {
            throw new ZQLError(Platform.i18n("invalid order by clause, expect direction[asc,desc] but got %s", node.direction))
        }

        String inventoryName = ZQLContext.peekQueryTargetInventoryName()
        ZQLMetadata.InventoryMetadata m = ZQLMetadata.getInventoryMetadataByName(inventoryName)
        if (!m.hasInventoryField(node.field)) {
            throw new ZQLError(Platform.i18n("invalid order by clause, inventory[%s] doesn't have field[%s]", m.simpleInventoryName(), node.field))
        }

        return "ORDER BY ${node.field} ${node.direction.toUpperCase()}"
    }
}
