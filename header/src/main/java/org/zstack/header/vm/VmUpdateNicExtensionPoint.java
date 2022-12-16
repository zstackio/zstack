package org.zstack.header.vm;

public interface VmUpdateNicExtensionPoint {
    void afterUpdateNic(VmNicInventory nic);
}
