package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DeadlockAutoRestart;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.HostCpuOverProvisioningManager;
import org.zstack.header.allocator.ReservedHostCapacity;
import org.zstack.header.allocator.ServerReservedCapacityExtensionPoint;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.server.CapacityUsage;
import org.zstack.header.server.PhysicalServerCapacityState;
import org.zstack.header.server.PhysicalServerCapacityVO;
import org.zstack.header.server.PhysicalServerRoleProvider;
import org.zstack.header.server.PhysicalServerRoleVO;
import org.zstack.header.server.PhysicalServerRoleVO_;
import org.zstack.header.server.ServerRoleType;
import org.zstack.header.server.PhysicalServerVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_ALLOCATOR_10038;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_ALLOCATOR_10039;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_ALLOCATOR_10040;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_ALLOCATOR_10041;

/**
 * Phase 3 Wave 1 U4 — unified path 2/3 server-level capacity recalculator.
 *
 * <p>Distinct from {@link HostCapacityUpdater} (path 1 / W1-W6 backward-compat). This component
 * does <b>not</b> replace {@code HostCapacityUpdater}; both coexist:
 * <ul>
 *   <li>{@code HostCapacityUpdater}   — runnable-driven, single-host POJO mutate (W1-W6 callers).</li>
 *   <li>{@code PhysicalServerCapacityUpdater.recalculate(serverUuid)} — full server-level
 *       aggregate over <i>all</i> active {@link PhysicalServerRoleVO} rows for the server.
 *       Reads each role module's {@link PhysicalServerRoleProvider#getCapacityConsumption}
 *       (business-tax bucket) and writes back
 *       {@link PhysicalServerCapacityVO#availableCpu}/{@code availableMemory} +
 *       {@link PhysicalServerCapacityState#Ready}.</li>
 * </ul>
 *
 * <p><b>Locking</b>: PESSIMISTIC_WRITE on {@code PhysicalServerCapacityVO} keyed by
 * {@code serverUuid} (NB-30 single-lock-key invariant — same key as {@code HostCapacityUpdater}).
 *
 * <p><b>Fail-loud</b> (ADR-001 / NB-24):
 * <ul>
 *   <li>Missing {@code PhysicalServerVO} → {@link OperationFailureException}.</li>
 *   <li>Any {@code RoleProvider.getCapacityConsumption} throw → wrap and abort with no PSC mutation.</li>
 *   <li>No {@code PhysicalServerRoleProvider} bean for a registered role type → abort fail-loud
 *       (silent zero-credit pollutes ledger; see Phase 2C learnings §3 fact #4).</li>
 * </ul>
 *
 * <p><b>Total CPU / memory authority</b>: this updater does <b>not</b> overwrite
 * {@code totalCpu / totalMemory} — those are populated by hardware-discovery flow (out of scope)
 * and by {@code HostCpuOverProvisioningManager} (Wave 3 U12). Only available* + capacityState are
 * mutated here.
 *
 * <p><b>Safety buffer</b> (Wave 2 U9, AC-CM-13):
 * {@code cpuBuffer = max(4, totalCpu * PHYSICAL_SERVER_CPU_SAFETY_BUFFER_PERCENT / 100)},
 * {@code memBuffer = max(4 GiB, totalMemory * PHYSICAL_SERVER_MEMORY_SAFETY_BUFFER_PERCENT / 100)}.
 * Defaults are 5% / 10% (see {@code conf/globalConfig/hostAllocator.xml}). Plus any contribution
 * from {@link ServerReservedCapacityExtensionPoint} implementors (e.g. cordoned container nodes,
 * BM2 maintenance markers).
 */
@Component
public class PhysicalServerCapacityUpdater {
    private static final CLogger logger = Utils.getLogger(PhysicalServerCapacityUpdater.class);

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private PluginRegistry pluginRgty;

    @Autowired
    private HostCpuOverProvisioningManager cpuRatioMgr;

    // Rule 15: lazy getter pattern — never field-initialize from pluginRgty.
    private volatile Map<String, PhysicalServerRoleProvider> providerByRoleType;
    private volatile List<ServerReservedCapacityExtensionPoint> reservedExts;

    private Map<String, PhysicalServerRoleProvider> getProviderByRoleType() {
        if (providerByRoleType == null) {
            Map<String, PhysicalServerRoleProvider> m = new HashMap<>();
            List<PhysicalServerRoleProvider> exts =
                    pluginRgty.getExtensionList(PhysicalServerRoleProvider.class);
            if (exts != null) {
                for (PhysicalServerRoleProvider p : exts) {
                    m.put(p.getRoleType().toString(), p);
                }
            }
            providerByRoleType = m;
        }
        return providerByRoleType;
    }

