package org.zstack.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.cluster.ClusterAO_;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.longjob.LongJobConstants;
import org.zstack.header.longjob.SubmitLongJobMsg;
import org.zstack.header.longjob.SubmitLongJobReply;
import org.zstack.header.server.*;
import org.zstack.server.hardware.HardwareDiscoveryScheduler;
import org.zstack.server.hardware.PhysicalServerHardwareService;
import org.zstack.server.hardware.UnifiedHardwareInfo;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.SshCmdHelper;

import javax.persistence.LockModeType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

public class PhysicalServerManagerImpl extends AbstractService implements PhysicalServerManager {
    private static final CLogger logger = Utils.getLogger(PhysicalServerManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CascadeFacade casf;
    @Autowired(required = false)
    private PhysicalServerAutoAssociator autoAssociator;
    @Autowired
    private HardwareDiscoveryScheduler hardwareDiscoveryScheduler;
    @Autowired
    private PhysicalServerHardwareService hardwareService;
    @Autowired
    private PhysicalServerIpmiPowerExecutor ipmiPowerExecutor;
    @Autowired
    private PhysicalServerScanner physicalServerScanner;
    @Autowired(required = false)
    private PhysicalServerPowerTracker powerTracker;
    @Autowired(required = false)
    private List<PhysicalServerRoleProvider> roleProviderList = java.util.Collections.emptyList();
    @Autowired
    private PhysicalServerEnqueueDiscoveryHook enqueueDiscoveryHook;

    private Map<String, PhysicalServerRoleProvider> roleProviders = new HashMap<>();

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateServerPoolMsg) {
            handle((APICreateServerPoolMsg) msg);
        } else if (msg instanceof APIDeleteServerPoolMsg) {
            handle((APIDeleteServerPoolMsg) msg);
        } else if (msg instanceof APIUpdateServerPoolMsg) {
            handle((APIUpdateServerPoolMsg) msg);
        } else if (msg instanceof APIChangeClusterServerPoolMsg) {
            handle((APIChangeClusterServerPoolMsg) msg);
        } else if (msg instanceof APICreatePhysicalServerMsg) {
            handle((APICreatePhysicalServerMsg) msg);
        } else if (msg instanceof APIDeletePhysicalServerMsg) {
            handle((APIDeletePhysicalServerMsg) msg);
        } else if (msg instanceof APIUpdatePhysicalServerMsg) {
            handle((APIUpdatePhysicalServerMsg) msg);
        } else if (msg instanceof APIChangePhysicalServerStateMsg) {
            handle((APIChangePhysicalServerStateMsg) msg);
        } else if (msg instanceof APICreateProvisionNetworkMsg) {
            handle((APICreateProvisionNetworkMsg) msg);
        } else if (msg instanceof APIDeleteProvisionNetworkMsg) {
            handle((APIDeleteProvisionNetworkMsg) msg);
        } else if (msg instanceof APIUpdateProvisionNetworkMsg) {
            handle((APIUpdateProvisionNetworkMsg) msg);
        } else if (msg instanceof APIAttachProvisionNetworkToClusterMsg) {
            handle((APIAttachProvisionNetworkToClusterMsg) msg);
        } else if (msg instanceof APIDetachProvisionNetworkFromClusterMsg) {
            handle((APIDetachProvisionNetworkFromClusterMsg) msg);
        } else if (msg instanceof APIAttachProvisionNetworkToPoolMsg) {
            handle((APIAttachProvisionNetworkToPoolMsg) msg);
        } else if (msg instanceof APIDetachProvisionNetworkFromPoolMsg) {
            handle((APIDetachProvisionNetworkFromPoolMsg) msg);
        } else if (msg instanceof APIAttachPhysicalServerRoleMsg) {
            handle((APIAttachPhysicalServerRoleMsg) msg);
        } else if (msg instanceof APIDetachPhysicalServerRoleMsg) {
            handle((APIDetachPhysicalServerRoleMsg) msg);
        } else if (msg instanceof APIPowerOnPhysicalServerMsg) {
            handle((APIPowerOnPhysicalServerMsg) msg);
        } else if (msg instanceof APIPowerOffPhysicalServerMsg) {
            handle((APIPowerOffPhysicalServerMsg) msg);
        } else if (msg instanceof APIPowerResetPhysicalServerMsg) {
            handle((APIPowerResetPhysicalServerMsg) msg);
        } else if (msg instanceof APIScanPhysicalServersMsg) {
            handle((APIScanPhysicalServersMsg) msg);
        } else if (msg instanceof APIProvisionPhysicalServerMsg) {
            handle((APIProvisionPhysicalServerMsg) msg);
        } else if (msg instanceof APIDiscoverPhysicalServerHardwareMsg) {
            handle((APIDiscoverPhysicalServerHardwareMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof PingPhysicalServerMsg) {
            handle((PingPhysicalServerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(PingPhysicalServerMsg msg) {
        PingPhysicalServerReply reply = new PingPhysicalServerReply();
        PhysicalServerVO vo = dbf.findByUuid(msg.getUuid(), PhysicalServerVO.class);
        if (vo == null) {
            reply.setError(operr("PhysicalServer[uuid:%s] not found", msg.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        PhysicalServerPowerStatus probed = probePowerStatus(vo);
        if (vo.getPowerStatus() != probed) {
            vo.setPowerStatus(probed);
            dbf.update(vo);
        }
        reply.setPowerStatus(probed);
        bus.reply(msg, reply);
    }

    private PhysicalServerPowerStatus probePowerStatus(PhysicalServerVO vo) {
        // Test seam: PhysicalServerPowerTracker.powerOverride is null in production; IT cases
        // set it to drive the handler without a real BMC. Mirrors the static-override pattern
        // used by PhysicalServerScanner.{probe,power}Override.
        if (PhysicalServerPowerTracker.powerOverride != null) {
            return PhysicalServerPowerTracker.powerOverride.apply(vo.getOobAddress(), vo.getOobUsername());
        }
        if (vo.getOobAddress() == null || vo.getOobUsername() == null || vo.getOobPassword() == null) {
            return PhysicalServerPowerStatus.POWER_UNKNOWN;
        }

        String passFile = PathUtil.createTempFileWithContent(vo.getOobPassword());
        try {
            int port = vo.getOobPort() == null ? 623 : vo.getOobPort();
            String cmd = String.format(
                    "timeout 5 ipmitool -I lanplus -H %s -p %d -U %s -f %s chassis power status",
                    SshCmdHelper.shellQuote(vo.getOobAddress()),
                    port,
                    SshCmdHelper.shellQuote(vo.getOobUsername()),
                    SshCmdHelper.shellQuote(passFile));
            ShellResult ret = ShellUtils.runAndReturn(cmd);
            if (ret.getRetCode() != 0) {
                return PhysicalServerPowerStatus.POWER_UNKNOWN;
            }
            return PhysicalServerPowerStatusParser.parse(ret.getStdout());
        } finally {
            PathUtil.forceRemoveFile(passFile);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(PhysicalServerConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();

        // v5.5.18: scan PS needing hardware discovery on MN start (role SPI PRD §2.5b NB-19).
        // TODO(U15): tighten with NOT EXISTS filter against PhysicalServerHardwareDetailVO
        //            once that VO is available, to skip servers already discovered.
        List<String> orphanServerUuids = Q.New(PhysicalServerVO.class)
                .select(PhysicalServerAO_.uuid)
                .listValues();
        orphanServerUuids.forEach(hardwareDiscoveryScheduler::enqueueDiscovery);

        return true;
    }

    private void populateExtensions() {
        for (PhysicalServerRoleProvider p : roleProviderList) {
            PhysicalServerRoleProvider old = roleProviders.get(p.getRoleType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format(
                    "duplicate PhysicalServerRoleProvider[%s, %s] for role type[%s]",
                    old.getClass().getName(), p.getClass().getName(), p.getRoleType()));
            }
            roleProviders.put(p.getRoleType().toString(), p);
        }

        // v5.5.18 M6: startup self-check for missing role providers. A split-repo deploy
        // where zstack OSS is pushed but the matching premium bump is missing will leave
        // one or more ServerRoleType entries without a provider; APIAttachPhysicalServerRoleMsg
        // against those role types will fail with a runtime error. Warn loudly at boot so
        // ops can catch the misconfigured deploy before a user hits it.
        for (String t : ServerRoleType.getAllTypeNames()) {
            if (!roleProviders.containsKey(t)) {
                logger.warn(String.format(
                    "No PhysicalServerRoleProvider registered for ServerRoleType[%s]; " +
                    "APIAttachPhysicalServerRoleMsg with this role type will fail. " +
                    "Likely cause: split-repo deploy (zstack pushed but premium bump missing).",
                    t));
            }
        }
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public PhysicalServerRoleProvider getRoleProvider(ServerRoleType type) {
        return roleProviders.get(type.toString());
    }

    /**
     * Acquire a PESSIMISTIC_WRITE row lock on {@code PhysicalServerVO.uuid = serverUuid}
     * (v5.5.18 B9 race fix, 2026-04-23). Serialises the entire
     * {@code [existence-check + createRoleEntity + attachRoleVO]} sequence per serverUuid so
     * that two concurrent {@code APIAttachPhysicalServerRoleMsg} with the same
     * {@code serverUuid+roleType} can NOT both fire {@code provider.createRoleEntity}
     * (bus.call AddKVMHostMsg / AddBareMetal2ChassisMsg / K8s sync) before the
     * {@code PhysicalServerRoleVO} uniqueness check runs.
     *
     * <p>Pre-v5.5.18 the lock was inside {@link #attachRoleVO} on
     * {@code PhysicalServerRoleVO WHERE serverUuid=X} — which locks an empty result set when
     * no role has ever attached, providing NO mutual exclusion against a second concurrent
     * insert-first attacker. A lock on the {@code PhysicalServerVO} row always targets a real
     * row (or fails fast with NotFound), giving a stable serialisation point.
     *
     * @throws OperationFailureException if the PS does not exist (single lock acquisition
     *     simultaneously proves existence, so callers MUST NOT re-check with
     *     {@code findByUuid} before this).
     */
    private PhysicalServerVO lockPhysicalServerForAttach(String serverUuid) {
        PhysicalServerVO locked = SQL.New(
                "select s from PhysicalServerVO s where s.uuid = :uuid",
                PhysicalServerVO.class)
                .param("uuid", serverUuid)
                .lock(LockModeType.PESSIMISTIC_WRITE)
                .find();
        if (locked == null) {
            throw new OperationFailureException(operr(
                "PhysicalServer[uuid:%s] not found", serverUuid));
        }
        return locked;
    }

    /**
     * Internal mutual-exclusion check + RoleVO persistence (v3, 2026-04-16). Called from the
     * {@code APIAttachPhysicalServerRoleMsg} handler after {@code RoleProvider.createRoleEntity}
     * returns a non-null {@code roleUuid}. Assumes the caller already holds the
     * {@link #lockPhysicalServerForAttach PhysicalServerVO row lock}; the PESSIMISTIC_WRITE on
     * {@code PhysicalServerRoleVO} here is now a belt-and-braces duplicate guard for the
     * RoleVO table itself (the real mutex lives on the PS row).
     */
    private PhysicalServerRoleVO attachRoleVO(String serverUuid, ServerRoleType roleType,
                                              String roleUuid, SchedulingMode mode) {
        List<PhysicalServerRoleVO> existingRoles = SQL.New(
                "select r from PhysicalServerRoleVO r where r.serverUuid = :serverUuid",
                PhysicalServerRoleVO.class)
                .param("serverUuid", serverUuid)
                .lock(LockModeType.PESSIMISTIC_WRITE)
                .list();

        for (PhysicalServerRoleVO existing : existingRoles) {
            if (existing.getRoleType().equals(roleType.toString())) {
                throw new OperationFailureException(operr(
                    "server[uuid:%s] already has role[type:%s]", serverUuid, roleType));
            }
            if (isExclusiveConflict(existing.getSchedulingMode(), mode)) {
                throw new OperationFailureException(operr(
                    "server[uuid:%s] has role[type:%s, mode:%s] which conflicts with new role[type:%s, mode:%s]",
                    serverUuid, existing.getRoleType(), existing.getSchedulingMode(),
                    roleType, mode));
            }
        }

        PhysicalServerRoleVO role = new PhysicalServerRoleVO();
        role.setUuid(Platform.getUuid());
        role.setServerUuid(serverUuid);
        role.setRoleType(roleType.toString());
        role.setRoleUuid(roleUuid);
        role.setSchedulingMode(mode);
        dbf.persist(role);
        return role;
    }

    private boolean isExclusiveConflict(SchedulingMode existing, SchedulingMode incoming) {
        if (existing == SchedulingMode.EXTERNAL_READONLY || incoming == SchedulingMode.EXTERNAL_READONLY) {
            return false;
        }
        if (existing == SchedulingMode.INTERNAL_EXCLUSIVE || incoming == SchedulingMode.INTERNAL_EXCLUSIVE) {
            return true;
        }
        return false;
    }

    // --- ServerPool handlers ---

    private void handle(APICreateServerPoolMsg msg) {
        ServerPoolVO vo = new ServerPoolVO();
        vo.setUuid(msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setZoneUuid(msg.getZoneUuid());
        vo.setPhysicalLocation(msg.getPhysicalLocation());
        vo.setNetworkTopology(msg.getNetworkTopology());
        vo.setState(ServerPoolState.Enabled);
        vo.setDefault(false);
        vo = dbf.persistAndRefresh(vo);

        APICreateServerPoolEvent evt = new APICreateServerPoolEvent(msg.getId());
        evt.setInventory(ServerPoolInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIDeleteServerPoolMsg msg) {
        SQL.New(ClusterVO.class)
            .eq(ClusterAO_.serverPoolUuid, msg.getUuid())
            .set(ClusterAO_.serverPoolUuid, null)
            .update();
        dbf.removeByPrimaryKey(msg.getUuid(), ServerPoolVO.class);
        APIDeleteServerPoolEvent evt = new APIDeleteServerPoolEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIUpdateServerPoolMsg msg) {
        ServerPoolVO vo = dbf.findByUuid(msg.getUuid(), ServerPoolVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("ServerPool[uuid:%s] not found", msg.getUuid()));
        }
        boolean update = false;
        if (msg.getName() != null) {
            vo.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
            update = true;
        }
        if (msg.getPhysicalLocation() != null) {
            vo.setPhysicalLocation(msg.getPhysicalLocation());
            update = true;
        }
        if (msg.getNetworkTopology() != null) {
            vo.setNetworkTopology(msg.getNetworkTopology());
            update = true;
        }
        if (update) {
            vo = dbf.updateAndRefresh(vo);
        }

        APIUpdateServerPoolEvent evt = new APIUpdateServerPoolEvent(msg.getId());
        evt.setInventory(ServerPoolInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIChangeClusterServerPoolMsg msg) {
        ServerPoolVO pool = dbf.findByUuid(msg.getServerPoolUuid(), ServerPoolVO.class);
        if (pool == null) {
            throw new OperationFailureException(operr("ServerPool[uuid:%s] not found", msg.getServerPoolUuid()));
        }

        SQL.New(ClusterVO.class)
            .eq(ClusterAO_.uuid, msg.getClusterUuid())
            .set(ClusterAO_.serverPoolUuid, msg.getServerPoolUuid())
            .update();

        APIChangeClusterServerPoolEvent evt = new APIChangeClusterServerPoolEvent(msg.getId());
        evt.setInventory(ServerPoolInventory.valueOf(pool));
        bus.publish(evt);
    }

    // --- PhysicalServer handlers ---

    private void handle(APICreatePhysicalServerMsg msg) {
        PhysicalServerVO vo = new PhysicalServerVO();
        vo.setUuid(msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setZoneUuid(msg.getZoneUuid());
        vo.setPoolUuid(msg.getPoolUuid());
        vo.setManagementIp(msg.getManagementIp());
        vo.setArchitecture(msg.getArchitecture());
        vo.setSerialNumber(msg.getSerialNumber());
        vo.setManufacturer(msg.getManufacturer());
        vo.setModel(msg.getModel());
        vo.setState(PhysicalServerState.Enabled);
        vo.setPowerStatus(PhysicalServerPowerStatus.POWER_UNKNOWN);
        vo.setOobManagementType(msg.getOobManagementType());
        vo.setOobAddress(msg.getOobAddress());
        vo.setOobPort(msg.getOobPort());
        vo.setOobUsername(msg.getOobUsername());
        vo.setOobPassword(msg.getOobPassword());
        vo = dbf.persistAndRefresh(vo);

        if (powerTracker != null) {
            powerTracker.track(vo.getUuid());
        }

        APICreatePhysicalServerEvent evt = new APICreatePhysicalServerEvent(msg.getId());
        evt.setInventory(PhysicalServerInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIDeletePhysicalServerMsg msg) {
        APIDeletePhysicalServerEvent evt = new APIDeletePhysicalServerEvent(msg.getId());
        PhysicalServerVO vo = dbf.findByUuid(msg.getUuid(), PhysicalServerVO.class);
        if (vo == null) {
            bus.publish(evt);
            return;
        }

        long activeRoleCount = Q.New(PhysicalServerRoleVO.class)
            .eq(PhysicalServerRoleVO_.serverUuid, msg.getUuid())
            .count();
        if (activeRoleCount > 0) {
            throw new OperationFailureException(operr(
                "Cannot delete PhysicalServer[uuid:%s]: %d role(s) still attached. Detach associated roles first.",
                msg.getUuid(), activeRoleCount
            ));
        }

        if (powerTracker != null) {
            powerTracker.untrack(msg.getUuid());
        }

        String issuer = PhysicalServerVO.class.getSimpleName();
        List<PhysicalServerInventory> ctx = Arrays.asList(PhysicalServerInventory.valueOf(vo));
        String deletionCode = APIDeleteMessage.DeletionMode.Permissive.equals(msg.getDeletionMode()) ?
                CascadeConstant.DELETION_DELETE_CODE : CascadeConstant.DELETION_FORCE_DELETE_CODE;
        casf.asyncCascade(deletionCode, issuer, ctx, new Completion(msg) {
            @Override
            public void success() {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIUpdatePhysicalServerMsg msg) {
        PhysicalServerVO vo = dbf.findByUuid(msg.getUuid(), PhysicalServerVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("PhysicalServer[uuid:%s] not found", msg.getUuid()));
        }
        boolean update = false;
        if (msg.getName() != null) { vo.setName(msg.getName()); update = true; }
        if (msg.getDescription() != null) { vo.setDescription(msg.getDescription()); update = true; }
        if (msg.getManagementIp() != null) { vo.setManagementIp(msg.getManagementIp()); update = true; }
        if (msg.getPoolUuid() != null) { vo.setPoolUuid(msg.getPoolUuid()); update = true; }
        if (msg.getArchitecture() != null) { vo.setArchitecture(msg.getArchitecture()); update = true; }
        if (msg.getSerialNumber() != null) { vo.setSerialNumber(msg.getSerialNumber()); update = true; }
        if (msg.getManufacturer() != null) { vo.setManufacturer(msg.getManufacturer()); update = true; }
        if (msg.getModel() != null) { vo.setModel(msg.getModel()); update = true; }
        if (msg.getOobManagementType() != null) { vo.setOobManagementType(msg.getOobManagementType()); update = true; }
        if (msg.getOobAddress() != null) { vo.setOobAddress(msg.getOobAddress()); update = true; }
        if (msg.getOobPort() != null) { vo.setOobPort(msg.getOobPort()); update = true; }
        if (msg.getOobUsername() != null) { vo.setOobUsername(msg.getOobUsername()); update = true; }
        if (msg.getOobPassword() != null) { vo.setOobPassword(msg.getOobPassword()); update = true; }
        if (update) {
            vo = dbf.updateAndRefresh(vo);
        }

        APIUpdatePhysicalServerEvent evt = new APIUpdatePhysicalServerEvent(msg.getId());
        evt.setInventory(PhysicalServerInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIChangePhysicalServerStateMsg msg) {
        PhysicalServerVO vo = dbf.findByUuid(msg.getUuid(), PhysicalServerVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("PhysicalServer[uuid:%s] not found", msg.getUuid()));
        }

        PhysicalServerStateEvent sevt = PhysicalServerStateEvent.valueOf(msg.getStateEvent());
        PhysicalServerState next = vo.getState().nextState(sevt);
        vo.setState(next);
        vo = dbf.updateAndRefresh(vo);

        APIChangePhysicalServerStateEvent evt = new APIChangePhysicalServerStateEvent(msg.getId());
        evt.setInventory(PhysicalServerInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIScanPhysicalServersMsg msg) {
        PhysicalServerScanner.ScanResult result = physicalServerScanner.scan(
                new PhysicalServerScanner.ScanSpec()
                        .setZoneUuid(msg.getZoneUuid())
                        .setPoolUuid(msg.getPoolUuid())
                        .setIpRange(msg.getIpRange())
                        .setOobPort(msg.getOobPort())
                        .setCredentials(msg.getCredentials())
                        .setTimeoutPerHost(msg.getTimeoutPerHost()));
        APIScanPhysicalServersEvent evt = new APIScanPhysicalServersEvent(msg.getId());
        evt.setDiscoveredCount(result.getDiscoveredCount());
        evt.setExistingCount(result.getExistingCount());
        evt.setUnreachableCount(result.getUnreachableCount());
        evt.setAuthFailedCount(result.getAuthFailedCount());
        evt.setDiscoveredServers(result.getDiscoveredServers());
        evt.setAuthFailedIps(result.getAuthFailedIps());
        bus.publish(evt);
    }

    // --- Role handlers ---

    private void handle(APIAttachPhysicalServerRoleMsg msg) {
        PhysicalServerRoleProvider provider = roleProviders.get(msg.getRoleType());
        if (provider == null) {
            throw new OperationFailureException(operr(
                "no RoleProvider registered for roleType[%s]", msg.getRoleType()));
        }

        // v5.5.18 U13: reject EXTERNAL_READONLY role types at the dispatcher boundary so the
        // user sees a clean operr instead of the stack trace ContainerRoleProvider.createRoleEntity
        // would throw from inside the lock + provider call. Container is currently the only
        // EXTERNAL_READONLY provider — its node lifecycle is driven by K8s, not Attach.
        if (provider.getSchedulingMode() == SchedulingMode.EXTERNAL_READONLY) {
            throw new OperationFailureException(operr(
                provider.getAttachUnsupportedErrorCode(),
                "role[type:%s] is EXTERNAL_READONLY and cannot be attached via API; " +
                "its lifecycle is driven externally (K8s node sync)",
                msg.getRoleType()));
        }

        // v5.5.18 B9: serialise the entire [existence-check + createRoleEntity +
        // attachRoleVO] sequence on the PhysicalServerVO row lock. Pre-fix, two concurrent
        // attach requests could both pass the findByUuid existence check and both fire
        // provider.createRoleEntity (bus.call AddKVMHostMsg → real SSH attempt), creating
        // duplicate HostVO rows before the RoleVO-level mutex ran. See
        // lockPhysicalServerForAttach javadoc for the full explanation.
        PhysicalServerVO server = lockPhysicalServerForAttach(msg.getServerUuid());

        // NB-24 / capacity PRD: write PhysicalServerRoleVO BEFORE provider.createRoleEntity
        // so the connect flow inside createRoleEntity (e.g. KVMHostCapacityExtension's
        // sync-host-capacity → HostCapacityUpdater.resolveServerUuidOrThrow) can find
        // the role mapping. Pre-generate the entity UUID and thread it through
        // CreateRoleEntityContext so HostVO.uuid == RoleVO.roleUuid post-flow.
        String preGenRoleUuid = Platform.getUuid();

        CreateRoleEntityContext ctx = new CreateRoleEntityContext()
            .setServerUuid(msg.getServerUuid())
            .setClusterUuid(msg.getClusterUuid())
            .setZoneUuid(server.getZoneUuid())
            .setManagementIp(server.getManagementIp())
            .setOobAddress(server.getOobAddress())
            .setOobPort(server.getOobPort())
            .setOobUsername(server.getOobUsername())
            .setOobPassword(server.getOobPassword())
            .setAccountUuid(msg.getSession() == null ? null : msg.getSession().getAccountUuid())
            .setPreGeneratedRoleUuid(preGenRoleUuid)
            .setRoleConfig(msg.getRoleConfig());

        final PhysicalServerRoleVO initialRole = attachRoleVO(
            msg.getServerUuid(),
            ServerRoleType.valueOf(msg.getRoleType()),
            preGenRoleUuid,
            provider.getSchedulingMode());
        final ServerRoleType roleType = ServerRoleType.valueOf(msg.getRoleType());
        final SchedulingMode mode = provider.getSchedulingMode();
        final APIAttachPhysicalServerRoleEvent evt = new APIAttachPhysicalServerRoleEvent(msg.getId());

        provider.createRoleEntity(ctx, new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String returnedUuid) {
                // Phase 1 placeholder providers return null — keep the pre-generated UUID.
                // Providers that ignore preGeneratedRoleUuid and produce a different entity UUID
                // would have already failed inside the connect flow (RoleVO points at preGenRoleUuid,
                // entity at returnedUuid → resolveServerUuidOrThrow miss). Defensive: if returned
                // UUID differs, rollback and rewrite RoleVO with the real entity UUID; the connect
                // flow has already completed, so capacity sync will catch up on next tick.
                PhysicalServerRoleVO role = initialRole;
                if (returnedUuid != null && !returnedUuid.equals(preGenRoleUuid)) {
                    dbf.remove(role);
                    role = attachRoleVO(msg.getServerUuid(), roleType, returnedUuid, mode);
                }

                // v5.5.18 U13 (AC-RS-20) / P1-4 fix: post-commit hook — RoleVO is durably written
                // above, so fire discovery best-effort. The hook impl swallows scheduler enqueue
                // failures internally per its contract; path-two contributors
                // (PhysicalServerPathTwoContributor / BareMetal2ChassisManagerImpl) call the same
                // single autowired bean.
                enqueueDiscoveryHook.enqueueDiscovery(msg.getServerUuid());

                evt.setInventory(PhysicalServerRoleInventory.valueOf(role));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode error) {
                dbf.remove(initialRole);
                evt.setError(error);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIDetachPhysicalServerRoleMsg msg) {
        final PhysicalServerRoleVO role = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, msg.getServerUuid())
                .eq(PhysicalServerRoleVO_.roleType, msg.getRoleType())
                .find();
        final APIDetachPhysicalServerRoleEvent evt = new APIDetachPhysicalServerRoleEvent(msg.getId());
        if (role == null) {
            bus.publish(evt);
            return;
        }

        PhysicalServerRoleProvider provider = roleProviders.get(msg.getRoleType());
        if (provider != null && !msg.isForce()) {
            RoleWorkloadStatus status = provider.getWorkloadStatus(
                msg.getServerUuid(), role.getRoleUuid());
            if (status != null && status.getDetachBlockReason() != null) {
                throw new OperationFailureException(operr(
                    "cannot detach role[type:%s] from server[uuid:%s]: %s",
                    msg.getRoleType(), msg.getServerUuid(), status.getDetachBlockReason()));
            }
        }

        if (provider == null) {
            dbf.remove(role);
            bus.publish(evt);
            return;
        }

        provider.deleteRoleEntity(role.getRoleUuid(), new Completion(msg) {
            @Override
            public void success() {
                dbf.remove(role);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode error) {
                evt.setError(error);
                bus.publish(evt);
            }
        });
    }

    // --- ProvisionNetwork handlers ---

    private void handle(APICreateProvisionNetworkMsg msg) {
        PhysicalServerProvisionNetworkVO vo = new PhysicalServerProvisionNetworkVO();
        vo.setUuid(msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setZoneUuid(msg.getZoneUuid());
        vo.setType(ProvisionNetworkType.valueOf(msg.getType()));
        vo.setDhcpInterface(emptyIfNull(msg.getDhcpInterface()));
        vo.setDhcpRangeStartIp(emptyIfNull(msg.getDhcpRangeStartIp()));
        vo.setDhcpRangeEndIp(emptyIfNull(msg.getDhcpRangeEndIp()));
        vo.setDhcpRangeNetmask(emptyIfNull(msg.getDhcpRangeNetmask()));
        vo.setDhcpRangeGateway(msg.getDhcpRangeGateway());
        vo.setState(ProvisionNetworkState.Enabled);
        vo = dbf.persistAndRefresh(vo);

        APICreateProvisionNetworkEvent evt = new APICreateProvisionNetworkEvent(msg.getId());
        evt.setInventory(PhysicalServerProvisionNetworkInventory.valueOf(vo));
        bus.publish(evt);
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private void handle(APIDeleteProvisionNetworkMsg msg) {
        dbf.removeByPrimaryKey(msg.getUuid(), PhysicalServerProvisionNetworkVO.class);
        APIDeleteProvisionNetworkEvent evt = new APIDeleteProvisionNetworkEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIUpdateProvisionNetworkMsg msg) {
        PhysicalServerProvisionNetworkVO vo = dbf.findByUuid(msg.getUuid(), PhysicalServerProvisionNetworkVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("ProvisionNetwork[uuid:%s] not found", msg.getUuid()));
        }

        boolean update = false;
        if (msg.getName() != null) { vo.setName(msg.getName()); update = true; }
        if (msg.getDescription() != null) { vo.setDescription(msg.getDescription()); update = true; }
        if (msg.getDhcpInterface() != null) { vo.setDhcpInterface(msg.getDhcpInterface()); update = true; }
        if (msg.getDhcpRangeStartIp() != null) { vo.setDhcpRangeStartIp(msg.getDhcpRangeStartIp()); update = true; }
        if (msg.getDhcpRangeEndIp() != null) { vo.setDhcpRangeEndIp(msg.getDhcpRangeEndIp()); update = true; }
        if (msg.getDhcpRangeNetmask() != null) { vo.setDhcpRangeNetmask(msg.getDhcpRangeNetmask()); update = true; }
        if (msg.getDhcpRangeGateway() != null) { vo.setDhcpRangeGateway(msg.getDhcpRangeGateway()); update = true; }

        if (update) {
            vo = dbf.updateAndRefresh(vo);
        }

        APIUpdateProvisionNetworkEvent evt = new APIUpdateProvisionNetworkEvent(msg.getId());
        evt.setInventory(PhysicalServerProvisionNetworkInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIAttachProvisionNetworkToClusterMsg msg) {
        PhysicalServerProvisionNetworkClusterRefVO ref = new PhysicalServerProvisionNetworkClusterRefVO();
        ref.setNetworkUuid(msg.getNetworkUuid());
        ref.setClusterUuid(msg.getClusterUuid());
        dbf.persist(ref);

        PhysicalServerProvisionNetworkVO networkVO = dbf.findByUuid(msg.getNetworkUuid(), PhysicalServerProvisionNetworkVO.class);
        if (networkVO == null) {
            throw new OperationFailureException(operr("ProvisionNetwork[uuid:%s] not found", msg.getNetworkUuid()));
        }

        APIAttachProvisionNetworkToClusterEvent evt = new APIAttachProvisionNetworkToClusterEvent(msg.getId());
        evt.setInventory(PhysicalServerProvisionNetworkInventory.valueOf(networkVO));
        bus.publish(evt);
    }

    private void handle(APIDetachProvisionNetworkFromClusterMsg msg) {
        SQL.New(PhysicalServerProvisionNetworkClusterRefVO.class)
            .eq(PhysicalServerProvisionNetworkClusterRefVO_.networkUuid, msg.getNetworkUuid())
            .eq(PhysicalServerProvisionNetworkClusterRefVO_.clusterUuid, msg.getClusterUuid())
            .delete();

        APIDetachProvisionNetworkFromClusterEvent evt = new APIDetachProvisionNetworkFromClusterEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIAttachProvisionNetworkToPoolMsg msg) {
        boolean exists = Q.New(PhysicalServerProvisionNetworkPoolRefVO.class)
            .eq(PhysicalServerProvisionNetworkPoolRefVO_.networkUuid, msg.getNetworkUuid())
            .eq(PhysicalServerProvisionNetworkPoolRefVO_.poolUuid, msg.getPoolUuid())
            .isExists();
        if (!exists) {
            PhysicalServerProvisionNetworkPoolRefVO ref = new PhysicalServerProvisionNetworkPoolRefVO();
            ref.setNetworkUuid(msg.getNetworkUuid());
            ref.setPoolUuid(msg.getPoolUuid());
            dbf.persist(ref);
        }

        PhysicalServerProvisionNetworkVO networkVO = dbf.findByUuid(
            msg.getNetworkUuid(), PhysicalServerProvisionNetworkVO.class);
        if (networkVO == null) {
            throw new OperationFailureException(
                operr("ProvisionNetwork[uuid:%s] not found", msg.getNetworkUuid()));
        }

        APIAttachProvisionNetworkToPoolEvent evt = new APIAttachProvisionNetworkToPoolEvent(msg.getId());
        evt.setInventory(PhysicalServerProvisionNetworkInventory.valueOf(networkVO));
        bus.publish(evt);
    }

    private void handle(APIDetachProvisionNetworkFromPoolMsg msg) {
        SQL.New("delete from PhysicalServerProvisionNetworkPoolRefVO r" +
                " where r.networkUuid = :networkUuid and r.poolUuid = :poolUuid")
            .param("networkUuid", msg.getNetworkUuid())
            .param("poolUuid", msg.getPoolUuid())
            .execute();

        APIDetachProvisionNetworkFromPoolEvent evt = new APIDetachProvisionNetworkFromPoolEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIProvisionPhysicalServerMsg msg) {
        APIProvisionPhysicalServerEvent evt = new APIProvisionPhysicalServerEvent(msg.getId());
        SubmitLongJobMsg smsg = new SubmitLongJobMsg();
        smsg.setName(msg.getLongJobName());
        smsg.setDescription(msg.getLongJobDescription());
        smsg.setJobName(APIProvisionPhysicalServerMsg.class.getSimpleName());
        smsg.setJobData(JSONObjectUtil.toJsonString(msg));
        smsg.setTargetResourceUuid(msg.getServerUuid());
        smsg.setAccountUuid(msg.getSession().getAccountUuid());
        bus.makeLocalServiceId(smsg, LongJobConstants.SERVICE_ID);
        bus.send(smsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply rly) {
                if (rly.isSuccess()) {
                    SubmitLongJobReply reply = rly.castReply();
                    evt.setInventory(reply.getInventory());
                } else {
                    evt.setError(rly.getError());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(APIPowerOnPhysicalServerMsg msg) {
        APIPowerOnPhysicalServerEvent evt = new APIPowerOnPhysicalServerEvent(msg.getId());
        dispatchPower(msg.getUuid(), evt, PhysicalServerPowerStatus.POWER_ON,
                "power-on", PowerAction.ON);
    }

    private void handle(APIPowerOffPhysicalServerMsg msg) {
        APIPowerOffPhysicalServerEvent evt = new APIPowerOffPhysicalServerEvent(msg.getId());
        dispatchPower(msg.getUuid(), evt, PhysicalServerPowerStatus.POWER_OFF,
                "power-off", PowerAction.OFF);
    }

    private void handle(APIPowerResetPhysicalServerMsg msg) {
        APIPowerResetPhysicalServerEvent evt = new APIPowerResetPhysicalServerEvent(msg.getId());
        dispatchPower(msg.getUuid(), evt, PhysicalServerPowerStatus.POWER_ON,
                "power-reset", PowerAction.RESET);
    }

    private enum PowerAction { ON, OFF, RESET }

    private void dispatchPower(String serverUuid,
                               APIEvent evt,
                               PhysicalServerPowerStatus postOpStatus,
                               String opLabel,
                               PowerAction action) {
        PhysicalServerVO server = dbf.findByUuid(serverUuid, PhysicalServerVO.class);
        if (server == null) {
            evt.setError(operr("PhysicalServer[uuid:%s] not found", serverUuid));
            bus.publish(evt);
            return;
        }

        List<PhysicalServerRoleVO> roles = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, serverUuid)
                .list();

        ErrorCode roleGate = checkRolePowerGate(serverUuid, roles, opLabel, action);
        if (roleGate != null) {
            evt.setError(roleGate);
            bus.publish(evt);
            return;
        }

        Completion completion = new Completion(null) {
            @Override
            public void success() {
                PhysicalServerVO reloaded = dbf.findByUuid(serverUuid, PhysicalServerVO.class);
                if (reloaded != null) {
                    reloaded.setPowerStatus(postOpStatus);
                    PhysicalServerVO updated = dbf.updateAndRefresh(reloaded);
                    setEventInventory(evt, PhysicalServerInventory.valueOf(updated));
                }
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        };

        if (ipmiPowerExecutor.hasOobCredentials(server)) {
            switch (action) {
                case ON:
                    ipmiPowerExecutor.powerOn(server, completion);
                    break;
                case OFF:
                    ipmiPowerExecutor.powerOff(server, completion);
                    break;
                case RESET:
                    ipmiPowerExecutor.powerReset(server, completion);
                    break;
                default:
                    throw new CloudRuntimeException("unknown power action: " + action);
            }
            return;
        }

        if (roles.isEmpty()) {
            evt.setError(operr("OOB credentials not configured for PhysicalServer[uuid:%s]", serverUuid));
            bus.publish(evt);
            return;
        }

        PhysicalServerRoleVO role = choosePowerFallbackRole(roles);
        PhysicalServerRoleProvider provider = roleProviders.get(role.getRoleType());

        switch (action) {
            case ON:
                provider.powerOn(serverUuid, role.getRoleUuid(), completion);
                break;
            case OFF:
                provider.powerOff(serverUuid, role.getRoleUuid(), completion);
                break;
            case RESET:
                provider.powerReset(serverUuid, role.getRoleUuid(), completion);
                break;
            default:
                throw new CloudRuntimeException("unreachable: unknown PowerAction " + action);
        }
    }

    private PhysicalServerRoleVO choosePowerFallbackRole(List<PhysicalServerRoleVO> roles) {
        return roles.stream()
                .max(java.util.Comparator.comparingInt(r -> {
                    PhysicalServerRoleProvider p = roleProviders.get(r.getRoleType());
                    return p == null ? 0 : p.getPowerFallbackPriority();
                }))
                .orElse(roles.get(0));
    }

    private ErrorCode checkRolePowerGate(String serverUuid, List<PhysicalServerRoleVO> roles,
                                         String opLabel, PowerAction action) {
        if (action == PowerAction.ON) {
            return null;
        }

        for (PhysicalServerRoleVO role : roles) {
            PhysicalServerRoleProvider provider = roleProviders.get(role.getRoleType());
            if (provider == null) {
                return operr(
                        "no PhysicalServerRoleProvider registered for roleType[%s] on server[uuid:%s]; cannot %s",
                        role.getRoleType(), serverUuid, opLabel);
            }

            RoleWorkloadStatus status = provider.getWorkloadStatus(serverUuid, role.getRoleUuid());
            if (status == null) {
                continue;
            }

            String reason = action == PowerAction.OFF
                    ? status.getPowerOffBlockReason()
                    : status.getPowerResetBlockReason();
            if (reason != null) {
                return operr("PhysicalServer[uuid:%s] cannot %s because role[type:%s, uuid:%s] blocks it: %s",
                        serverUuid, opLabel, role.getRoleType(), role.getRoleUuid(), reason);
            }
        }

        return null;
    }

    /**
     * Reflectively invokes the matching {@code setInventory} on the power event. Each
     * {@code APIPower*PhysicalServerEvent} has its own {@code setInventory(PhysicalServerInventory)}
     * but they don't share a common interface, and we don't want to import 3 specific events into
     * the generic dispatch helper.
     */
    private void setEventInventory(APIEvent evt, PhysicalServerInventory inv) {
        if (evt instanceof APIPowerOnPhysicalServerEvent) {
            ((APIPowerOnPhysicalServerEvent) evt).setInventory(inv);
        } else if (evt instanceof APIPowerOffPhysicalServerEvent) {
            ((APIPowerOffPhysicalServerEvent) evt).setInventory(inv);
        } else if (evt instanceof APIPowerResetPhysicalServerEvent) {
            ((APIPowerResetPhysicalServerEvent) evt).setInventory(inv);
        } else if (evt instanceof APIDiscoverPhysicalServerHardwareEvent) {
            ((APIDiscoverPhysicalServerHardwareEvent) evt).setInventory(inv);
        }
    }

    // --- Discover handler (Phase 3 fix-plan U3 — AC-CB-18) ---
    //
    // APIDiscoverPhysicalServerHardwareMsg.responseClass = APIDiscoverPhysicalServerHardwareEvent
    // (which carries an inventory field) — Event semantics imply sync return. The hardware
    // service's discoverHardware(serverUuid) is itself synchronous and persists discovered fields
    // into the PS row before returning. So the handler is: load PS, call service, reload, build
    // inventory, publish event. Async batch-discovery is still available via
    // hardwareDiscoveryScheduler.enqueueDiscovery(uuid) for orphan PS at MN boot, but the API
    // path uses the sync flavour.

    private void handle(APIDiscoverPhysicalServerHardwareMsg msg) {
        PhysicalServerVO server = dbf.findByUuid(msg.getUuid(), PhysicalServerVO.class);
        if (server == null) {
            throw new OperationFailureException(operr("PhysicalServer[uuid:%s] not found", msg.getUuid()));
        }

        UnifiedHardwareInfo info = hardwareService.discoverHardware(msg.getUuid());
        if (info == null) {
            logger.warn(String.format(
                    "discoverHardware returned null for server[uuid:%s]; treating as no-op",
                    msg.getUuid()));
        }

        PhysicalServerVO reloaded = dbf.findByUuid(msg.getUuid(), PhysicalServerVO.class);
        APIDiscoverPhysicalServerHardwareEvent evt = new APIDiscoverPhysicalServerHardwareEvent(msg.getId());
        evt.setInventory(PhysicalServerInventory.valueOf(reloaded != null ? reloaded : server));
        bus.publish(evt);
    }
}
