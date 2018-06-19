package org.zstack.header;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .normalAPIs(APIIsOpensourceVersionMsg.class.getName())
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .name("other")
                .uuid("80315b1f85314917826b182bf6def552")
                .actions(APIIsOpensourceVersionMsg.class.getName())
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
