package org.zstack.header.vm;

public interface AfterUpdateVmNicMacExtensionPoint {
    void afterUpdateVmNicMac(VmNicInventory nic, String oldMac, String newMac);
}
