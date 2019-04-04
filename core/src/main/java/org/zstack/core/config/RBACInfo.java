package org.zstack.core.config;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.vo.ResourceVO;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("global-config")
                .adminOnlyAPIs("org.zstack.core.config.**")
                .normalAPIs(APIQueryGlobalConfigMsg.class)
                .normalAPIs("org.zstack.core.config.resourceconfig.**")
                .targetResources(ResourceVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("other")
                .actions(APIQueryGlobalConfigMsg.class)
                .build();
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {

    }
}
