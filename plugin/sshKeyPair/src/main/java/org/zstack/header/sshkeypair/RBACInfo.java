package org.zstack.header.sshkeypair;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "ssh-key-pair";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(SshKeyPairVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void roles() {
        contributeNormalApiToOtherRole();
    }
}
