package org.zstack.zql.ast.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.ast.ZQLMetadata;

import static org.zstack.zql.ast.visitors.plugin.CountPlugin.GROUP_COUNT_TARGET_FILED;

/**
 * Created by MaJin on 2019/4/25.
 */
public class GroupCountOrderByVisitor extends OrderByVisitor {
    @Override
    protected boolean hasInventoryField(ASTNode.OrderByExpr node, ZQLMetadata.InventoryMetadata m) {
        return node.getField().equalsIgnoreCase(GROUP_COUNT_TARGET_FILED) || m.hasInventoryField(node.getField());
    }
}
