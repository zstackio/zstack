package org.zstack.header.vm;

import org.zstack.header.core.Completion;

/**
 * Extension point called after IP address(es) have been successfully allocated
 * and flushed to the database for VmNics in VmAllocateNicIpFlow.
 *
 * At the time this fires:
 * - VmNicVO rows exist in the database (created by VmAllocateNicFlow)
 * - UsedIpVO rows are committed (allocated by VmAllocateNicIpFlow)
 * - spec.getDestNics() contains up-to-date NIC inventories with IP info
 *
 * If the implementation fails, the flow chain rolls back:
 * VmAllocateNicIpFlow.rollback (returns IPs) → VmAllocateNicFlow.rollback (deletes NICs).
 */
public interface AfterAllocateVmNicIpExtensionPoint {
    void afterAllocateVmNicIp(VmInstanceSpec spec, Completion completion);
}
