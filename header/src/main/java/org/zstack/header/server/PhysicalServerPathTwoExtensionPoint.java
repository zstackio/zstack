package org.zstack.header.server;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.host.AddHostMessage;
import org.zstack.header.host.HostVO;

/**
 * Extension seam invoked by {@code HostManagerImpl.doAddHost} immediately after the AddHost
 * {@link FlowChain} is built but before any other flow is appended. Implementations contribute
 * the path-2 {@code AutoAssociateFlow → CreatePhysicalServerRoleFlow → InitPhysicalServerCapacityFlow}
 * trio (plus a post-commit {@code enqueueDiscovery} step) so the legacy AddHost path converges
 * on the same internal flow as path 1 (Attach API).
 *
 * <p>The contract intentionally lives in {@code header/} so {@code compute/} can call the
 * extension without depending on {@code plugin/physicalServer/}. The implementing bean
 * autowires the concrete {@code Flow} beans declared in the physicalServer module and
 * appends them via {@link FlowChain#then}.</p>
 *
 * <p>Implementations must be no-ops when the message and cluster do not opt into path 2
 * (e.g., hypervisor types that have not been integrated with PhysicalServer yet).</p>
 *
 * <p>Phase 3 fix-plan U1a / U1b — closes AC-RS-04 / AC-RS-07 root cause via shared seam.</p>
 *
 * @see AddHostMessage#getServerUuid()
 * @see CreateRoleEntityContext
 */
public interface PhysicalServerPathTwoExtensionPoint {
    /**
     * Append path-2 flow steps to the supplied {@link FlowChain}. Called once per AddHost
     * invocation, before any other {@code chain.then(...)} call. The implementation owns
     * gating logic — it should inspect {@code msg} / {@code cluster} and return without
     * mutating the chain if path 2 does not apply.
     *
     * @param chain   the live FlowChain being built (mutable, not yet started)
     * @param msg     the AddHost message (may be API or internal subtype; implementation
     *                may downcast to read role-specific fields like {@code serverUuid})
     * @param hvo     the host entity already persisted by {@code factory.createHost(...)};
     *                its {@code uuid} is the role-side entity UUID per ADR-012
     * @param cluster the target cluster; used to resolve {@code zoneUuid} / hypervisor type
     */
    void contributeAddHostFlows(FlowChain chain, AddHostMessage msg, HostVO hvo, ClusterVO cluster);
}
