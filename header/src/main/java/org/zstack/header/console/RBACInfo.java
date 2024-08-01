package org.zstack.header.console;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "console";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APIRequestConsoleAccessMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("6f5a7d6d2da9499da9e4bdb079f65adf")
                .permissionBaseOnThis()
                .build();
    }
}
