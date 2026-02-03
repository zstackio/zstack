package org.zstack.header.tpm;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.rest.SDKPackage;
import org.zstack.header.tpm.entity.TpmVO;

@SDKPackage(packageName="org.zstack.sdk.tpm")
public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("tpm")
                .normalAPIs("org.zstack.header.tpm.**")
                .targetResources(TpmVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("other")
                .actionsByPermissionName("tpm")
                .build();
    }

    @Override
    public void roles() {
    }

    @Override
    public void globalReadableResources() {
    }
}
