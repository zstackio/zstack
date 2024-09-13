package org.zstack.kvm;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.kvm.hypervisor.message.APIQueryHostOsCategoryMsg;
import org.zstack.kvm.hypervisor.message.APIQueryKvmHypervisorInfoMsg;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "kvm-host";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .normalAPIs(APIQueryHostOsCategoryMsg.class, APIQueryKvmHypervisorInfoMsg.class)
                .adminOnlyForAll()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void contributeToRoles() {
        contributeNormalApiToOtherRole();
    }
}
