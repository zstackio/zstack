package org.zstack.kvm;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.allocator.HostCapacityVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostVO;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.AddHostReply;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostDeletionMsg;
import org.zstack.header.message.MessageReply;
import org.zstack.header.server.*;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 * KVM Host role provider implementing the v3 {@link PhysicalServerRoleProvider} SPI (FR-022,
 * 2026-04-16).
 *
 * <p>Phase 2C (U8) wire-up responsibilities (per role SPI PRD §2.2):
 * <ul>
 *   <li>{@code createRoleEntity}: forward to {@code AddKVMHostMsg} with {@code serverUuid}
 *       threaded through so the PS-first flow (APIAttachPhysicalServerRoleMsg) and the legacy
 *       flow (APIAddKVMHostMsg) run the same {@code HostManagerImpl.doAddHost} code path.
 *   <li>{@code deleteRoleEntity}: forward to {@code DeleteHostMsg}
 *   <li>{@code getCapacityConsumption}: read the already-aggregated {@code HostCapacityVO} for
 *       the host (used = total − available). Keeps the RoleProvider at the Host/HCV layer and
 *       does not re-aggregate {@code VmInstanceVO} rows — the existing capacity update path
 *       is the canonical source.
 *   <li>{@code getWorkloadStatus}: fill {@code detachBlockReason / powerOffBlockReason /
 *       powerResetBlockReason / migrationBlockReason / maintenanceBlockReason} based on
 *       the VM inventory. {@code maintenanceBlockReason} fires when any host-bound VM is
 *       in a state libvirt cannot live-migrate out of (see {@code UN_MIGRATABLE_STATES}).
 * </ul>
 *
 * <p>U11 note: the {@code AddKVMHostMsg.serverUuid} field is carried through the message but
 * FlowChain persistence of serverUuid into HostVO is deferred to Phase 2C U11.
 */
public class KvmRoleProvider implements PhysicalServerRoleProvider {
    private static final CLogger logger = Utils.getLogger(KvmRoleProvider.class);

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private CloudBus bus;

    @Autowired
    private HostCapacityOverProvisioningManager memRatioMgr;

    /**
     * States considered "host-bound": the VM's {@code hostUuid} is set and the VM is still
     * owned by this host (i.e. a destructive op on the host would orphan / kill the VM).
     *
     * <p>Criterion: {@code VmInstanceState.values()} minus terminal / off states where the
     * host is not bound to the VM's runtime:
     * <ul>
     *   <li>Excluded: {@code Created, Stopped, Destroyed, Expunged, Error, Destroying,
     *       Expunging} — VM is not running on the host or is being terminated.
     *   <li>Included: {@code Running, Starting, Stopping, Rebooting, Migrating, Pausing,
     *       Paused, Resuming, VolumeMigrating, VolumeRecovering, Unknown, NoState, Crashed}
     *       — the host holds live libvirt state that would be orphaned by host power-off
     *       / detach / reset.
     * </ul>
     *
     * <p>Note: {@code Crashed} and {@code Unknown} are included because libvirt still pins
     * these VMs to this host for recovery; an unannounced host teardown would prevent the
     * VM from ever transitioning back to Running.
     */
    private static final List<VmInstanceState> ACTIVE_STATES = list(
            VmInstanceState.Starting,
            VmInstanceState.Running,
            VmInstanceState.Stopping,
            VmInstanceState.Rebooting,
            VmInstanceState.Migrating,
            VmInstanceState.Pausing,
            VmInstanceState.Paused,
            VmInstanceState.Resuming,
            VmInstanceState.VolumeMigrating,
            VmInstanceState.VolumeRecovering,
            VmInstanceState.Unknown,
            VmInstanceState.NoState,
            VmInstanceState.Crashed
    );

