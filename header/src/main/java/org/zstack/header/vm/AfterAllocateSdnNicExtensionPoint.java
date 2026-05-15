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

    /**
     * Release IP only for NICs whose VM is being destroyed but NIC VO is retained (Recover policy).
     * The segment port itself is kept; only the IP binding is released so the port can be
     * re-assigned an IP when the VM starts again after recovery.
     *
     * Default is a no-op; only SDN controllers that manage IP allocation (e.g. ZNS) override this.
     *
     * @param nics NICs to release IP from (implementation filters SDN NICs internally)
     * @param completion success/fail callback (best-effort)
     */
    default void releaseNicIps(List<VmNicInventory> nics, Completion completion) {
        completion.success();
    }
}
