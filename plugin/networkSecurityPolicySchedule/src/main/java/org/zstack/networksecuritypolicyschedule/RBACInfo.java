package org.zstack.networksecuritypolicyschedule;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.vo.ResourceVO;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("networkSecurityPolicySchedule")
                .normalAPIs("org.zstack.networksecuritypolicyschedule.**")
                .targetResources(ResourceVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
    }

    @Override
    public void roles() {
        roleBuilder()
                .name("networkSecurityPolicySchedule")
                .permissionsByName("networkSecurityPolicySchedule")
                .uuid("db5d42b57c5c43c5a0ea53a1fba4649c")
                .build();
    }

    @Override
    public void globalReadableResources() {
    }
}
