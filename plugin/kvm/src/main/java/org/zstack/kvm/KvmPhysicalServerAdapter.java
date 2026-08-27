package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.compute.host.PostHostConnectExtensionPoint;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.host.GetHostNumaTopologyMsg;
import org.zstack.header.host.GetHostNumaTopologyReply;
import org.zstack.header.host.HostAO_;
import org.zstack.header.host.HostAfterConnectedExtensionPoint;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostDeleteExtensionPoint;
import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostNUMANode;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.physicalserver.PhysicalServerResourceControlAdapter;
import org.zstack.header.physicalserver.PhysicalServerResourceApplicationMode;
import org.zstack.header.physicalserver.PhysicalServerResourceConsumerState;
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode;
import org.zstack.header.physicalserver.PhysicalServerCpuSet;
import org.zstack.header.physicalserver.PhysicalServerCpuTopology;
import org.zstack.header.physicalserver.PhysicalServerIdentitySpec;
import org.zstack.header.physicalserver.PhysicalServerManager;
import org.zstack.header.physicalserver.ManagedServiceResourceUsage;
import org.zstack.header.physicalserver.PhysicalServerNumaNode;
import org.zstack.header.physicalserver.ResourceControlCommand;
import org.zstack.header.physicalserver.ResourceControlResponse;
import org.zstack.header.physicalserver.ResourceConsumerHandle;
import org.zstack.header.physicalserver.ResourceControlResult;
import org.zstack.header.physicalserver.RoleServiceManifest;
import org.zstack.header.tag.AbstractSystemTagLifeCycleListener;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.zstack.core.Platform.operr;

