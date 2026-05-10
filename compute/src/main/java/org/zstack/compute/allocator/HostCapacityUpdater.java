package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DeadlockAutoRestart;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.server.PhysicalServerCapacityVO;
import org.zstack.header.server.PhysicalServerRoleVO;
import org.zstack.header.server.PhysicalServerRoleVO_;
import org.zstack.header.server.ServerRoleType;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;

/**
 * Created by frank on 11/2/2015.
 *
 * <p>Only the {@code (hostUuid)} constructor is supported. The former
 * {@code (TypedQuery<HostCapacityVO>)} constructor was removed in v5.5.18 (2026-04-20) because it
 * exposed a {@code SELECT ... FOR UPDATE} path over the {@code HostCapacityVO} entity — once that
 * entity becomes a VIEW (capacity PRD §2.1), MariaDB/MySQL rejects row-level locks against
 * non-updatable views.
 *
 * <p>Phase 2 (2026-04-22, U4) internals rewrite per capacity PRD §2.1 W3 / NB-22 / NB-24 / NB-30:
 * <ul>
 *   <li>{@link #lockCapacity()} resolves {@code serverUuid} from {@code hostUuid} via
 *       {@link #resolveServerUuidOrThrow(String)} (NB-24 fail-loud) and locks the
 *       {@code PhysicalServerCapacityVO} truth table with {@link LockModeType#PESSIMISTIC_WRITE}
 *       keyed by {@code serverUuid} (NB-30 single lock key invariant).</li>
 *   <li>Ten authoritative fields are copied from {@code PhysicalServerCapacityVO} into a transient
 *       {@link HostCapacityVO} POJO (NB-22 in-method exception to the "no {@code new HostCapacityVO()}"
 *       invariant; the POJO never escapes this class and is never {@code em.merge}ed).</li>
 *   <li>{@code HostCapacityUpdaterRunnable#call(HostCapacityVO)} interface signature is unchanged —
 *       the 4 call sites (HostAllocatorManagerImpl:247/809, HostCapacityReserveManagerImpl:253/289)
 *       see the POJO and mutate it in place, unaware of the backing table switch.</li>
 *   <li>{@link #merge()} flushes exactly 3 runnable-authored fields
 *       ({@code availableCpu / availableMemory / availablePhysicalMemory}) back to the
 *       {@code PhysicalServerCapacityVO} row. Mutations to {@code totalCpu} etc. on the POJO are
 *       intentionally dropped — ratio-driven {@code totalCpu} is authoritative via
 *       {@code HostCpuOverProvisioningManager} (U5) JPQL updates against the same truth table.</li>
 * </ul>
 *
 * @deprecated Retained for {@code HostCapacityAllocatorFlow} / {@code ReturnHostCapacityMsg} VM
 *             allocator incremental write paths only. New call sites must use
 *             {@link PhysicalServerCapacityUpdater#recalculate(String)} instead (U-B, 2026-05-08).
 */