    private List<ServerReservedCapacityExtensionPoint> getReservedExts() {
        if (reservedExts == null) {
            List<ServerReservedCapacityExtensionPoint> exts =
                    pluginRgty.getExtensionList(ServerReservedCapacityExtensionPoint.class);
            reservedExts = exts != null ? exts : java.util.Collections.<ServerReservedCapacityExtensionPoint>emptyList();
        }
        return reservedExts;
    }

    /**
     * Recalculate {@link PhysicalServerCapacityVO} for the given physical server.
     *
     * @param serverUuid {@link PhysicalServerVO#getUuid()}.
     * @throws OperationFailureException if the PhysicalServer or PSC row is missing, or any role
     *         provider call fails. PSC is <b>not</b> partially mutated on error.
     */
    @DeadlockAutoRestart
    public void recalculate(String serverUuid) {
        if (serverUuid == null) {
            throw new OperationFailureException(failLoud(ORG_ZSTACK_COMPUTE_ALLOCATOR_10038,
                    "PhysicalServerCapacityUpdater.recalculate called with null serverUuid"));
        }
        _recalculate(serverUuid);
    }

    /**
     * Build an {@link ErrorCode} directly without going through {@link org.zstack.core.Platform#operr}.
     * Bypassing {@code Platform} keeps fail-loud paths unit-testable: {@code Platform.<clinit>}
     * scans the full inventory + searchConfig graph and is fragile under module-isolated test
     * classpaths. Production behavior is unchanged — the global error code constant is still
     * recorded; only the i18n elaboration cache (which Platform owns) is bypassed.
     */
    private static ErrorCode failLoud(String globalCode, String fmt, Object... args) {
        ErrorCode ec = new ErrorCode(globalCode, String.format(fmt, args));
        ec.setGlobalErrorCode(globalCode);
        return ec;
    }

    @Transactional
    protected void _recalculate(String serverUuid) {
        // ---- 1. Verify the parent PhysicalServerVO exists (fail-loud per ADR-001). ----
        PhysicalServerVO ps = dbf.getEntityManager().find(PhysicalServerVO.class, serverUuid);
        if (ps == null) {
            throw new OperationFailureException(failLoud(ORG_ZSTACK_COMPUTE_ALLOCATOR_10039,
                    "PhysicalServer[uuid:%s] not found", serverUuid));
        }

        // ---- 2. Lock the PSC row (NB-30 single-lock-key invariant). ----
        PhysicalServerCapacityVO psc = dbf.getEntityManager()
                .find(PhysicalServerCapacityVO.class, serverUuid, LockModeType.PESSIMISTIC_WRITE);
        if (psc == null) {
            throw new OperationFailureException(failLoud(ORG_ZSTACK_COMPUTE_ALLOCATOR_10040,
                    "PhysicalServerCapacityVO[serverUuid:%s] not found — InitPhysicalServerCapacityFlow"
                            + " must run before recalculate", serverUuid));
        }

        // ---- 3. Aggregate consumption across all active roles for this server. ----
        List<PhysicalServerRoleVO> roles = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, serverUuid)
                .list();

        long consumedCpu = 0L;
        long consumedMemory = 0L;
        boolean anyExclusive = false;
        String kvmRoleUuid = null;
        Map<String, PhysicalServerRoleProvider> providers = getProviderByRoleType();

        for (PhysicalServerRoleVO role : roles) {
            String roleType = role.getRoleType();
            if (ServerRoleType.KVM_HOST.toString().equals(roleType)) {
                kvmRoleUuid = role.getRoleUuid();
            }
            PhysicalServerRoleProvider provider = providers.get(roleType);
            if (provider == null) {
                // Fail-loud: a registered RoleVO with no provider bean would silently credit zero
                // (Phase 2C learnings §3 fact #4 — pollutes the ledger). Better to abort.
                throw new OperationFailureException(failLoud(ORG_ZSTACK_COMPUTE_ALLOCATOR_10041,
                        "no PhysicalServerRoleProvider registered for roleType[%s] (serverUuid[%s],"
                                + " roleUuid[%s])", roleType, serverUuid, role.getRoleUuid()));
            }
            CapacityUsage usage;
            try {
                usage = provider.getCapacityConsumption(serverUuid, role.getRoleUuid());
            } catch (RuntimeException e) {
                throw new OperationFailureException(failLoud(ORG_ZSTACK_COMPUTE_ALLOCATOR_10041,
                        "PhysicalServerRoleProvider[roleType:%s].getCapacityConsumption failed for"
                                + " server[uuid:%s] role[uuid:%s]: %s",
                        roleType, serverUuid, role.getRoleUuid(), e.getMessage()));
            }
            if (usage == null) {
                continue;
            }
            consumedCpu += usage.getUsedCpu();
            consumedMemory += usage.getUsedMemory();
            if (usage.isExclusive()) {
                anyExclusive = true;
            }
        }

