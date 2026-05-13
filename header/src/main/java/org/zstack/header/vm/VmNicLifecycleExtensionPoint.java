package org.zstack.header.vm;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;

import java.util.List;

/**
 * Extension point for managing the lifecycle of VM NICs on hosts.
 * Implementations are invoked during VM start, stop, migration, NIC attach/detach,
 * and periodic reconciliation driven by KVM heartbeat.
 *
 * <p>All async methods must invoke their completion callback exactly once,
 * even on error paths. {@link Completion}-based methods may fail the operation;
 * {@link NoErrorCompletion}-based methods must always succeed (log and absorb errors).
 */
public interface VmNicLifecycleExtensionPoint {

    /**
     * Returns true if this extension should manage the given NIC.
     * Called synchronously; must not block or throw checked exceptions.
     *
     * @param nic the NIC to evaluate
     * @return true if this extension handles the NIC
     */
    boolean isApplicable(VmNicInventory nic);

    /**
     * Called when a VM starts or a NIC is attached to a running VM.
     * Failure aborts the VM start / NIC attach operation.
     *
     * @param hostUuid   UUID of the host where the VM is starting
     * @param nics       NICs filtered by {@link #isApplicable}
     * @param completion call {@code success()} or {@code fail()} exactly once
     */
    void setupOnHost(String hostUuid, List<VmNicInventory> nics, Completion completion);

    /**
     * Called when a VM stops or a NIC is detached. Errors are logged but do not
     * block the operation.
     *
     * @param hostUuid   UUID of the host the VM is leaving
     * @param nics       NICs filtered by {@link #isApplicable}
     * @param completion call {@code done()} exactly once
     */
    void cleanupFromHost(String hostUuid, List<VmNicInventory> nics, NoErrorCompletion completion);

    /**
     * Called before live migration starts. Default: setup on destination host.
     * Failure aborts the migration.
     * Execution order: preMigrate → (live migration) → postMigrate or failedMigrate.
     *
     * @param srcHostUuid  UUID of the source host
     * @param destHostUuid UUID of the destination host
     * @param nics         NICs filtered by {@link #isApplicable}
     * @param completion   call {@code success()} or {@code fail()} exactly once
     */
    default void preMigrate(String srcHostUuid, String destHostUuid,
                            List<VmNicInventory> nics, Completion completion) {
        setupOnHost(destHostUuid, nics, completion);
    }

    /**
     * Called after live migration succeeds. Errors are logged but do not block.
     *
     * @param srcHostUuid  UUID of the source host
     * @param destHostUuid UUID of the destination host
     * @param nics         NICs filtered by {@link #isApplicable}
     * @param completion   call {@code done()} exactly once
     */
    default void postMigrate(String srcHostUuid, String destHostUuid,
                             List<VmNicInventory> nics, NoErrorCompletion completion) {
        cleanupFromHost(srcHostUuid, nics, completion);
    }

    /**
     * Called when live migration fails. Errors are logged but do not block.
     *
     * @param srcHostUuid  UUID of the source host
     * @param destHostUuid UUID of the destination host (where partial setup may exist)
     * @param nics         NICs filtered by {@link #isApplicable}
     * @param completion   call {@code done()} exactly once
     */
    default void failedMigrate(String srcHostUuid, String destHostUuid,
                               List<VmNicInventory> nics, NoErrorCompletion completion) {
        cleanupFromHost(destHostUuid, nics, completion);
    }

    /**
     * Called on VM start when the VM's last known host differs from the destination host.
     * Used to clean up state left on the previous host after an ungraceful shutdown.
     * Errors are logged but do not block.
     *
     * @param lastHostUuid UUID of the host where the VM last ran
     * @param nics         NICs filtered by {@link #isApplicable}
     * @param completion   call {@code done()} exactly once
     */
    default void cleanupStaleResource(String lastHostUuid, List<VmNicInventory> nics,
                                      NoErrorCompletion completion) {
        cleanupFromHost(lastHostUuid, nics, completion);
    }

    /**
     * Called periodically on each successful KVM heartbeat to reconcile NIC state.
     * Implementations should ensure remote systems match the expected state in {@code expectedNics}.
     * Errors are logged but do not block the heartbeat.
     *
     * @param hostUuid      UUID of the host being reconciled
     * @param expectedNics  all Running-VM NICs on this host, filtered by {@link #isApplicable}
     * @param completion    call {@code done()} exactly once
     */
    default void reconcileOnHost(String hostUuid, List<VmNicInventory> expectedNics,
                                 NoErrorCompletion completion) {
        completion.done();
    }
}
