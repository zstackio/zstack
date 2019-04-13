package org.zstack.resourceconfig;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.vo.ResourceVO;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("resource-config")
                .normalAPIs("org.zstack.resourceconfig.**")
                .targetResources(ResourceVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("other")
                .actionsByPermissionName("resource-config")
                .build();
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {

    }
}