public class KvmPhysicalServerAdapter implements
        PhysicalServerResourceControlAdapter,
        PostHostConnectExtensionPoint,
        HostAfterConnectedExtensionPoint,
        HostDeleteExtensionPoint,
        ManagementNodeReadyExtensionPoint,
        Component {
    public static final String ROLE_TYPE = "COMPUTE";
    public static final String APPLY_RESOURCE_CONTROL_PATH = "/host/resourcecontrol/apply";
    public static final String GET_MANAGED_SERVICE_USAGE_PATH =
            "/host/resourcecontrol/services";
    public static final String RESTART_MANAGED_SERVICES_PATH =
            "/host/resourcecontrol/restart";
    public static final String ROLE_SERVICE_MANIFEST_PATH =
            "physical-server-roles/compute.yaml";
    private static final String ERROR_CODE = "ORG_ZSTACK_KVM_10000";
    private static final int HOST_LINK_UPDATE_BATCH_SIZE = 500;
    private static final CLogger logger = Utils.getLogger(KvmPhysicalServerAdapter.class);
    private static final RoleServiceManifest ROLE_SERVICES =
            RoleServiceManifest.load(
                    ROLE_SERVICE_MANIFEST_PATH,
                    ROLE_TYPE,
                    PhysicalServerResourceApplicationMode.RESOURCE_HANDLES);
    private final AtomicReference<Map<String, HostRelation>> hostRelations =
            new AtomicReference<>(Collections.emptyMap());

    @Autowired(required = false)
    private PhysicalServerManager physicalServerManager;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private KVMHostCapacityExtension capacityExtension;

    @Override
    public String getRoleType() {
        return ROLE_TYPE;
    }

    @Override
    public PhysicalServerResourceIsolationMode getIsolationMode() {
        return PhysicalServerResourceIsolationMode.SHARED;
    }

    @Override
    public PhysicalServerResourceApplicationMode getApplicationMode() {
        return PhysicalServerResourceApplicationMode.RESOURCE_HANDLES;
    }

    @Override
    public String getDefaultCpuSet(
            PhysicalServerCpuTopology topology,
            Set<Integer> allocatedExclusiveCpus) {
        return PhysicalServerCpuSet.firstAvailableExcludingCpuZeroCore(
                topology,
                allocatedExclusiveCpus,
                ROLE_SERVICES.getDefaultCpuCount());
    }

    @Override
    public Set<String> getAssociatedServerUuids() {
        if (hostRelations.get().isEmpty()) {
            refreshHostRelations(Collections.emptySet());
        }
        return new LinkedHashSet<>(hostRelations.get().keySet());
    }

    @Override
    public void refreshAssociations() {
        backfill(false);
        refreshHostRelations(Collections.emptySet());
    }

    @Override
    public void refreshAssociations(Collection<String> serverUuids) {
        if (serverUuids == null || serverUuids.isEmpty()) {
            refreshAssociations();
            return;
        }
        refreshHostRelations(serverUuids);
    }

    @Override
    public PhysicalServerResourceConsumerState getState(String serverUuid) {
        HostRelation host = hostRelation(serverUuid);
        if (host == null) {
            return PhysicalServerResourceConsumerState.MISSING;
        }
        return host.status == HostStatus.Connected
                ? PhysicalServerResourceConsumerState.AVAILABLE
                : PhysicalServerResourceConsumerState.UNAVAILABLE;
    }

    @Override
    public Map<String, PhysicalServerResourceConsumerState> getStates(
            Collection<String> serverUuids) {
        if (serverUuids == null || serverUuids.isEmpty()) {
            return Collections.emptyMap();
        }
        refreshMissingHostRelations(serverUuids);
        Map<String, PhysicalServerResourceConsumerState> result = new HashMap<>();
        for (String serverUuid : serverUuids) {
            HostRelation relation = hostRelations.get().get(serverUuid);
            result.put(serverUuid, relation == null
                    ? PhysicalServerResourceConsumerState.MISSING
                    : relation.status == HostStatus.Connected
                    ? PhysicalServerResourceConsumerState.AVAILABLE
                    : PhysicalServerResourceConsumerState.UNAVAILABLE);
        }
        return result;
    }

    @Override
    public void refreshCapacity(String serverUuid) {
        String hostUuid = hostUuid(serverUuid, null);
        if (hostUuid == null) {
            return;
        }
        capacityExtension.reportCapacity(hostUuid, new Completion(null) {
            @Override
            public void success() {
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format(
                        "failed to refresh capacity for host[uuid:%s] after " +
                                "physical server resource assignment changed: %s",
                        hostUuid, errorCode));
            }
        });
    }

    @Override
    public void collectTopology(
            String serverUuid,
            ReturnValueCompletion<PhysicalServerCpuTopology> completion) {
        String hostUuid = hostUuid(serverUuid, null);
        if (hostUuid == null) {
            completion.fail(operr(ERROR_CODE,
                    "HOST_RELATION_MISSING: physical server[uuid:%s] has no KVM host",
                    serverUuid));
            return;
        }

        GetHostNumaTopologyMsg msg = new GetHostNumaTopologyMsg();
        msg.setHostUuid(hostUuid);
        msg.setTimeout(TimeUnit.MINUTES.toMillis(5));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                completeTopology(reply.castReply(), completion);
            }
        });
    }

    private void completeTopology(
            GetHostNumaTopologyReply reply,
            ReturnValueCompletion<PhysicalServerCpuTopology> completion) {
        try {
            completion.success(PhysicalServerCpuTopology.from(
                    neutralTopology(reply.getNuma())));
        } catch (RuntimeException error) {
            completion.fail(operr(ERROR_CODE,
                    "CPU_TOPOLOGY_FACT_INVALID: %s", error.getMessage()));
        }
    }

    @Override
    public void apply(
            String serverUuid,
            String consumerUuid,
            ResourceControlCommand command,
            ReturnValueCompletion<ResourceControlResponse> completion) {
        String hostUuid = hostUuid(serverUuid, consumerUuid);
        if (hostUuid == null) {
            completion.fail(operr(ERROR_CODE,
                    "HOST_RELATION_MISSING: physical server[uuid:%s] has no matching KVM host",
                    serverUuid));
            return;
        }

        ResourceControlAgentCommand agentCommand = new ResourceControlAgentCommand();
        agentCommand.setRoleType(command.getRoleType());
        agentCommand.setOperation(command.getOperation());
        agentCommand.setCpuSet(command.getCpuSet());
        agentCommand.setMemory(command.getMemory());
        agentCommand.setSliceName(ROLE_SERVICES.getSliceName());
        agentCommand.setHandles(handles(hostUuid, command.isIncludeAuxiliaryServices()));

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setPath(APPLY_RESOURCE_CONTROL_PATH);
        msg.setHostUuid(hostUuid);
        msg.setCommand(agentCommand);
        msg.setTimeout(TimeUnit.MINUTES.toMillis(5));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                ErrorableValue<ResourceControlAgentResponse> result =
                        KVMHostAsyncHttpCallReply.unwrap(
                                reply, ResourceControlAgentResponse.class);
                if (!result.isSuccess()) {
                    completion.fail(result.error);
                    return;
                }
                completion.success(result.result.toInventory());
            }
        });
    }

    @Override
    public void collectManagedServiceUsage(
            String serverUuid,
            boolean includeAuxiliaryServices,
            ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion) {
        String hostUuid = hostUuid(serverUuid, null);
        if (hostUuid == null) {
            completion.success(ROLE_SERVICES.managedServiceUsages(
                    includeAuxiliaryServices, "UNAVAILABLE"));
            return;
        }
        ManagedServiceAgentCommand command = new ManagedServiceAgentCommand();
        command.setRoleType(ROLE_TYPE);
        command.setSliceName(ROLE_SERVICES.getSliceName());
        command.setHandles(handles(hostUuid, includeAuxiliaryServices));
        KVMHostAsyncHttpCallMsg msg = managedServiceCall(
                hostUuid, GET_MANAGED_SERVICE_USAGE_PATH, command);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                ErrorableValue<ManagedServiceUsageAgentResponse> result =
                        KVMHostAsyncHttpCallReply.unwrap(
                                reply, ManagedServiceUsageAgentResponse.class);
                if (!result.isSuccess()) {
                    logger.warn(String.format(
                            "failed to query managed services on host[uuid:%s]: %s",
                            hostUuid, result.error));
                    completion.success(ROLE_SERVICES.managedServiceUsages(
                            includeAuxiliaryServices, "UNAVAILABLE"));
                    return;
                }
                List<ManagedServiceResourceUsage> services =
                        result.result.getServices();
                if (services == null) {
                    completion.success(ROLE_SERVICES.managedServiceUsages(
                            includeAuxiliaryServices, "UNAVAILABLE"));
                    return;
                }
                for (ManagedServiceResourceUsage usage : services) {
                    usage.setRoleType(ROLE_TYPE);
                }
                completion.success(services);
            }
        });
    }

    @Override
    public void restartManagedServices(
            String serverUuid,
            boolean includeAuxiliaryServices,
            Collection<String> serviceNames,
            Completion completion) {
        String hostUuid = hostUuid(serverUuid, null);
        if (hostUuid == null) {
            completion.fail(operr(ERROR_CODE,
                    "HOST_RELATION_MISSING: physical server[uuid:%s] has no KVM host",
                    serverUuid));
            return;
        }
        ManagedServiceAgentCommand command = new ManagedServiceAgentCommand();
        command.setRoleType(ROLE_TYPE);
        command.setSliceName(ROLE_SERVICES.getSliceName());
        try {
            command.setHandles(ROLE_SERVICES.handlesByServiceNames(
                    serviceNames,
                    String.format("host-agent:%s", hostUuid),
                    String.format("host-agent:%s", hostUuid),
                    includeAuxiliaryServices,
                    Collections.emptyMap()));
        } catch (RuntimeException error) {
            completion.fail(operr(ERROR_CODE, "%s", error.getMessage()));
            return;
        }
        KVMHostAsyncHttpCallMsg msg = managedServiceCall(
                hostUuid, RESTART_MANAGED_SERVICES_PATH, command);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                ErrorableValue<KVMAgentCommands.AgentResponse> result =
                        KVMHostAsyncHttpCallReply.unwrap(
                                reply, KVMAgentCommands.AgentResponse.class);
                if (!result.isSuccess()) {
                    completion.fail(result.error);
                    return;
                }
                completion.success();
            }
        });
    }

    private KVMHostAsyncHttpCallMsg managedServiceCall(
            String hostUuid, String path, ManagedServiceAgentCommand command) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setPath(path);
        msg.setHostUuid(hostUuid);
        msg.setCommand(command);
        msg.setTimeout(TimeUnit.MINUTES.toMillis(5));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        return msg;
    }

    @Override
    public Flow createPostHostConnectFlow(HostInventory host) {
        return new NoRollbackFlow() {
            String __name__ = "associate-kvm-host-with-physical-server";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                associate(host);
                trigger.next();
            }
        };
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        if (physicalServerManager == null) {
            return;
        }
        String serverUuid = Q.New(HostVO.class)
                .select(HostVO_.serverUuid)
                .eq(HostVO_.uuid, host.getUuid())
                .findValue();
        if (serverUuid != null) {
            physicalServerManager.reconcile(serverUuid, true);
        }
    }

    @Override
    public void preDeleteHost(HostInventory inventory) throws HostException {
    }

    @Override
    public void beforeDeleteHost(HostInventory inventory) {
        if (inventory.getServerUuid() == null) {
            return;
        }
        String serverUuid = inventory.getServerUuid();
        org.zstack.core.db.SQL.New(HostEO.class)
                .eq(HostAO_.uuid, inventory.getUuid())
                .eq(HostAO_.serverUuid, serverUuid)
                .set(HostAO_.serverUuid, null)
                .update();
        removeHostRelation(serverUuid);
    }

    @Override
    public void afterDeleteHost(HostInventory inventory) {
        if (inventory.getServerUuid() == null || physicalServerManager == null) {
            return;
        }
        removeHostRelation(inventory.getServerUuid());
        physicalServerManager.reconcile(inventory.getServerUuid(), true);
    }

    @Override
    public void managementNodeReady() {
        backfill(true);
    }

    @Override
    public boolean start() {
        HostSystemTags.SYSTEM_SERIAL_NUMBER.installLifeCycleListener(
                new AbstractSystemTagLifeCycleListener() {
                    @Override
                    public void tagCreated(SystemTagInventory tag) {
                        backfill(Collections.singleton(tag.getResourceUuid()), true);
                    }

                    @Override
                    public void tagUpdated(
                            SystemTagInventory old, SystemTagInventory newTag) {
                        backfill(Collections.singleton(newTag.getResourceUuid()), true);
                    }
                });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Transactional
    public void associate(HostInventory host) {
        if (physicalServerManager == null
                || !KVMConstant.KVM_HYPERVISOR_TYPE.equals(host.getHypervisorType())) {
            return;
        }
        String current = Q.New(HostVO.class)
                .select(HostVO_.serverUuid)
                .eq(HostVO_.uuid, host.getUuid())
                .findValue();
        if (current != null) {
            physicalServerManager.reconcile(current, true);
            return;
        }

        String serialNumber = Platform.normalizeMachineSerialNumber(
                HostSystemTags.SYSTEM_SERIAL_NUMBER.getTokenByResourceUuid(
                        host.getUuid(), HostSystemTags.SYSTEM_SERIAL_NUMBER_TOKEN));
        if (serialNumber == null) {
            logger.warn(String.format(
                    "cannot associate host[uuid:%s] with a physical server because " +
                            "its machine serial number is unavailable", host.getUuid()));
            return;
        }
        String zoneUuid = host.getZoneUuid() == null
                ? Q.New(HostVO.class).select(HostVO_.zoneUuid)
                .eq(HostVO_.uuid, host.getUuid()).findValue()
                : host.getZoneUuid();
        String serverUuid = physicalServerManager.resolveIdentities(
                Collections.singletonList(
                        new PhysicalServerIdentitySpec(serialNumber, zoneUuid)))
                .get(serialNumber);
        if (serverUuid == null) {
            return;
        }
        clearDeletedHostLink(serverUuid);
        if (linkedHost(serverUuid, host.getUuid()) != null) {
            return;
        }

        Query update = dbf.getEntityManager().createNativeQuery(
                "UPDATE IGNORE HostEO SET serverUuid = :serverUuid " +
                        "WHERE uuid = :hostUuid AND serverUuid IS NULL");
        update.setParameter("serverUuid", serverUuid);
        update.setParameter("hostUuid", host.getUuid());
        update.executeUpdate();
        current = Q.New(HostVO.class)
                .select(HostVO_.serverUuid)
                .eq(HostVO_.uuid, host.getUuid())
                .findValue();
        if (!serverUuid.equals(current)) {
            logger.warn(String.format(
                    "cannot associate host[uuid:%s] with physical server[uuid:%s] " +
                            "because the host is already associated elsewhere",
                    host.getUuid(), serverUuid));
            return;
        }
        physicalServerManager.reconcile(serverUuid, true);
    }

    @Transactional
    private void backfill(boolean requestReconcile) {
        backfill(Collections.emptySet(), requestReconcile);
    }

    @Transactional
    private void backfill(
            Collection<String> hostUuids, boolean requestReconcile) {
        if (physicalServerManager == null) {
            return;
        }
        Q hostQuery = Q.New(HostVO.class)
                .select(HostVO_.uuid, HostVO_.zoneUuid)
                .eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE)
                .isNull(HostVO_.serverUuid);
        if (hostUuids != null && !hostUuids.isEmpty()) {
            hostQuery.in(HostVO_.uuid, hostUuids);
        }
        List<Tuple> hostTuples = hostQuery.listTuple();
        if (hostTuples.isEmpty()) {
            return;
        }
        Map<String, String> zonesByHost = new LinkedHashMap<>();
        for (Tuple tuple : hostTuples) {
            zonesByHost.put(tuple.get(0, String.class), tuple.get(1, String.class));
        }
        Map<String, Set<String>> serialsByHost = serialsByHost(zonesByHost.keySet());
        Map<String, List<HostCandidate>> candidatesBySerial = new LinkedHashMap<>();
        for (Map.Entry<String, String> host : zonesByHost.entrySet()) {
            Set<String> serials = serialsByHost.get(host.getKey());
            if (serials == null || serials.size() != 1) {
                continue;
            }
            candidatesBySerial
                    .computeIfAbsent(serials.iterator().next(), ignored -> new ArrayList<>())
                    .add(new HostCandidate(host.getKey(), host.getValue()));
        }

        Map<String, HostCandidate> candidates = new LinkedHashMap<>();
        List<PhysicalServerIdentitySpec> identities = new ArrayList<>();
        for (Map.Entry<String, List<HostCandidate>> candidate : candidatesBySerial.entrySet()) {
            if (candidate.getValue().size() != 1) {
                logger.warn(String.format(
                        "cannot backfill host physical server association for serialNumber[%s] " +
                                "because it matches multiple hosts",
                        candidate.getKey()));
                continue;
            }
            HostCandidate host = candidate.getValue().get(0);
            candidates.put(candidate.getKey(), host);
            identities.add(new PhysicalServerIdentitySpec(candidate.getKey(), host.zoneUuid));
        }
        Map<String, String> resolved = physicalServerManager.resolveIdentities(identities);
        clearDeletedHostLinks(resolved.values());
        Set<String> used = new HashSet<>(Q.New(HostVO.class)
                .select(HostVO_.serverUuid)
                .notNull(HostVO_.serverUuid)
                .listValues());
        Map<String, String> links = new LinkedHashMap<>();
        for (Map.Entry<String, HostCandidate> candidate : candidates.entrySet()) {
            String serverUuid = resolved.get(candidate.getKey());
            if (serverUuid != null && used.add(serverUuid)) {
                links.put(candidate.getValue().hostUuid, serverUuid);
            }
        }
        updateHostLinks(links);

        List<Tuple> linked = links.isEmpty() ? Collections.emptyList() : Q.New(HostVO.class)
                .select(HostVO_.uuid, HostVO_.serverUuid)
                .in(HostVO_.uuid, links.keySet())
                .notNull(HostVO_.serverUuid)
                .listTuple();
        Set<String> linkedServers = new LinkedHashSet<>();
        for (Tuple tuple : linked) {
            String serverUuid = tuple.get(1, String.class);
            linkedServers.add(serverUuid);
        }
        if (requestReconcile && !linkedServers.isEmpty()) {
            for (String serverUuid : linkedServers) {
                physicalServerManager.reconcile(serverUuid, true);
            }
        }
    }

    private Map<String, Set<String>> serialsByHost(Collection<String> hostUuids) {
        List<Tuple> tagTuples = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.resourceUuid, SystemTagVO_.tag)
                .in(SystemTagVO_.resourceUuid, hostUuids)
                .eq(SystemTagVO_.resourceType, HostVO.class.getSimpleName())
                .like(SystemTagVO_.tag, TagUtils.tagPatternToSqlPattern(
                        HostSystemTags.SYSTEM_SERIAL_NUMBER.getTagFormat()))
                .listTuple();
        Map<String, Set<String>> result = new HashMap<>();
        for (Tuple tuple : tagTuples) {
            String hostUuid = tuple.get(0, String.class);
            String serialNumber = Platform.normalizeMachineSerialNumber(
                    HostSystemTags.SYSTEM_SERIAL_NUMBER.getTokenByTag(
                            tuple.get(1, String.class),
                            HostSystemTags.SYSTEM_SERIAL_NUMBER_TOKEN));
            if (serialNumber != null) {
                result.computeIfAbsent(hostUuid, ignored -> new LinkedHashSet<>())
                        .add(serialNumber);
            }
        }
        return result;
    }

    private void updateHostLinks(Map<String, String> links) {
        if (links.isEmpty()) {
            return;
        }
        List<Map.Entry<String, String>> entries =
                new ArrayList<>(links.entrySet());
        for (int start = 0; start < entries.size();
                start += HOST_LINK_UPDATE_BATCH_SIZE) {
            updateHostLinks(entries.subList(
                    start,
                    Math.min(start + HOST_LINK_UPDATE_BATCH_SIZE, entries.size())));
        }
    }

    private void updateHostLinks(List<Map.Entry<String, String>> links) {
        StringBuilder sql = new StringBuilder(
                "UPDATE IGNORE HostEO SET serverUuid = CASE uuid");
        int index = 0;
        for (Map.Entry<String, String> ignored : links) {
            sql.append(" WHEN :host").append(index)
                    .append(" THEN :server").append(index);
            index++;
        }
        sql.append(" ELSE serverUuid END WHERE serverUuid IS NULL AND uuid IN (");
        for (int i = 0; i < links.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append(":host").append(i);
        }
        sql.append(')');
        Query query = dbf.getEntityManager().createNativeQuery(sql.toString());
        index = 0;
        for (Map.Entry<String, String> link : links) {
            query.setParameter("host" + index, link.getKey());
            query.setParameter("server" + index, link.getValue());
            index++;
        }
        query.executeUpdate();
    }

    private void clearDeletedHostLinks(Collection<String> serverUuids) {
        Set<String> targets = new LinkedHashSet<>(serverUuids);
        targets.remove(null);
        if (targets.isEmpty()) {
            return;
        }
        StringBuilder sql = new StringBuilder(
                "UPDATE HostEO SET serverUuid = NULL " +
                        "WHERE deleted IS NOT NULL AND serverUuid IN (");
        for (int i = 0; i < targets.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append(":server").append(i);
        }
        sql.append(')');
        Query query = dbf.getEntityManager().createNativeQuery(sql.toString());
        int index = 0;
        for (String serverUuid : targets) {
            query.setParameter("server" + index, serverUuid);
            index++;
        }
        query.executeUpdate();
    }

    private void clearDeletedHostLink(String serverUuid) {
        Query query = dbf.getEntityManager().createNativeQuery(
                "UPDATE HostEO SET serverUuid = NULL " +
                        "WHERE deleted IS NOT NULL AND serverUuid = :serverUuid");
        query.setParameter("serverUuid", serverUuid);
        query.executeUpdate();
    }

    private String linkedHost(String serverUuid, String excludedHostUuid) {
        return Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .eq(HostVO_.serverUuid, serverUuid)
                .notEq(HostVO_.uuid, excludedHostUuid)
                .findValue();
    }

    private String hostUuid(String serverUuid, String consumerUuid) {
        HostRelation relation = hostRelation(serverUuid);
        if (relation == null) {
            return null;
        }
        return consumerUuid == null || consumerUuid.equals(relation.hostUuid)
                ? relation.hostUuid : null;
    }

    private HostRelation hostRelation(String serverUuid) {
        HostRelation relation = hostRelations.get().get(serverUuid);
        if (relation != null) {
            return relation;
        }
        refreshHostRelations(Collections.singleton(serverUuid));
        return hostRelations.get().get(serverUuid);
    }

    private void refreshMissingHostRelations(Collection<String> serverUuids) {
        Set<String> missing = new LinkedHashSet<>(serverUuids);
        missing.removeAll(hostRelations.get().keySet());
        if (!missing.isEmpty()) {
            refreshHostRelations(missing);
        }
    }

    private void refreshHostRelations(Collection<String> serverUuids) {
        Q query = Q.New(HostVO.class)
                .select(HostVO_.serverUuid, HostVO_.uuid, HostVO_.status)
                .eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE)
                .notNull(HostVO_.serverUuid);
        boolean partial = serverUuids != null && !serverUuids.isEmpty();
        if (partial) {
            query.in(HostVO_.serverUuid, serverUuids);
        }
        Map<String, HostRelation> loaded = new HashMap<>();
        for (Tuple host : (List<Tuple>) query.listTuple()) {
            loaded.put(
                    host.get(0, String.class),
                    new HostRelation(
                            host.get(1, String.class),
                            host.get(2, HostStatus.class)));
        }
        if (!partial) {
            hostRelations.set(Collections.unmodifiableMap(loaded));
            return;
        }
        while (true) {
            Map<String, HostRelation> current = hostRelations.get();
            Map<String, HostRelation> replacement = new HashMap<>(current);
            for (String serverUuid : serverUuids) {
                replacement.remove(serverUuid);
            }
            replacement.putAll(loaded);
            if (hostRelations.compareAndSet(
                    current, Collections.unmodifiableMap(replacement))) {
                return;
            }
        }
    }

    private void removeHostRelation(String serverUuid) {
        while (true) {
            Map<String, HostRelation> current = hostRelations.get();
            if (!current.containsKey(serverUuid)) {
                return;
            }
            Map<String, HostRelation> replacement = new HashMap<>(current);
            replacement.remove(serverUuid);
            if (hostRelations.compareAndSet(
                    current, Collections.unmodifiableMap(replacement))) {
                return;
            }
        }
    }

    private static class HostRelation {
        private final String hostUuid;
        private final HostStatus status;

        private HostRelation(String hostUuid, HostStatus status) {
            this.hostUuid = hostUuid;
            this.status = status;
        }
    }

    private Map<String, PhysicalServerNumaNode> neutralTopology(
            Map<String, HostNUMANode> topology) {
        Map<String, PhysicalServerNumaNode> result = new LinkedHashMap<>();
        if (topology == null) {
            return result;
        }
        for (Map.Entry<String, HostNUMANode> entry : topology.entrySet()) {
            PhysicalServerNumaNode node = new PhysicalServerNumaNode();
            node.setNodeId(entry.getKey());
            node.setOnlineCpus(entry.getValue().getOnlineCpus());
            node.setCoreGroups(entry.getValue().getCoreGroups());
            result.put(entry.getKey(), node);
        }
        return result;
    }

    private List<ResourceConsumerHandle> handles(
            String hostUuid, boolean includeAuxiliaryServices) {
        String consumerKey = String.format("host-agent:%s", hostUuid);
        return ROLE_SERVICES.handles(
                consumerKey, consumerKey, includeAuxiliaryServices);
    }

    public static class ResourceControlAgentCommand extends KVMAgentCommands.AgentCommand {
        private String roleType;
        private String operation;
        private String cpuSet;
        private Long memory;
        private String sliceName;
        private List<ResourceConsumerHandle> handles = new ArrayList<>();

        public String getRoleType() {
            return roleType;
        }

        public void setRoleType(String roleType) {
            this.roleType = roleType;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public String getCpuSet() {
            return cpuSet;
        }

        public void setCpuSet(String cpuSet) {
            this.cpuSet = cpuSet;
        }

        public Long getMemory() {
            return memory;
        }

        public void setMemory(Long memory) {
            this.memory = memory;
        }

        public String getSliceName() {
            return sliceName;
        }

        public void setSliceName(String sliceName) {
            this.sliceName = sliceName;
        }

        public List<ResourceConsumerHandle> getHandles() {
            return handles;
        }

        public void setHandles(List<ResourceConsumerHandle> handles) {
            this.handles = handles;
        }
    }

    public static class ManagedServiceAgentCommand extends KVMAgentCommands.AgentCommand {
        private String roleType;
        private String sliceName;
        private List<ResourceConsumerHandle> handles = new ArrayList<>();

        public String getRoleType() {
            return roleType;
        }

        public void setRoleType(String roleType) {
            this.roleType = roleType;
        }

        public String getSliceName() {
            return sliceName;
        }

        public void setSliceName(String sliceName) {
            this.sliceName = sliceName;
        }

        public List<ResourceConsumerHandle> getHandles() {
            return handles;
        }

        public void setHandles(List<ResourceConsumerHandle> handles) {
            this.handles = handles;
        }
    }

    public static class ManagedServiceUsageAgentResponse extends KVMAgentCommands.AgentResponse {
        private List<ManagedServiceResourceUsage> services = new ArrayList<>();

        public List<ManagedServiceResourceUsage> getServices() {
            return services;
        }

        public void setServices(List<ManagedServiceResourceUsage> services) {
            this.services = services;
        }
    }

    public static class ResourceControlAgentResponse extends KVMAgentCommands.AgentResponse {
        private String cpuSet;
        private Long memory;
        private Integer coveredServiceCount;
        private Integer expectedServiceCount;
        private List<ResourceControlResult> results = new ArrayList<>();

        public String getCpuSet() {
            return cpuSet;
        }

        public void setCpuSet(String cpuSet) {
            this.cpuSet = cpuSet;
        }

        public Long getMemory() {
            return memory;
        }

        public void setMemory(Long memory) {
            this.memory = memory;
        }

        public Integer getCoveredServiceCount() {
            return coveredServiceCount;
        }

        public void setCoveredServiceCount(Integer coveredServiceCount) {
            this.coveredServiceCount = coveredServiceCount;
        }

        public Integer getExpectedServiceCount() {
            return expectedServiceCount;
        }

        public void setExpectedServiceCount(Integer expectedServiceCount) {
            this.expectedServiceCount = expectedServiceCount;
        }

        public List<ResourceControlResult> getResults() {
            return results;
        }

        public void setResults(List<ResourceControlResult> results) {
            this.results = results;
        }

        private ResourceControlResponse toInventory() {
            ResourceControlResponse response = new ResourceControlResponse();
            response.setCpuSet(cpuSet);
            response.setMemory(memory);
            response.setCoveredServiceCount(coveredServiceCount);
            response.setExpectedServiceCount(expectedServiceCount);
            response.setResults(results);
            return response;
        }
    }

    private static class HostCandidate {
        private final String hostUuid;
        private final String zoneUuid;

        private HostCandidate(String hostUuid, String zoneUuid) {
            this.hostUuid = hostUuid;
            this.zoneUuid = zoneUuid;
        }
    }
}