    /**
     * States from which libvirt live-migration is NOT possible. If any host-bound VM is
     * in one of these states, an attempted maintenance-mode evacuation in
     * {@code MaintenanceHostExtensionPoint} will fail mid-flow. Surface this up-front as
     * {@code maintenanceBlockReason} so the orchestrator can refuse the op cleanly.
     */
    private static final List<VmInstanceState> UN_MIGRATABLE_STATES = list(
            VmInstanceState.Unknown,
            VmInstanceState.Crashed,
            VmInstanceState.NoState,
            VmInstanceState.Pausing,
            VmInstanceState.Paused
    );

    @Override
    public ServerRoleType getRoleType() {
        return ServerRoleType.KVM_HOST;
    }

    @Override
    public SchedulingMode getSchedulingMode() {
        return SchedulingMode.INTERNAL_SHARED;
    }

    @Override
    public Optional<ServerRoleType> classify(HostVO hvo) {
        // KVMHostVO catches both plain KVM hosts and BareMetal2GatewayVO subclass
        // (gateway is structurally a KVM host even though hypervisorType="baremetal2").
        return hvo instanceof KVMHostVO ? Optional.of(ServerRoleType.KVM_HOST) : Optional.empty();
    }

    /**
     * Creates a KVM HostVO by forwarding to {@code AddKVMHostMsg} with credentials from
     * {@code roleConfig}. Required roleConfig keys: {@code username}, {@code password}.
     * Optional: {@code sshPort} (default 22), {@code name} (falls back to managementIp).
     *
     * @return the created HostVO uuid, which is stored as {@code PhysicalServerRoleVO.roleUuid}.
     * @throws OperationFailureException if username or password is missing from roleConfig.
     */
    @Override
    public void createRoleEntity(CreateRoleEntityContext ctx, ReturnValueCompletion<String> completion) {
        Map<String, String> cfg = ctx.getRoleConfig();

        String username = cfg.get("username");
        if (username == null || username.isEmpty()) {
            throw new OperationFailureException(
                    operr(ORG_ZSTACK_KVM_10165, "roleConfig missing required key 'username' for KVM host creation"));
        }
        String password = cfg.get("password");
        if (password == null || password.isEmpty()) {
            throw new OperationFailureException(
                    operr(ORG_ZSTACK_KVM_10163, "roleConfig missing required key 'password' for KVM host creation"));
        }

        int sshPort = 22;
        String portStr = cfg.get("sshPort");
        if (portStr != null && !portStr.isEmpty()) {
            sshPort = Integer.parseInt(portStr);
        }

        // Null-check SPI-edge inputs: AddKVMHostMsg / doAddHost assume both are non-null
        // and non-empty. Fail fast with a typed ErrorCode rather than NPE deep in the flow.
        if (ctx.getClusterUuid() == null || ctx.getClusterUuid().isEmpty()) {
            throw new OperationFailureException(
                    operr(ORG_ZSTACK_KVM_10166,
                            "CreateRoleEntityContext missing required 'clusterUuid' for KVM host creation"));
        }
        if (ctx.getManagementIp() == null || ctx.getManagementIp().isEmpty()) {
            throw new OperationFailureException(
                    operr(ORG_ZSTACK_KVM_10166,
                            "CreateRoleEntityContext missing required 'managementIp' for KVM host creation"));
        }

        String name = cfg.getOrDefault("name", ctx.getManagementIp());

        AddKVMHostMsg msg = new AddKVMHostMsg();
        msg.setName(name);
        msg.setManagementIp(ctx.getManagementIp());
        msg.setClusterUuid(ctx.getClusterUuid());
        msg.setUsername(username);
        msg.setPassword(password);
        msg.setSshPort(sshPort);
        msg.setServerUuid(ctx.getServerUuid());
        if (ctx.getPreGeneratedRoleUuid() != null) {
            msg.setResourceUuid(ctx.getPreGeneratedRoleUuid());
        }
        if (ctx.getAccountUuid() != null) {
            msg.setAccountUuid(ctx.getAccountUuid());
        }
        bus.makeLocalServiceId(msg, HostConstant.SERVICE_ID);

        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                AddHostReply addReply = reply.castReply();
                completion.success(addReply.getInventory().getUuid());
            }
        });
    }

    /**
     * Deletes the KVM HostVO identified by {@code roleUuid} by forwarding to
     * {@code HostDeletionMsg} (the cascade-framework deletion message handled by
     * {@code HostBase}). PhysicalServerRoleVO deletion is handled by the caller's cascade.
     */
    @Override
    public void deleteRoleEntity(String roleUuid, Completion completion) {
        HostDeletionMsg msg = new HostDeletionMsg();
        msg.setHostUuid(roleUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, roleUuid);

        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                completion.success();
            }
        });
    }

    /**
     * Returns the used CPU / memory for this KVM host by aggregating live VM rows directly.
     * {@code roleUuid} for the KVM role equals the legacy {@code HostVO.uuid}.
     *
     * <p>Used CPU is the raw {@code sum(VmInstanceVO.cpuNum)} (host total already pre-applies
     * the cpu over-provisioning ratio in {@code PhysicalServerCapacityVO.totalCpu} via
     * {@code HostCpuOverProvisioningManagerImpl}). Used memory is wrapped through
     * {@code HostCapacityOverProvisioningManager.calculateMemoryByRatio} so a memory
     * over-provisioning ratio change immediately moves {@code availableMemory} on the next
     * recalculate.
     *
     * <p>States excluded from the aggregation match the legacy
     * {@code HostAllocatorManagerImpl.handle(RecalculateHostCapacityMsg)} path:
     * {@link VmInstanceState#Destroyed}, {@link VmInstanceState#Created},
     * {@link VmInstanceState#Destroying}, {@link VmInstanceState#Stopped} — those VMs do
     * not consume host runtime capacity.
     */
    @Override
    public CapacityUsage getCapacityConsumption(String serverUuid, String roleUuid) {
        // hypervisorType filter excludes BM2 instances whose hostUuid is the BM2
        // gateway (a KVM host playing gateway role) — those don't consume gateway
        // CPU/memory, their capacity is accounted on the chassis via Bm2RoleProvider.
        String sql = "select sum(vm.cpuNum), sum(vm.memorySize)" +
                " from VmInstanceVO vm" +
                " where vm.hostUuid = :hostUuid" +
                " and vm.hypervisorType = 'KVM'" +
                " and vm.state not in (:excludedStates)";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("hostUuid", roleUuid);
        q.setParameter("excludedStates", list(
                VmInstanceState.Destroyed,
                VmInstanceState.Created,
                VmInstanceState.Destroying,
                VmInstanceState.Stopped,
                VmInstanceState.Expunging));
        Tuple t = q.getSingleResult();
        Long sumCpu = t.get(0, Long.class);
        Long sumMemory = t.get(1, Long.class);

        CapacityUsage usage = new CapacityUsage();
        if (sumCpu != null) {
            usage.setUsedCpu(sumCpu);
        }
        if (sumMemory != null) {
            usage.setUsedMemory(memRatioMgr.calculateMemoryByRatio(roleUuid, sumMemory));
        }
        return usage;
    }

    /**
     * Fills block reasons for destructive operations on the KVM host role:
     * <ul>
     *   <li>{@code detachBlockReason}: non-null when any host-bound VMs are present
     *       (see {@link #ACTIVE_STATES}) — detach would orphan them. Includes a per-state
     *       breakdown to help the operator decide how to clear the host.
     *   <li>{@code powerOffBlockReason}: same condition — power-off would kill all VMs
     *       holding libvirt state on this host.
     *   <li>{@code powerResetBlockReason}: same condition — a hard reset would crash them.
     *   <li>{@code maintenanceBlockReason}: non-null when one or more VMs are in a state
     *       that libvirt cannot live-migrate out of (see {@link #UN_MIGRATABLE_STATES}).
     *       {@code MaintenanceHostExtensionPoint} drives evacuation internally but would
     *       fail mid-flow on these VMs, so the SPI surfaces the block up-front.
     *   <li>{@code migrationBlockReason}: non-null when any VM on this host is currently
     *       Migrating (source side). Per {@code VmInstanceBase} semantics, during the
     *       Migrating state {@code hostUuid} still points at the source host — it only
     *       flips to the destination when the flow transitions the VM back to Running.
     *       {@code lastHostUuid} is not usable as a target proxy: it is set to the
     *       previous-successful host only on Running/Stopped/Destroyed transitions, so
     *       during Migrating it is stale. Target-side protection is out of scope here;
     *       source-side blocking is sufficient because evacuating the source would kill
     *       the in-flight migration.
     * </ul>
     *
     * The activeWorkloads list is also populated for UI rendering.
     */
    @Override
    public RoleWorkloadStatus getWorkloadStatus(String serverUuid, String roleUuid) {
        // Load all active VMs on this host in one query.
        List<VmInstanceVO> activeVms = SQL.New(
                "select v from VmInstanceVO v" +
                " where v.hostUuid = :hostUuid" +
                " and v.state in (:states)",
                VmInstanceVO.class)
                .param("hostUuid", roleUuid)
                .param("states", ACTIVE_STATES)
                .list();

        RoleWorkloadStatus status = new RoleWorkloadStatus();
        status.setActiveWorkloadCount(activeVms.size());

        if (!activeVms.isEmpty()) {
            // Populate workload refs for UI.
            for (VmInstanceVO vm : activeVms) {
                WorkloadRef ref = new WorkloadRef();
                ref.setUuid(vm.getUuid());
                ref.setName(vm.getName());
                ref.setType("VM");
                ref.setState(vm.getState().toString());
                status.getActiveWorkloads().add(ref);
            }

            int vmCount = activeVms.size();
            String stateBreakdown = activeVms.stream()
                    .collect(Collectors.groupingBy(VmInstanceVO::getState, Collectors.counting()))
                    .entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .sorted()
                    .collect(Collectors.joining(", "));

            status.setDetachBlockReason(String.format(
                    "KVM host has %d host-bound VM(s) [%s]; detach would orphan them",
                    vmCount, stateBreakdown));
            status.setPowerOffBlockReason(String.format(
                    "KVM host has %d host-bound VM(s); power-off would terminate them",
                    vmCount));
            status.setPowerResetBlockReason(String.format(
                    "KVM host has %d host-bound VM(s); power-reset would crash them",
                    vmCount));
        }

        // Migration block: set when any VM on this host is Migrating — i.e. this host
        // is the source side of an in-flight migration. See Javadoc above for why
        // target-side cannot be inferred from VmInstanceVO fields alone.
        boolean isMigrationSource = activeVms.stream()
                .anyMatch(v -> v.getState() == VmInstanceState.Migrating);

        if (isMigrationSource) {
            status.setMigrationBlockReason(
                    "KVM host is the source of an ongoing VM migration; wait for it to complete");
        }

        // Maintenance block: only when at least one host-bound VM is in a state libvirt
        // cannot live-migrate out of. Reuse the activeVms list to avoid a second query.
        List<VmInstanceVO> unMigratable = activeVms.stream()
                .filter(v -> UN_MIGRATABLE_STATES.contains(v.getState()))
                .collect(Collectors.toList());
        if (!unMigratable.isEmpty()) {
            String unMigratableBreakdown = unMigratable.stream()
                    .collect(Collectors.groupingBy(VmInstanceVO::getState, Collectors.counting()))
                    .entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .sorted()
                    .collect(Collectors.joining(", "));
            status.setMaintenanceBlockReason(String.format(
                    "KVM host has %d non-live-migratable VM(s) [%s]; " +
                            "resolve these (stop / recover) before entering maintenance",
                    unMigratable.size(), unMigratableBreakdown));
        }

        return status;
    }
}
