package org.zstack.directory;

import org.zstack.header.identity.rbac.RBACDescription;

/**
 * @author shenjin
 * @date 2022/12/7 11:27
 */
public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "vm-directory";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyForAll()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }
}