        // ---- 4. Compute available, write PSC. ----
        // totalCpu / totalMemory authority: PSC fields populated by hardware-discovery flow +
        // HostCpuOverProvisioningManager (Wave 3 U12); this updater intentionally does NOT
        // overwrite them (mirrors HostCapacityUpdater.merge() 3-field writeback policy).
        long totalCpu = psc.getTotalCpu();
        long totalMemory = psc.getTotalMemory();
        long reservedMemory = psc.getReservedMemory();

        // INTERNAL_EXCLUSIVE consumer policy (Phase 2C learnings §architectural implications):
        // when any role flagged exclusive, available = 0 regardless of usedCpu/usedMemory magnitude.
        long availableCpu;
        long availableMemory;
        if (anyExclusive) {
            availableCpu = 0L;
            availableMemory = 0L;
        } else {
            long extReservedCpu = 0L;
            long extReservedMemory = 0L;
            for (ServerReservedCapacityExtensionPoint ext : getReservedExts()) {
                ReservedHostCapacity rc = ext.getReservedCapacityForPhysicalServer(serverUuid);
                if (rc == null) {
                    continue;
                }
                // P1-1: per-extension whole-or-nothing. A misbehaving impl returning a
                // partial-negative tuple (e.g. cpu=10, mem=-1) used to silently honour cpu
                // and drop mem — the SPI contract does not define partial-honor. Reject the
                // whole contribution and log so the offending impl surfaces. Zero is a
                // valid no-op (e.g. Container with no cordoned pods on this host).
                long cpuRsv = rc.getReservedCpuCapacity();
                long memRsv = rc.getReservedMemoryCapacity();
                if (cpuRsv < 0 || memRsv < 0) {
                    logger.warn(String.format(
                            "ServerReservedCapacityExtensionPoint[%s] returned negative "
                                    + "reservation for server[uuid:%s] (cpu=%d, mem=%d); "
                                    + "discarding entire contribution.",
                            ext.getClass().getName(), serverUuid, cpuRsv, memRsv));
                    continue;
                }
                extReservedCpu += cpuRsv;
                extReservedMemory += memRsv;
            }

            // Mixed-deployment safety buffer: only when this physical server hosts more
            // than one role (e.g. KVM + Container coexisting) does the implicit buffer
            // apply. Single-role hosts use HostVO/PSC reservedMemory + ext-reported
            // reservation as their sole reservation mechanism.
            long cpuBuffer = 0L;
            long memBuffer = 0L;
            if (roles.size() > 1) {
                cpuBuffer = PhysicalServerCapacityBuffers.calcCpuBuffer(totalCpu);
                memBuffer = PhysicalServerCapacityBuffers.calcMemBuffer(totalMemory);
            }

            availableCpu = totalCpu - consumedCpu - cpuBuffer - extReservedCpu;
            availableMemory = totalMemory - consumedMemory - reservedMemory - memBuffer - extReservedMemory;
        }

        psc.setAvailableCpu(availableCpu);
        psc.setAvailableMemory(availableMemory);
        psc.setCapacityState(PhysicalServerCapacityState.Ready);
        dbf.getEntityManager().merge(psc);

        if (logger.isTraceEnabled()) {
            logger.trace(String.format(
                    "[PhysicalServer Capacity] recalculated server[uuid:%s]: "
                            + "totalCpu=%d, consumedCpu=%d, exclusive=%s, availableCpu=%d / "
                            + "totalMemory=%d, consumedMemory=%d, reservedMemory=%d, availableMemory=%d",
                    serverUuid, totalCpu, consumedCpu, anyExclusive, availableCpu,
                    totalMemory, consumedMemory, reservedMemory, availableMemory));
        }
    }
}
