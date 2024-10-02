package org.zstack.header.identity.rbac;

import org.zstack.header.message.APIMessage;

import java.util.List;
import java.util.function.Function;

public interface RBACDescription {
    default RBAC.PermissionBuilder permissionBuilder() {
        return new RBAC.PermissionBuilder(this);
    }

    default <MSG extends APIMessage> void expandedPermission(Class<MSG> apiClass,
                                                             Function<MSG, List<APIMessage>> function) {
        new RBAC.ExpendedPermission<>(apiClass)
                .expandTo(function)
                .build();
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

    String permissionName();

    void permissions();

    default void contributeToRoles() {}

    default void roles() {}

    default void globalReadableResources() {}
}
