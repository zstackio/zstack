package org.zstack.header.identity.rbac;

import org.zstack.header.message.APIMessage;

import java.util.ArrayList;
import java.util.List;

public interface RBACDescription {
    default RBAC.PermissionBuilder permissionBuilder() {
        return new RBAC.PermissionBuilder(this);
    }

    default RBAC.ExpendedFieldPermissionBuilder expandedpermissionBuilder() {
        return new RBAC.ExpendedFieldPermissionBuilder();
    }

    default RBAC.RoleContributorBuilder roleContributorBuilder() {
        return new RBAC.RoleContributorBuilder(this);
    }

    default void contributeNormalApiToOtherRole() {
        roleContributorBuilder().toOtherRole().actionsInThisPermission().build();
    }

    default RBAC.RoleBuilder roleBuilder() {
        return new RBAC.RoleBuilder(this);
    }

    default RBAC.GlobalReadableResourceBuilder globalReadableResourceBuilder() {
        return new RBAC.GlobalReadableResourceBuilder();
    }

    default RBAC.ResourceEnsembleContributorBuilder resourceEnsembleContributorBuilder() {
        return new RBAC.ResourceEnsembleContributorBuilder();
    }

    default void registerAPIPermissionChecker(Class aClz, boolean takeOver, APIPermissionChecker checker) {
        List<Class> clzs = new ArrayList<>();
        if (aClz == null) {
            clzs.addAll(APIMessage.apiMessageClasses);
        } else {
            clzs.add(aClz);
        }

        clzs.forEach(apiClz-> {
            List<RBAC.APIPermissionCheckerWrapper> ws = RBAC.permissionCheckers.computeIfAbsent(apiClz, x->new ArrayList<>());
            RBAC.APIPermissionCheckerWrapper w = new RBAC.APIPermissionCheckerWrapper();
            w.takeOver = takeOver;
            w.checker = checker;
            ws.add(w);
        });
    }

    default void registerAPIPermissionChecker(Class apiClz, APIPermissionChecker checker) {
        registerAPIPermissionChecker(apiClz, false, checker);
    }

    default RBACEntityFormatter entityFormatter() {
        return null;
    }

    String permissionName();

    void permissions();

    default void contributeToRoles() {}

    default void roles() {}

    default void globalReadableResources() {}
}
