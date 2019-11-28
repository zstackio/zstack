package org.zstack.identity.rbac;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.EntityMetadata;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.PolicyInventory;
import org.zstack.header.identity.PolicyStatement;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.MarshalZQLASTTreeExtensionPoint;
import org.zstack.header.zql.RestrictByExprExtensionPoint;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.identity.AccountManager;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ast.ZQLMetadata;

import java.util.*;
import java.util.stream.Collectors;

public class RBACZQLExtension implements MarshalZQLASTTreeExtensionPoint, RestrictByExprExtensionPoint {
    private static final String ENTITY_NAME = "__RESOURCE_FILTER__";
    private static final String ENTITY_FIELD = "__RESOURCE_FILTER_FIELD__";

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

    private Map<String, Set<String>> getResourceUuidFilterInPolicy(Map<PolicyInventory, List<PolicyStatement>> policies) {
        Map<String, Set<String>> resourceUuidInPolicy = new HashMap<>();
        for (Map.Entry<PolicyInventory, List<PolicyStatement>> e : policies.entrySet()) {
            for (PolicyStatement statement : e.getValue()) {
                if (statement.getTargetResources() == null) {
                    continue;
                }

                for (String filter : statement.getTargetResources()) {
                    String[] ss = filter.split(":", 2);
                    String resourceName = ss[0];
                    Set<String> allowedResourceUuids = new HashSet<>();

                    if (ss.length > 1) {
                        allowedResourceUuids.addAll(Arrays.asList(ss[1].split(",")));
                    }

                    resourceUuidInPolicy.putIfAbsent(resourceName, new HashSet<>());
                    resourceUuidInPolicy.get(resourceName).addAll(allowedResourceUuids);
                }
            }
        }

        return resourceUuidInPolicy;
    }

    protected List<PolicyInventory> getPolicies(SessionInventory session) {
        return RBACManager.getPoliciesBySession(session);
    }

    @Override
    public String restrictByExpr(ZQLExtensionContext context, ASTNode.RestrictExpr expr) {
        if (!ENTITY_NAME.equals(expr.getEntity()) || !ENTITY_FIELD.equals(expr.getField())) {
            // not for us
            return null;
        }

        if (AccountConstant.isAdmin(context.getAPISession())) {
            throw new RestrictByExprExtensionPoint.SkipThisRestrictExprException();
        }

        List<PolicyInventory> policies = getPolicies(context.getAPISession());
        Map<PolicyInventory, List<PolicyStatement>> denyStatements = RBACManager.collectDenyStatements(policies);
        Map<PolicyInventory, List<PolicyStatement>> allowStatements = RBACManager.collectAllowedStatements(policies);

        Map<String, Set<String>> deniedResourceUuidFilter = getResourceUuidFilterInPolicy(denyStatements);
        Map<String, Set<String>> allowedResourceUuidFilter = getResourceUuidFilterInPolicy(allowStatements);

        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(context.getQueryTargetInventoryName());

        Set<String> deniedUuids = deniedResourceUuidFilter.get(src.inventoryAnnotation.mappingVOClass().getSimpleName());
        Set<String> allowedUuids = allowedResourceUuidFilter.get(src.inventoryAnnotation.mappingVOClass().getSimpleName());
        if ((deniedUuids == null || deniedUuids.isEmpty()) && (allowedUuids == null || allowedUuids.isEmpty())) {
            throw new RestrictByExprExtensionPoint.SkipThisRestrictExprException();
        }

        String primaryKey = EntityMetadata.getPrimaryKeyField(src.inventoryAnnotation.mappingVOClass()).getName();

        String restrictStr = "";
        if (allowedUuids != null && !allowedUuids.isEmpty()) {
            restrictStr += restrictStr + String.format("(%s.%s IN (%s))", src.simpleInventoryName(), primaryKey, allowedUuids.stream()
                    .map(uuid -> String.format("'%s'", uuid))
                    .collect(Collectors.joining(",")));
        }

        if (deniedUuids != null && !deniedUuids.isEmpty()) {
            if (!StringUtils.isEmpty(restrictStr)) {
                restrictStr += " AND ";
            }

            restrictStr += restrictStr + String.format("(%s.%s NOT IN (%s))", src.simpleInventoryName(), primaryKey, deniedUuids.stream()
                    .map(uuid -> String.format("'%s'", uuid))
                    .collect(Collectors.joining(",")));
        }

        return restrictStr;
    }
}
