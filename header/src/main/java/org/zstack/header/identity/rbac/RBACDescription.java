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

    default RBAC.AttributeSupportResourceBuilder attributeSupportResourceBuilder() {
        return new RBAC.AttributeSupportResourceBuilder();
    }

    /**
     * If you want to contribute a resource to a resource ensemble, you can use this method:
     *
     * Ex: (Make TpmVO as a child resource of VmInstanceVO)
     * <blockquote><pre>
     * resourceEnsembleContributorBuilder()
     *     .resource(TpmVO.class)
     *     .contributeTo(VmInstanceVO.class)
     *     .build();
     * </pre></blockquote>
     *
     * You must set @EntityGraph.Neighbour on VmInstanceVO.class
     * <blockquote><pre>
     * \@EntityGraph(
     *         friends = {
     *                 \@EntityGraph.Neighbour(type = TpmVO.class, myField = "uuid", targetField = "vmInstanceUuid"),
     *         }
     * )
     * </pre></blockquote>
     *
     * or use {@link org.zstack.header.identity.rbac.RBAC.ResourceEnsembleContributorBuilder#resourceWithCustomizeFindingMethods(java.lang.Class, java.util.function.Consumer, java.util.function.Consumer)}
     * to specify how to find the resource by SQL.
     */
    default RBAC.ResourceEnsembleContributorBuilder resourceEnsembleContributorBuilder() {
        return new RBAC.ResourceEnsembleContributorBuilder();
    }

    String permissionName();

    void permissions();

    default void contributeToRoles() {}

    default void roles() {}

    default void globalReadableResources() {}
}
