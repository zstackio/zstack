package org.zstack.header.vm;

import org.zstack.header.core.Completion;

import java.util.List;

/**
 * Extension point called after VmNicVO is persisted and non-SDN NIC IPs have
 * been allocated (i.e. after VmAllocateNicIpFlow), but before the VM is
 * instantiated on the hypervisor.
 *
 * Implementations should:
 * 1. Filter NICs that belong to SDN-managed L2 networks (via VSwitchType).
 * 2. Send REST API calls to the SDN controller to create ports (e.g.
 *    OVN logical switch ports, ZNS segment ports).
 * 3. For controllers that return IP addresses (e.g. ZNS), write the
 *    returned IPs back into UsedIpVO and VmNicVO.
 *
 * If the implementation fails, VmAllocateSdnNicFlow will trigger a rollback
 * via {@link #rollbackSdnNic}.
 */
public interface AfterAllocateSdnNicExtensionPoint {
    /**
     * Create SDN ports for the given NICs.
     *
     * @param spec the VM instance spec
     * @param nics all NICs from spec.getDestNics() — implementation filters SDN NICs internally
     * @param completion success/fail callback
     */
    void afterAllocateSdnNic(VmInstanceSpec spec, List<VmNicInventory> nics, Completion completion);

    /**
     * Rollback: remove SDN ports and clean up any IPs allocated by the SDN controller.
     *
     * @param spec the VM instance spec
     * @param nics all NICs from spec.getDestNics()
     * @param completion success/fail callback (best-effort — failures should be logged but not block rollback)
     */
    void rollbackSdnNic(VmInstanceSpec spec, List<VmNicInventory> nics, Completion completion);

    /**
     * Release SDN ports for NICs being detached or destroyed.
     * Used by VmDetachNicFlow and VmReturnReleaseNicFlow.
     *
     * @param nics NICs to release (implementation filters SDN NICs internally)
     * @param completion success/fail callback (best-effort)
     */
    void releaseSdnNics(List<VmNicInventory> nics, Completion completion);
}
