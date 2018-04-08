package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.EntityMetadata;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.MarshalZQLASTTreeExtensionPoint;
import org.zstack.header.zql.RestrictByExprExtensionPoint;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ast.ZQLMetadata;

import java.util.ArrayList;

public class IdentityZQLExtension implements MarshalZQLASTTreeExtensionPoint, RestrictByExprExtensionPoint {
    private static final String ENTITY_NAME = "account";
    private static final String ENTITY_FIELD = "uuid";

    @Autowired
    private AccountManager acntMgr;

    @Override
    public void marshalZQLASTTree(ASTNode.Query node) {
        SessionInventory session = ZQLContext.getAPISession();
        if (session == null) {
            return;
        }

        if (node.getRestrictBy() == null)  {
            node.setRestrictBy(new ASTNode.RestrictBy());
        }

        if (node.getRestrictBy().getExprs() == null) {
            node.getRestrictBy().setExprs(new ArrayList<>());
        }

        ASTNode.RestrictExpr expr = new ASTNode.RestrictExpr();
        expr.setEntity(ENTITY_NAME);
        expr.setField(ENTITY_FIELD);
        expr.setOperator("=");
        ASTNode.PlainValue v = new ASTNode.PlainValue();
        v.setText(session.getAccountUuid());
        v.setType(String.class);
        v.setCtype(String.class.getName());
        expr.setValue(v);

        node.getRestrictBy().getExprs().add(expr);
    }

    @Override
    public String restrictByExpr(ZQLExtensionContext context, RestrictByExpr expr) {
        if (!ENTITY_NAME.equals(expr.entity) || !ENTITY_FIELD.equals(expr.field)) {
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
