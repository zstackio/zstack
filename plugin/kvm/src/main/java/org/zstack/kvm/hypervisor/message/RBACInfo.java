package org.zstack.kvm.hypervisor.message;

import org.zstack.header.host.HostVO;
import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .normalAPIs(APIQueryHostOsCategoryMsg.class, APIQueryKvmHypervisorInfoMsg.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("other")
                .actions(APIQueryHostOsCategoryMsg.class, APIQueryKvmHypervisorInfoMsg.class)
                .build();
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {
        globalReadableResourceBuilder()
                .resources(HostVO.class)
                .build();
    }
}
