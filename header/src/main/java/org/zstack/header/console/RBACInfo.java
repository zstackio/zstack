package org.zstack.header.console;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("console")
                .adminOnlyAPIs("org.zstack.header.console.**")
                .normalAPIs(APIRequestConsoleAccessMsg.class)
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .name("console")
                .uuid("6f5a7d6d2da9499da9e4bdb079f65adf")
                .permissionsByName("console")
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
