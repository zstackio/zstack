package org.zstack.header.vm;

/**
 * Only for UserVM.
 *
 * Appliance VM use {@link ApplianceVmInstanceCreateExtensionPoint}
 */
public interface VmInstanceCreateExtensionPoint {
    void preCreateVmInstance(CreateVmInstanceMsg msg);

    default void afterPersistVmInstanceVO(VmInstanceVO vo, CreateVmInstanceMsg msg) {}
}
