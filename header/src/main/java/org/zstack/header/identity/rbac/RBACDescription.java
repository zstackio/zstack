package org.zstack.header.identity.rbac;

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

    default RBACEntityFormatter entityFormatter() {
        return null;
    }

    String permissionName();

    void permissions();

    default void contributeToRoles() {}

    default void roles() {}

    default void globalReadableResources() {}
}
