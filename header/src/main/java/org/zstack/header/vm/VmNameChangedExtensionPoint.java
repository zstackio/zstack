package org.zstack.header.vm;

public interface VmNameChangedExtensionPoint {
    void vmNameChanged(VmInstanceInventory vm, String oldName, String newName);
}
