package org.zstack.header.vm;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.network.l3.L3NetworkInventory;

/**
 * Extension point allowing a network provider to take over VM NIC network change execution.
 */
public interface VmChangeNicNetworkExtensionPoint {
    /**
     * Returns whether this extension takes over changing {@code nic} on {@code vm} to {@code destination}.
     * The caller invokes the first extension that returns {@code true} and skips the default network change flow.
     */
    boolean accepts(VmInstanceInventory vm, VmNicInventory nic, L3NetworkInventory destination);

    /**
     * Executes the NIC network change to {@code destination}, including target network/IP configuration and
     * rollback or failure handling, and reports the updated {@link VmNicInventory} or failure via {@code completion}.
     */
    void changeNetwork(ChangeVmNicNetworkMsg msg, VmInstanceSpec spec, VmNicInventory nic,
                       L3NetworkInventory destination, ReturnValueCompletion<VmNicInventory> completion);
}
