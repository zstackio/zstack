package org.zstack.zql.ast.visitors;

import org.apache.logging.log4j.util.Strings;
import org.zstack.core.Platform;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ast.ZQLMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OrderByVisitor implements ASTVisitor<String, ASTNode.OrderBy> {
    @Override
    public String visit(ASTNode.OrderBy node) {
        String inventoryName = ZQLContext.peekQueryTargetInventoryName();
        ZQLMetadata.InventoryMetadata m = ZQLMetadata.getInventoryMetadataByName(inventoryName);
        for (ASTNode.OrderByExpr orderByExpr : node.getExprs()) {
            if (!hasInventoryField(orderByExpr, m)) {
                throw new ZQLError(Platform.i18n("invalid order by clause, inventory[%s] doesn't have field[%s]", m.simpleInventoryName(), orderByExpr.getField()));
            }
        }

        List<String> conds = node.getExprs().stream()
                .map(it -> (String) it.accept(new OrderByExprVistor()))
                .collect(Collectors.toList());

        return String.format("ORDER BY %s", Strings.join(conds, ','));
    }

    protected boolean hasInventoryField(ASTNode.OrderByExpr node, ZQLMetadata.InventoryMetadata m) {
        return m.hasInventoryField(node.getField());
    }
}
