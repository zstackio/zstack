package org.zstack.identity;

import org.zstack.core.db.EntityMetadata;
import org.zstack.header.identity.ExternalTenantContext;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.MarshalZQLASTTreeExtensionPoint;
import org.zstack.header.zql.RestrictByExprExtensionPoint;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ast.ZQLMetadata;

import java.util.regex.Pattern;

/**
 * ZQL extension: automatically inject resource filter conditions when request carries external tenant context.
 *
 * Working principle (same two-phase mode as IdentityZQLExtension):
 * 1. marshalZQLASTTree() -- Insert a placeholder RestrictExpr in the AST tree
 * 2. restrictByExpr()    -- Expand placeholder to actual SQL subquery
 *
 * Filter SQL looks like:
 *   entity.uuid IN (SELECT ref.resourceUuid FROM ExternalTenantResourceRefVO ref
 *                   WHERE ref.source = :source AND ref.tenantId = :tenantId)
 */
public class ExternalTenantZQLExtension implements MarshalZQLASTTreeExtensionPoint, RestrictByExprExtensionPoint {

    private static final String ENTITY_NAME = "__EXTERNAL_TENANT_FILTER__";
    private static final String ENTITY_FIELD = "__EXTERNAL_TENANT_FILTER_FIELD__";
    private static final Pattern SAFE_TENANT_VALUE = Pattern.compile("^[a-zA-Z0-9_-]+$");

    @Override
    public void marshalZQLASTTree(ASTNode.Query node) {
        SessionInventory session = ZQLContext.getAPISession();
        if (session == null || !session.hasExternalTenant()) {
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
            return null;
        }

        SessionInventory session = context.getAPISession();
        if (session == null || !session.hasExternalTenant()) {
            throw new SkipThisRestrictExprException();
        }

        ExternalTenantContext tenantCtx = session.getExternalTenantContext();
        if (tenantCtx == null || tenantCtx.getSource() == null || tenantCtx.getTenantId() == null) {
            throw new SkipThisRestrictExprException();
        }

        // Defense-in-depth: reject values that don't match the safe charset.
        // RestServer already enforces this whitelist, but a future entry point might not.
        if (!SAFE_TENANT_VALUE.matcher(tenantCtx.getSource()).matches()
                || !SAFE_TENANT_VALUE.matcher(tenantCtx.getTenantId()).matches()) {
            throw new SkipThisRestrictExprException();
        }

        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(context.getQueryTargetInventoryName());
        String primaryKey = EntityMetadata.getPrimaryKeyField(src.inventoryAnnotation.mappingVOClass()).getName();
        String inventoryAlias = src.simpleInventoryName();

        // Generate subquery, filter associated resources by source + tenantId (+ userId if present)
        // Add userId filter only when present and valid; invalid userId is
        // silently ignored (falls back to tenant-level isolation) rather than
        // throwing SkipThisRestrictExprException which would remove the entire
        // tenant filter — a security escalation.
        String userId = tenantCtx.getUserId();
        String userFilter = "";
        if (userId != null && !userId.isEmpty() && SAFE_TENANT_VALUE.matcher(userId).matches()) {
            userFilter = String.format(" AND etref.userId = '%s'", escapeSql(userId));
        }

        return String.format(
                "(%s.%s IN (SELECT etref.resourceUuid FROM ExternalTenantResourceRefVO etref" +
                " WHERE etref.source = '%s' AND etref.tenantId = '%s'%s))",
                inventoryAlias,
                primaryKey,
                escapeSql(tenantCtx.getSource()),
                escapeSql(tenantCtx.getTenantId()),
                userFilter
        );
    }

    /**
     * Secondary SQL escape — NOT a general-purpose sanitizer.
     * This method only handles single-quote and backslash escaping, which is sufficient
     * because upstream RestServer enforces a strict charset whitelist ([a-zA-Z0-9_-])
     * on source and tenantId before they reach this point.
     * If a new entry point bypasses RestServer validation, this method alone is NOT
     * sufficient to prevent SQL injection — callers must enforce their own whitelist.
     */
    private static String escapeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''").replace("\\", "\\\\");
    }
}
