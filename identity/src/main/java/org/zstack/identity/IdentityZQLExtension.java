package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.EntityMetadata;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.MarshalZQLASTTreeExtensionPoint;
import org.zstack.header.zql.RestrictByExprExtensionPoint;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.zql1.ZQLContext;
import org.zstack.zql1.ast.ZQLMetadata;

public class IdentityZQLExtension implements MarshalZQLASTTreeExtensionPoint, RestrictByExprExtensionPoint {
    private static final String ENTITY_NAME = "__ACCOUNT_FILTER__";
    private static final String ENTITY_FIELD = "__ACCOUNT_FILTER_FIELD__";

    @Autowired
    private AccountManager acntMgr;

    @Override
    public void marshalZQLASTTree(ASTNode.Query node) {
        SessionInventory session = ZQLContext.getAPISession();
        if (session == null) {
            return;
        }

        ASTNode.RestrictExpr expr = new ASTNode.RestrictExpr();
        expr.setEntity(ENTITY_NAME);
        expr.setField(ENTITY_FIELD);

        node.addRestrictExpr(expr);
    }

    @Override
    public String restrictByExpr(ZQLExtensionContext context, ASTNode.RestrictExpr expr) {
        if (!ENTITY_NAME.equals(expr.getEntity()) || !ENTITY_FIELD.equals(expr.getField())) {
            // not for us
            return null;
        }

        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(context.getQueryTargetInventoryName());
        String accountUuid = context.getAPISession().getAccountUuid();
        if (AccountConstant.INITIAL_SYSTEM_ADMIN_UUID.equals(accountUuid)) {
            throw new SkipThisRestrictExprException();
        }

        if (!acntMgr.isResourceHavingAccountReference(src.getInventoryAnnotation().mappingVOClass())) {
            throw new SkipThisRestrictExprException();
        }

        String resourceType = acntMgr.getBaseResourceType(src.getInventoryAnnotation().mappingVOClass()).getSimpleName();
        String primaryKey = EntityMetadata.getPrimaryKeyField(src.getInventoryAnnotation().mappingVOClass()).getName();

        return String.format("(%s.%s IN (SELECT accountresourcerefvo.resourceUuid FROM AccountResourceRefVO accountresourcerefvo WHERE" +
                        "  (accountresourcerefvo.ownerAccountUuid = '%s' AND accountresourcerefvo.resourceType = '%s') OR (accountresourcerefvo.resourceUuid" +
                        " IN (SELECT sharedresourcevo.resourceUuid FROM SharedResourceVO sharedresourcevo WHERE" +
                        " (sharedresourcevo.receiverAccountUuid = '%s' OR sharedresourcevo.toPublic = 1) AND sharedresourcevo.resourceType = '%s'))))",
                src.simpleInventoryName(), primaryKey, accountUuid, resourceType, accountUuid, resourceType);
    }
}
