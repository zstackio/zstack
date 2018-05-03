package org.zstack.tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.EntityMetadata;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.UserTagVO;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.RestrictByExprExtensionPoint;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.identity.AccountManager;
import org.zstack.zql.ast.ZQLMetadata;

public class TagZQLExtension implements RestrictByExprExtensionPoint {
    public static final String USER_TAG_NAME = "__userTag__";
    public static final String SYS_TAG_NAME = "__systemTag__";

    @Autowired
    private AccountManager acntMgr;

    @Override
    public String restrictByExpr(ZQLExtensionContext context, ASTNode.RestrictExpr expr) {
        if (expr.getEntity() != null) {
            return null;
        }

        if (!expr.getField().equals(USER_TAG_NAME) && !expr.getField().equals(SYS_TAG_NAME)) {
            return null;
        }

        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(context.getQueryTargetInventoryName());
        Class resourceType = acntMgr.getBaseResourceType(src.inventoryAnnotation.mappingVOClass());
        if (resourceType == null) {
            resourceType = src.inventoryAnnotation.mappingVOClass();
        }

        String primaryKey = EntityMetadata.getPrimaryKeyField(src.inventoryAnnotation.mappingVOClass()).getName();
        String tableName = expr.getField().equals(USER_TAG_NAME) ? UserTagVO.class.getSimpleName() : SystemTagVO.class.getSimpleName();

        if (expr.getValue() == null) {
            return String.format("(%s.%s IN (SELECT tagvo.resourceUuid FROM %s tagvo WHERE tagvo.resourceType = '%s' AND tagvo.tag %s))",
                    src.simpleInventoryName(), primaryKey, tableName, resourceType.getSimpleName(), expr.getOperator());
        } else {
            return String.format("(%s.%s IN (SELECT tagvo.resourceUuid FROM %s tagvo WHERE tagvo.resourceType = '%s' AND tagvo.tag %s %s))",
                    src.simpleInventoryName(), primaryKey, tableName, resourceType.getSimpleName(), expr.getOperator(), ((ASTNode.PlainValue)expr.getValue()).getText());
        }
    }
}
