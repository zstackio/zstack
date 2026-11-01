package org.zstack.header.vm;

import org.zstack.header.core.Completion;
import org.zstack.header.network.l3.L3NetworkInventory;

/**
 * Extension point allowing a network provider to take over setting a static IP on a VM NIC.
 */
public interface VmSetStaticIpExtensionPoint {
    /**
     * Returns whether this extension takes over setting a static IP on {@code nic} of {@code vm} in {@code l3}.
     * The caller invokes the first extension that returns {@code true} and skips the default static IP flow.
     */
    boolean accepts(VmInstanceInventory vm, VmNicInventory nic, L3NetworkInventory l3);

    /**
     * Applies and persists the static IP and related DNS configuration for {@code nic} in {@code l3},
     * and reports completion or failure via {@code completion}.
     */
    void setStaticIp(SetVmStaticIpMsg msg, VmInstanceSpec spec, VmNicInventory nic,
                     L3NetworkInventory l3, Completion completion);
}