@Deprecated
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostCapacityUpdater {
    private static final CLogger logger = Utils.getLogger(HostCapacityUpdater.class);

    @Autowired
    private DatabaseFacade dbf;

    private String hostUuid;
    private HostCapacityVO capacityVO;
    private HostCapacityVO originalCopy;
    private PhysicalServerCapacityVO physCapacityVO;

    public HostCapacityUpdater(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    /**
     * Resolve PhysicalServer UUID from a KVM host UUID via PhysicalServerRoleVO mapping.
     *
     * <p>Throws {@link CloudRuntimeException} when no KVM_HOST role mapping is found (NB-24,
     * 2026-04-22). Previous NB-22 "log null + boolean" silent-drop was reverted — fail-loud
     * surfaces FlowChain timing bugs / orphan windows instead of masking them as silent capacity
     * update losses. The existing "host deleted naturally" semantic is still carried by
     * {@link #lockCapacity()} returning {@code false} when the capacity row itself is absent.
     *
     * <p>NB-30: Phase 2 lock key invariant. All PESSIMISTIC_WRITE paths on PhysicalServerCapacityVO
     * use {@code serverUuid} as the single lock key; callers MUST NOT mix {@code hostUuid} and
     * {@code serverUuid}.
     */
    public static String resolveServerUuidOrThrow(String hostUuid) {
        String serverUuid = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.roleUuid, hostUuid)
                .eq(PhysicalServerRoleVO_.roleType, ServerRoleType.KVM_HOST.toString())
                .select(PhysicalServerRoleVO_.serverUuid)
                .findValue();
        if (serverUuid == null) {
            throw new CloudRuntimeException(String.format(
                    "cannot resolve PhysicalServer UUID for host[uuid:%s]: no KVM_HOST "
                            + "PhysicalServerRoleVO found. FlowChain timing bug or orphan "
                            + "PhysicalServerVO — capacity PRD NB-24.", hostUuid));
        }
        return serverUuid;
    }

    private void logDeletedHost() {
        logger.warn(String.format("[Host Capacity] unable to update capacity for the host[uuid:%s]. It may have been deleted, cannot find it in database",
                hostUuid));
    }

    private void logCapacityChange() {
        if (logger.isTraceEnabled()) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            int index = 0;
            String fileName = HostCapacityUpdater.class.getSimpleName() + ".java";
            for (int i=0; i<stackTraceElements.length; i++) {
                if (fileName.equals(stackTraceElements[i].getFileName())) {
                    index = i;
                }
            }
            StackTraceElement caller = stackTraceElements[index+1];
            logger.trace(String.format("[Host Capacity] %s:%s:%s changed the capacity of the host[uuid:%s] as:\n" +
                            "total cpu: %s --> %s\n" +
                            "available cpu: %s --> %s\n" +
                            "total memory: %s --> %s\n" +
                            "available memory: %s --> %s\n" +
                            "total physical memory: %s --> %s\n" +
                            "available physical memory: %s --> %s\n",
                    caller.getFileName(), caller.getMethodName(), caller.getLineNumber(), capacityVO.getUuid(),
                    originalCopy.getTotalCpu(), capacityVO.getTotalCpu(),
                    originalCopy.getAvailableCpu(), capacityVO.getAvailableCpu(),
                    originalCopy.getTotalMemory(), capacityVO.getTotalMemory(),
                    originalCopy.getAvailableMemory(), capacityVO.getAvailableMemory(),
                    originalCopy.getTotalPhysicalMemory(), capacityVO.getTotalPhysicalMemory(),
                    originalCopy.getAvailablePhysicalMemory(), capacityVO.getAvailablePhysicalMemory()));
        }
    }

    private boolean lockCapacity() {
        String serverUuid = resolveServerUuidOrThrow(hostUuid);
        physCapacityVO = dbf.getEntityManager()
                .find(PhysicalServerCapacityVO.class, serverUuid, LockModeType.PESSIMISTIC_WRITE);
        if (physCapacityVO == null) {
            return false;
        }

        // NB-22 in-method POJO exception: capacityVO is a transient HostCapacityVO that never
        // escapes this class and is never em.merge()'d. 10 authoritative fields copied
        // physCapacity → HCV POJO; runnable sees stable HostCapacityVO contract.
        capacityVO = new HostCapacityVO();
        capacityVO.setUuid(hostUuid);
        capacityVO.setTotalMemory(physCapacityVO.getTotalMemory());
        capacityVO.setTotalCpu(physCapacityVO.getTotalCpu());
        capacityVO.setCpuNum((int) physCapacityVO.getCpuNum());
        capacityVO.setCpuSockets(physCapacityVO.getCpuSockets());
        capacityVO.setCpuCoreNum(physCapacityVO.getCpuCoreNum());
        capacityVO.setAvailableMemory(physCapacityVO.getAvailableMemory());
        capacityVO.setAvailableCpu(physCapacityVO.getAvailableCpu());
        capacityVO.setTotalPhysicalMemory(physCapacityVO.getTotalPhysicalMemory());
        capacityVO.setAvailablePhysicalMemory(physCapacityVO.getAvailablePhysicalMemory());

        originalCopy = new HostCapacityVO();
        originalCopy.setTotalCpu(capacityVO.getTotalCpu());
        originalCopy.setAvailableCpu(capacityVO.getAvailableCpu());
        originalCopy.setTotalMemory(capacityVO.getTotalMemory());
        originalCopy.setAvailableMemory(capacityVO.getAvailableMemory());
        originalCopy.setTotalPhysicalMemory(capacityVO.getTotalPhysicalMemory());
        originalCopy.setAvailablePhysicalMemory(capacityVO.getAvailablePhysicalMemory());
        return true;
    }

    private void merge() {
        // NB-22 3-field writeback: only runnable-authored fields flush back to PSC truth table.
        // Mutations to totalCpu / totalMemory / totalPhysicalMemory on the POJO are intentionally
        // dropped; ratio-driven totalCpu is authoritative via HostCpuOverProvisioningManager (U5).
        physCapacityVO.setAvailableCpu(capacityVO.getAvailableCpu());
        physCapacityVO.setAvailableMemory(capacityVO.getAvailableMemory());
        physCapacityVO.setAvailablePhysicalMemory(capacityVO.getAvailablePhysicalMemory());
        physCapacityVO = dbf.getEntityManager().merge(physCapacityVO);
        logCapacityChange();
    }

    @Transactional
    private boolean _run(HostCapacityUpdaterRunnable runnable) {
        if (!lockCapacity()) {
            logDeletedHost();
            return false;
        }

        HostCapacityVO cap = runnable.call(capacityVO);
        if (cap != null) {
            capacityVO = cap;
            merge();
            return true;
        }
        return false;
    }

    @DeadlockAutoRestart
    public boolean run(HostCapacityUpdaterRunnable runnable) {
        return _run(runnable);
    }

}
