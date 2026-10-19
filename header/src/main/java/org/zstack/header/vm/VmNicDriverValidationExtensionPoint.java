package org.zstack.header.vm;

/** Additional restrictions imposed by the hypervisor's effective NIC backend. */
public interface VmNicDriverValidationExtensionPoint {
    void validateNicDriver(VmInstanceInventory vm, VmNicInventory nic);
}
