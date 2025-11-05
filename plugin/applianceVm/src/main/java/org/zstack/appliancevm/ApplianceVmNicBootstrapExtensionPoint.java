package org.zstack.appliancevm;

import org.zstack.header.vm.VmNicInventory;

public interface ApplianceVmNicBootstrapExtensionPoint {
    void fillNicBootstrapInfo(VmNicInventory nic, ApplianceVmNicTO nicTo);
}
