package org.zstack.network.securitygroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.identity.IdentityZQLExtension;

public class SecurityGroupIdentityQueryExtension extends IdentityZQLExtension {
    @Autowired
    @Qualifier("IdentityZQLExtension")
    private IdentityZQLExtension identityQueryExtension;

    @Override
    public String restrictByExpr(ZQLExtensionContext context, ASTNode.RestrictExpr expr) {
        String inventory = context.getQueryTargetInventoryName();
        if ((!SecurityGroupRuleInventory.class.getName().equals(inventory)
                && !VmNicSecurityGroupRefInventory.class.getName().equals(inventory))
                || !ENTITY_NAME.equals(expr.getEntity()) || !ENTITY_FIELD.equals(expr.getField())) {
            return null;
        }

        String targetInventory = inventory.substring(inventory.lastIndexOf('.') + 1);
        String sgFilter = getParentRestriction(context, expr, SecurityGroupInventory.class, targetInventory + ".securityGroupUuid");
        String nicFilter = VmNicSecurityGroupRefInventory.class.getName().equals(inventory)
                ? getParentRestriction(context, expr, VmNicInventory.class, targetInventory + ".vmNicUuid") : null;
        if (sgFilter == null && nicFilter == null) {
            throw new SkipThisRestrictExprException();
        }
        return sgFilter == null ? nicFilter : nicFilter == null ? sgFilter : String.format("(%s and %s)", sgFilter, nicFilter);
    }

    private String getParentRestriction(ZQLExtensionContext context, ASTNode.RestrictExpr expr, Class<?> inventory, String targetField) {
        try {
            String restriction = identityQueryExtension.restrictByExpr(new ZQLExtensionContext() {
                @Override
                public String getQueryTargetInventoryName() {
                    return inventory.getName();
                }

                @Override
                public SessionInventory getAPISession() {
                    return context.getAPISession();
                }
            }, expr);
            return restriction == null ? null : restriction.replace(inventory.getSimpleName() + ".uuid", targetField);
        } catch (SkipThisRestrictExprException ignored) {
            // Only this parent is unrestricted; the other parent's restriction still applies.
            return null;
        }
    }
}
