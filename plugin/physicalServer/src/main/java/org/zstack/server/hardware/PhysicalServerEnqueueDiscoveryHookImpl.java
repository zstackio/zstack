package org.zstack.server.hardware;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.server.PhysicalServerEnqueueDiscoveryHook;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Phase 3 fix-plan U1-lead: Spring bean adapter exposing
 * {@link PhysicalServerEnqueueDiscoveryHook} as the post-commit hook implementation. KVM /
 * BM2 / Container modules autowire the SPI interface, keeping a stable seam decoupled from
 * the {@link HardwareDiscoveryScheduler} bean (whose API may change as Wave 3 U16 wires up
 * the three private discover() methods).
 *
 * <p>Best-effort: scheduler enqueue exceptions are logged but never propagate (post-commit
 * hooks must not break the caller's transaction outcome).</p>
 */
public class PhysicalServerEnqueueDiscoveryHookImpl implements PhysicalServerEnqueueDiscoveryHook {
    private static final CLogger logger = Utils.getLogger(PhysicalServerEnqueueDiscoveryHookImpl.class);

    @Autowired
    private HardwareDiscoveryScheduler scheduler;

    @Override
    public void enqueueDiscovery(String serverUuid) {
        if (serverUuid == null || serverUuid.isEmpty()) {
            return;
        }
        try {
            scheduler.enqueueDiscovery(serverUuid);
        } catch (Exception e) {
            // NB-4: scheduler retry already exists internally; we only swallow here so a
            // transient enqueue failure (e.g., executor shutdown during MN restart) cannot
            // poison a freshly-attached role. JVM-fatal Errors propagate.
            logger.warn(String.format(
                    "failed to enqueue hardware discovery for server[uuid:%s]: %s",
                    serverUuid, e.getMessage()));
        }
    }
}
