package org.zstack.header.tpm;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.rest.SDKPackage;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.vm.VmInstanceVO;

@SDKPackage(packageName="org.zstack.sdk.tpm")
public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "tpm";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        resourceEnsembleContributorBuilder()
                .resource(TpmVO.class)
                .contributeTo(VmInstanceVO.class)
                .build();
    }

    @Override
    public void roles() {
        roleContributorBuilder()
                .actionsInThisPermission()
                .toOtherRole()
                .build();
    }
}
