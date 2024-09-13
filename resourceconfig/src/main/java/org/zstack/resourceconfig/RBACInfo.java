package org.zstack.resourceconfig;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.vo.ResourceVO;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "resource-config";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(ResourceVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void contributeToRoles() {
        contributeNormalApiToOtherRole();
    }
}
