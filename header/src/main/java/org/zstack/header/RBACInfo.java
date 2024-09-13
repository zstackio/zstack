package org.zstack.header;

import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "core-open-source";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .name("other")
                .uuid(AccountConstant.OTHER_ROLE_UUID)
                .actions(APIIsOpensourceVersionMsg.class)
                .build();

        roleBuilder()
                .name("legacy")
                .uuid(AccountConstant.LEGACY_ROLE_UUID)
                .actions("org.zstack.header.**")
                .build();
    }
}
