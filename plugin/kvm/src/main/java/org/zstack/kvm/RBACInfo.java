package org.zstack.kvm;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.kvm.hypervisor.message.APIQueryHostOsCategoryMsg;
import org.zstack.kvm.hypervisor.message.APIQueryKvmHypervisorInfoMsg;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .normalAPIs(APIQueryHostOsCategoryMsg.class, APIQueryKvmHypervisorInfoMsg.class)
                .adminOnlyAPIs("org.zstack.kvm.**")
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

    }
}
