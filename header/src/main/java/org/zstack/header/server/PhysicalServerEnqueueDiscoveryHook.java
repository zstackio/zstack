package org.zstack.header.server;

/**
 * Post-commit hook fired after a PhysicalServer has been bound to a role:
 * <ul>
 *   <li>path 1 — {@code APIAttachPhysicalServerRoleMsg} handler after attach commits</li>
 *   <li>path 2 — KVM / BM2 legacy {@code Add*Msg} FlowChain after the
 *       {@code AutoAssociateFlow → CreatePhysicalServerRoleFlow → InitPhysicalServerCapacityFlow}
 *       chain commits</li>
 *   <li>path 3 — {@code ContainerEndpointBase.processNodeTransactional} after the
 *       single {@code @Transactional} method commits</li>
 * </ul>
 *
 * <p>Implementations enqueue an asynchronous hardware-discovery request so the PS row's
 * hardware detail tables fill in without blocking the caller. The reference implementation
 * lives in {@code plugin/physicalServer} and delegates to {@code HardwareDiscoveryScheduler}
 * (NB-4 limited concurrency / timeout / retry).</p>
 *
 * <p>Phase 3 fix-plan U1-lead: introduces the SPI seam so role modules can fire discovery
 * without statically depending on the scheduler bean. Wave 3 U16 will wire the three
 * private discover() implementations to actually persist hardware info.</p>
 *
 * @see PhysicalServerRoleProvider
 * @see CreateRoleEntityContext
 */
public interface PhysicalServerEnqueueDiscoveryHook {
    /**
     * Enqueue an asynchronous hardware-discovery request. Returns immediately; failure
     * to enqueue is logged but does not propagate to the caller (best-effort post-commit).
     */
    void enqueueDiscovery(String serverUuid);
}
