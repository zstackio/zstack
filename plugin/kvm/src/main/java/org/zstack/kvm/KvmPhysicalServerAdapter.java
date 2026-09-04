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
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostDeleteExtensionPoint;
import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostNUMANode;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentController;
import org.zstack.header.physicalserver.PhysicalServerResourceBoundary;
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode;
import org.zstack.header.physicalserver.PhysicalServerCpuTopology;
import org.zstack.header.physicalserver.PhysicalServerManager;
import org.zstack.header.physicalserver.PhysicalServerResourceUsageObserver;
import org.zstack.header.physicalserver.PhysicalServerRoleAssociationProvider;
import org.zstack.header.physicalserver.PhysicalServerRoleType;
import org.zstack.header.physicalserver.ManagedServiceResourceUsage;
import org.zstack.header.physicalserver.PhysicalServerNumaNode;
import org.zstack.header.physicalserver.ResourceControlCommand;
import org.zstack.header.physicalserver.ResourceConsumerHandle;
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
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_KVM_10000;

public class KvmPhysicalServerAdapter implements
        PhysicalServerResourceAssignmentController,
        PhysicalServerResourceUsageObserver,
        PhysicalServerRoleAssociationProvider,
        PostHostConnectExtensionPoint, HostDeleteExtensionPoint, ManagementNodeReadyExtensionPoint, Component {
    public static final PhysicalServerRoleType type = new PhysicalServerRoleType("COMPUTE");
    public static final String APPLY_RESOURCE_CONTROL_PATH = "/host/resourcecontrol/apply";
    public static final String RELEASE_RESOURCE_CONTROL_PATH = "/host/resourcecontrol/release";
    public static final String GET_MANAGED_SERVICE_USAGE_PATH = "/host/resourcecontrol/services";
    public static final String RESTART_MANAGED_SERVICES_PATH = "/host/resourcecontrol/restart";
    public static final String ROLE_SERVICE_MANIFEST_PATH = "physical-server-roles/compute.yaml";
    private static final int HOST_LINK_UPDATE_BATCH_SIZE = 500;
    private static final CLogger logger = Utils.getLogger(KvmPhysicalServerAdapter.class);
    private final AtomicReference<Map<String, String>> hostRelations = new AtomicReference<>(Collections.emptyMap());

    @Autowired(required = false)
    private PhysicalServerManager physicalServerManager;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Override
    public PhysicalServerRoleType getRoleType() {
        return type;
    }

    @Override
    public PhysicalServerResourceIsolationMode getIsolationMode() {
        return roleServices().getIsolationMode();
    }

    @Override
    public Integer getDefaultCpuCount() {
        return roleServices().getDefaultCpuCount();
    }

    @Override
    public Set<String> discoverAssociations(Collection<String> serverUuids) {
        return discoverHostRelations(serverUuids);
    }

    @Override
    public List<ResourceConsumerHandle> getResourceConsumers(String serverUuid) {
        String hostUuid = hostUuid(serverUuid);
        if (hostUuid == null) {
            throw new IllegalStateException(String.format("Physical server[uuid:%s] has no KVM host", serverUuid));
        }
        return roleServices().handles();
    }

    @Override
    public void collectResourceAssignment(
            String serverUuid, ReturnValueCompletion<PhysicalServerResourceBoundary> completion) {
        collectManagedServiceUsage(
                serverUuid, new ReturnValueCompletion<List<ManagedServiceResourceUsage>>(completion) {
                    @Override
                    public void success(List<ManagedServiceResourceUsage> services) {
                        try {
                            completion.success(PhysicalServerResourceBoundary.fromManagedServiceUsages(services));
                        } catch (RuntimeException error) {
                            completion.fail(operr(ORG_ZSTACK_KVM_10000, "%s", error.getMessage()));
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    @Override
    public void collectTopology(String serverUuid, ReturnValueCompletion<PhysicalServerCpuTopology> completion) {
        String hostUuid = hostUuid(serverUuid);
        if (hostUuid == null) {
            completion.fail(operr(ORG_ZSTACK_KVM_10000, "Physical server[uuid:%s] has no KVM host", serverUuid));
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
            GetHostNumaTopologyReply reply, ReturnValueCompletion<PhysicalServerCpuTopology> completion) {
        try {
            completion.success(PhysicalServerCpuTopology.from(neutralTopology(reply.getNuma())));
        } catch (RuntimeException error) {
            completion.fail(operr(ORG_ZSTACK_KVM_10000, "Host returned invalid CPU topology: %s", error.getMessage()));
        }
    }

    @Override
    public void apply(String serverUuid, ResourceControlCommand command, ReturnValueCompletion<Boolean> completion) {
        String hostUuid = hostUuid(serverUuid);
        if (hostUuid == null) {
            completion.fail(operr(ORG_ZSTACK_KVM_10000, "Physical server[uuid:%s] has no KVM host", serverUuid));
            return;
        }

        ApplyResourceControlAgentCommand agentCommand = applyAgentCommand(command);
        agentCommand.setCpuSet(command.getCpuSet());
        agentCommand.setMemory(command.getMemory());
        agentCommand.setIsolationMode(
                command.getIsolationMode() == null
                        ? PhysicalServerResourceIsolationMode.SHARED.name() : command.getIsolationMode().name());
        sendApplyResourceControl(hostUuid, agentCommand, completion);
    }

    @Override
    public void release(String serverUuid, ResourceControlCommand command, ReturnValueCompletion<Boolean> completion) {
        String hostUuid = hostUuid(serverUuid);
        if (hostUuid == null) {
            completion.fail(operr(ORG_ZSTACK_KVM_10000, "Physical server[uuid:%s] has no KVM host", serverUuid));
            return;
        }

        ManagedServiceAgentCommand agentCommand = new ManagedServiceAgentCommand();
        agentCommand.setRoleType(command.getRoleType());
        agentCommand.setSliceName(roleServices().getSliceName());
        agentCommand.setHandles(command.getHandles());
        sendReleaseResourceControl(hostUuid, agentCommand, completion);
    }

    private ApplyResourceControlAgentCommand applyAgentCommand(ResourceControlCommand command) {
        ApplyResourceControlAgentCommand result = new ApplyResourceControlAgentCommand();
        result.setRoleType(command.getRoleType());
        result.setSliceName(roleServices().getSliceName());
        result.setHandles(command.getHandles());
        return result;
    }

    private void sendApplyResourceControl(String hostUuid, ApplyResourceControlAgentCommand agentCommand,
                                          ReturnValueCompletion<Boolean> completion) {
        KVMHostAsyncHttpCallMsg msg = resourceControlCall(hostUuid, agentCommand);
        msg.setPath(APPLY_RESOURCE_CONTROL_PATH);
        sendResourceControl(msg, completion);
    }

    private void sendReleaseResourceControl(String hostUuid, ManagedServiceAgentCommand agentCommand,
                                            ReturnValueCompletion<Boolean> completion) {
        KVMHostAsyncHttpCallMsg msg = resourceControlCall(hostUuid, agentCommand);
        msg.setPath(RELEASE_RESOURCE_CONTROL_PATH);
        sendResourceControl(msg, completion);
    }

    private KVMHostAsyncHttpCallMsg resourceControlCall(String hostUuid, KVMAgentCommands.AgentCommand agentCommand) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(agentCommand);
        msg.setTimeout(TimeUnit.MINUTES.toMillis(5));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        return msg;
    }

    private void sendResourceControl(KVMHostAsyncHttpCallMsg msg, ReturnValueCompletion<Boolean> completion) {
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                ErrorableValue<ResourceControlAgentResponse> result =
                        KVMHostAsyncHttpCallReply.unwrap(reply, ResourceControlAgentResponse.class);
                if (!result.isSuccess()) {
                    completion.fail(result.error);
                    return;
                }
                completion.success(result.result.isSynced());
            }
        });
    }

    @Override
    public void collectManagedServiceUsage(
            String serverUuid, ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion) {
        RoleServiceManifest roleServices = roleServices();
        String hostUuid = hostUuid(serverUuid);
        if (hostUuid == null) {
            completion.fail(operr(ORG_ZSTACK_KVM_10000, "Physical server[uuid:%s] has no KVM host", serverUuid));
            return;
        }
        ManagedServiceAgentCommand command = new ManagedServiceAgentCommand();
        command.setRoleType(type.toString());
        command.setSliceName(roleServices.getSliceName());
        command.setHandles(roleServices.handles());
        KVMHostAsyncHttpCallMsg msg = managedServiceCall(hostUuid, GET_MANAGED_SERVICE_USAGE_PATH, command);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                ErrorableValue<ManagedServiceUsageAgentResponse> result =
                        KVMHostAsyncHttpCallReply.unwrap(reply, ManagedServiceUsageAgentResponse.class);
                if (!result.isSuccess()) {
                    completion.fail(result.error);
                    return;
                }
                if (result.result == null || result.result.getServices() == null) {
                    completion.fail(operr(ORG_ZSTACK_KVM_10000,
                            "Host[uuid:%s] returned no managed service usage", hostUuid));
                    return;
                }
                List<ManagedServiceResourceUsage> services = result.result.getServices();
                for (ManagedServiceResourceUsage usage : services) {
                    usage.setRoleType(type.toString());
                }
                completion.success(services);
            }
        });
    }

    @Override
    public void restartManagedServices(
            String serverUuid, Collection<ResourceConsumerHandle> consumers, Completion completion) {
        String hostUuid = hostUuid(serverUuid);
        if (hostUuid == null) {
            completion.fail(operr(ORG_ZSTACK_KVM_10000, "Physical server[uuid:%s] has no KVM host", serverUuid));
            return;
        }
        ManagedServiceAgentCommand command = new ManagedServiceAgentCommand();
        command.setRoleType(type.toString());
        command.setSliceName(roleServices().getSliceName());
        command.setHandles(new ArrayList<>(consumers));
        KVMHostAsyncHttpCallMsg msg = managedServiceCall(hostUuid, RESTART_MANAGED_SERVICES_PATH, command);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                ErrorableValue<KVMAgentCommands.AgentResponse> result =
                        KVMHostAsyncHttpCallReply.unwrap(reply, KVMAgentCommands.AgentResponse.class);
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
                .eq(HostAO_.serverUuid, serverUuid).set(HostAO_.serverUuid, null).update();
        removeHostRelation(serverUuid);
    }

    @Override
    public void afterDeleteHost(HostInventory inventory) {
        if (inventory.getServerUuid() == null || physicalServerManager == null) {
            return;
        }
        removeHostRelation(inventory.getServerUuid());
        physicalServerManager.associationChanged(inventory.getServerUuid());
    }

    @Override
    public void managementNodeReady() {
        backfill();
    }

    @Override
    public boolean start() {
        HostSystemTags.SYSTEM_SERIAL_NUMBER.installLifeCycleListener(
                new AbstractSystemTagLifeCycleListener() {
                    @Override
                    public void tagCreated(SystemTagInventory tag) {
                        backfill(Collections.singleton(tag.getResourceUuid()));
                    }

                    @Override
                    public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {
                        backfill(Collections.singleton(newTag.getResourceUuid()));
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
        if (physicalServerManager == null || !KVMConstant.KVM_HYPERVISOR_TYPE.equals(host.getHypervisorType())) {
            return;
        }
        String current = Q.New(HostVO.class).select(HostVO_.serverUuid).eq(HostVO_.uuid, host.getUuid()).findValue();
        if (current != null) {
            physicalServerManager.associationChanged(current);
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
        String serverUuid = physicalServerManager.resolveBySerialNumbers(
                Collections.singleton(serialNumber)).get(serialNumber);
        if (serverUuid == null) {
            return;
        }
        clearDeletedHostLinks(Collections.singleton(serverUuid));
        if (linkedHost(serverUuid, host.getUuid()) != null) {
            return;
        }

        Query update = dbf.getEntityManager().createNativeQuery(
                "UPDATE IGNORE HostEO SET serverUuid = :serverUuid " + "WHERE uuid = :hostUuid AND serverUuid IS NULL");
        update.setParameter("serverUuid", serverUuid);
        update.setParameter("hostUuid", host.getUuid());
        update.executeUpdate();
        current = Q.New(HostVO.class).select(HostVO_.serverUuid).eq(HostVO_.uuid, host.getUuid()).findValue();
        if (!serverUuid.equals(current)) {
            logger.warn(String.format(
                    "cannot associate host[uuid:%s] with physical server[uuid:%s] " +
                            "because the host is already associated elsewhere", host.getUuid(), serverUuid));
            return;
        }
        physicalServerManager.associationChanged(serverUuid);
    }

    @Transactional
    private void backfill() {
        backfill(Collections.emptySet());
    }

    @Transactional
    private void backfill(Collection<String> hostUuids) {
        if (physicalServerManager == null) {
            return;
        }
        Q hostQuery = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE).isNull(HostVO_.serverUuid);
        if (hostUuids != null && !hostUuids.isEmpty()) {
            hostQuery.in(HostVO_.uuid, hostUuids);
        }
        List<String> unresolvedHostUuids = hostQuery.listValues();
        if (unresolvedHostUuids.isEmpty()) {
            return;
        }
        Map<String, Set<String>> serialsByHost = serialsByHost(unresolvedHostUuids);
        Map<String, List<String>> candidatesBySerial = new LinkedHashMap<>();
        for (String hostUuid : unresolvedHostUuids) {
            Set<String> serials = serialsByHost.get(hostUuid);
            if (serials == null || serials.size() != 1) {
                continue;
            }
            candidatesBySerial.computeIfAbsent(serials.iterator().next(), ignored -> new ArrayList<>()).add(hostUuid);
        }

        Map<String, String> candidates = new LinkedHashMap<>();
        Set<String> serialNumbers = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> candidate : candidatesBySerial.entrySet()) {
            if (candidate.getValue().size() != 1) {
                logger.warn(String.format(
                        "cannot backfill host physical server association for serialNumber[%s] " +
                                "because it matches multiple hosts", candidate.getKey()));
                continue;
            }
            String host = candidate.getValue().get(0);
            candidates.put(candidate.getKey(), host);
            serialNumbers.add(candidate.getKey());
        }
        Map<String, String> resolved = physicalServerManager.resolveBySerialNumbers(serialNumbers);
        clearDeletedHostLinks(resolved.values());
        Set<String> used = new HashSet<>(Q.New(HostVO.class)
                .select(HostVO_.serverUuid).notNull(HostVO_.serverUuid).listValues());
        Map<String, String> links = new LinkedHashMap<>();
        for (Map.Entry<String, String> candidate : candidates.entrySet()) {
            String serverUuid = resolved.get(candidate.getKey());
            if (serverUuid != null && used.add(serverUuid)) {
                links.put(candidate.getValue(), serverUuid);
            }
        }
        updateHostLinks(links);

        Set<String> linkedServers = links.isEmpty()
                ? Collections.emptySet()
                : new LinkedHashSet<>(Q.New(HostVO.class)
                        .select(HostVO_.serverUuid)
                        .in(HostVO_.uuid, links.keySet()).notNull(HostVO_.serverUuid).listValues());
        for (String serverUuid : linkedServers) {
            physicalServerManager.associationChanged(serverUuid);
        }
    }

    private Map<String, Set<String>> serialsByHost(Collection<String> hostUuids) {
        List<Tuple> tagTuples = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.resourceUuid, SystemTagVO_.tag)
                .in(SystemTagVO_.resourceUuid, hostUuids)
                .eq(SystemTagVO_.resourceType, HostVO.class.getSimpleName())
                .like(SystemTagVO_.tag, TagUtils.tagPatternToSqlPattern(
                        HostSystemTags.SYSTEM_SERIAL_NUMBER.getTagFormat())).listTuple();
        Map<String, Set<String>> result = new HashMap<>();
        for (Tuple tuple : tagTuples) {
            String hostUuid = tuple.get(0, String.class);
            String serialNumber = Platform.normalizeMachineSerialNumber(
                    HostSystemTags.SYSTEM_SERIAL_NUMBER.getTokenByTag(
                            tuple.get(1, String.class), HostSystemTags.SYSTEM_SERIAL_NUMBER_TOKEN));
            if (serialNumber != null) {
                result.computeIfAbsent(hostUuid, ignored -> new LinkedHashSet<>()).add(serialNumber);
            }
        }
        return result;
    }

    private void updateHostLinks(Map<String, String> links) {
        if (links.isEmpty()) {
            return;
        }
        List<Map.Entry<String, String>> entries = new ArrayList<>(links.entrySet());
        for (int start = 0; start < entries.size(); start += HOST_LINK_UPDATE_BATCH_SIZE) {
            updateHostLinks(entries.subList(start, Math.min(start + HOST_LINK_UPDATE_BATCH_SIZE, entries.size())));
        }
    }

    private void updateHostLinks(List<Map.Entry<String, String>> links) {
        StringBuilder sql = new StringBuilder("UPDATE IGNORE HostEO SET serverUuid = CASE uuid");
        int index = 0;
        for (Map.Entry<String, String> ignored : links) {
            sql.append(" WHEN :host").append(index).append(" THEN :server").append(index);
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
                "UPDATE HostEO SET serverUuid = NULL " + "WHERE deleted IS NOT NULL AND serverUuid IN (");
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

    private String linkedHost(String serverUuid, String excludedHostUuid) {
        return Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .eq(HostVO_.serverUuid, serverUuid).notEq(HostVO_.uuid, excludedHostUuid).findValue();
    }

    private String hostUuid(String serverUuid) {
        return hostRelations.get().get(serverUuid);
    }

    private Set<String> discoverHostRelations(Collection<String> serverUuids) {
        Q query = Q.New(HostVO.class)
                .select(HostVO_.serverUuid, HostVO_.uuid)
                .eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE).notNull(HostVO_.serverUuid);
        boolean partial = serverUuids != null && !serverUuids.isEmpty();
        if (partial) {
            query.in(HostVO_.serverUuid, serverUuids);
        }
        Map<String, String> loaded = new HashMap<>();
        for (Tuple host : (List<Tuple>) query.listTuple()) {
            loaded.put(host.get(0, String.class), host.get(1, String.class));
        }
        if (!partial) {
            hostRelations.set(Collections.unmodifiableMap(loaded));
            return new LinkedHashSet<>(loaded.keySet());
        }
        hostRelations.updateAndGet(current -> {
            Map<String, String> replacement = new HashMap<>(current);
            serverUuids.forEach(replacement::remove);
            replacement.putAll(loaded);
            return Collections.unmodifiableMap(replacement);
        });
        return new LinkedHashSet<>(loaded.keySet());
    }

    private void removeHostRelation(String serverUuid) {
        hostRelations.updateAndGet(current -> {
            if (!current.containsKey(serverUuid)) {
                return current;
            }
            Map<String, String> replacement = new HashMap<>(current);
            replacement.remove(serverUuid);
            return Collections.unmodifiableMap(replacement);
        });
    }

    private Map<String, PhysicalServerNumaNode> neutralTopology(Map<String, HostNUMANode> topology) {
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

    private RoleServiceManifest roleServices() {
        return RoleServiceManifest.load(ROLE_SERVICE_MANIFEST_PATH, type.toString());
    }

    public static class ApplyResourceControlAgentCommand extends ManagedServiceAgentCommand {
        private String cpuSet;
        private Long memory;
        private String isolationMode;

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

        public String getIsolationMode() {
            return isolationMode;
        }

        public void setIsolationMode(String isolationMode) {
            this.isolationMode = isolationMode;
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
        private boolean synced;

        public boolean isSynced() {
            return synced;
        }

        public void setSynced(boolean synced) {
            this.synced = synced;
        }

    }

}
