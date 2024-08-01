package org.zstack.sshkeypair;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.sshkeypair.SshKeyPairVO;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("ssh-key-pair")
                .normalAPIs(
                        "org.zstack.sshkeypair.**",
                        "org.zstack.header.sshkeypair.**")
                .targetResources(SshKeyPairVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("2ae2f3bdb0ff4296bda2447aa7b334e7")
                .name("ssh-key-pair")
                .permissionsByName("ssh-key-pair")
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
