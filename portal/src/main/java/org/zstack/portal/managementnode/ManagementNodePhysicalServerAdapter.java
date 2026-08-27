package org.zstack.portal.managementnode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeState;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.physicalserver.PhysicalServerResourceControlAdapter;
import org.zstack.header.physicalserver.PhysicalServerResourceApplicationMode;
import org.zstack.header.physicalserver.PhysicalServerResourceConsumerState;
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode;
import org.zstack.header.physicalserver.ManagedServiceResourceUsage;
import org.zstack.header.physicalserver.PhysicalServerCpuSet;
import org.zstack.header.physicalserver.PhysicalServerCpuTopology;
import org.zstack.header.physicalserver.PhysicalServerIdentitySpec;
import org.zstack.header.physicalserver.PhysicalServerManager;
import org.zstack.header.physicalserver.ResourceControlCommand;
import org.zstack.header.physicalserver.ResourceControlResponse;
import org.zstack.header.physicalserver.ResourceConsumerHandle;
import org.zstack.header.physicalserver.RoleServiceManifest;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.zstack.core.Platform.operr;

public class ManagementNodePhysicalServerAdapter extends AbstractService implements
        PhysicalServerResourceControlAdapter,
        ManagementNodeChangeListener,
        ManagementNodeReadyExtensionPoint,
        Component {
    public static final String ROLE_TYPE = "MANAGEMENT";
    public static final String ROLE_SERVICE_MANIFEST_PATH =
            "physical-server-roles/management.yaml";
    private static final String SERVICE_ID = "managementNodePhysicalServerResourceControl";
    private static final String ERROR_CODE = "ORG_ZSTACK_PORTAL_10000";
    private static final CLogger logger = Utils.getLogger(
            ManagementNodePhysicalServerAdapter.class);
    private static final RoleServiceManifest ROLE_SERVICES =
            RoleServiceManifest.load(
                    ROLE_SERVICE_MANIFEST_PATH,
                    ROLE_TYPE,
                    PhysicalServerResourceApplicationMode.RESOURCE_HANDLES);
    private final AtomicReference<Map<String, NodeRelation>> nodeRelations =
            new AtomicReference<>(Collections.emptyMap());
    private volatile String testSerialNumber;

    @Autowired(required = false)
    private PhysicalServerManager physicalServerManager;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private LocalCpuTopologyCollector localTopology;
    @Autowired
    private LocalResourceControlExecutor localResourceControlExecutor;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof CollectManagementNodeCpuTopologyMsg) {
            handle((CollectManagementNodeCpuTopologyMsg) msg);
        } else if (msg instanceof ApplyManagementNodeResourceControlMsg) {
            handle((ApplyManagementNodeResourceControlMsg) msg);
        } else if (msg instanceof CollectManagementNodeManagedServicesMsg) {
            handle((CollectManagementNodeManagedServicesMsg) msg);
        } else if (msg instanceof RestartManagementNodeManagedServicesMsg) {
            handle((RestartManagementNodeManagedServicesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CollectManagementNodeCpuTopologyMsg msg) {
        CollectManagementNodeCpuTopologyReply reply =
                new CollectManagementNodeCpuTopologyReply();
        if (!owns(msg.getServerUuid())) {
            reply.setError(notOwner(msg.getServerUuid()));
            bus.reply(msg, reply);
            return;
        }
        collectLocalTopology(new ReturnValueCompletion<PhysicalServerCpuTopology>(null) {
            @Override
            public void success(PhysicalServerCpuTopology topology) {
                reply.setTopology(topology);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(ApplyManagementNodeResourceControlMsg msg) {
        ApplyManagementNodeResourceControlReply reply =
                new ApplyManagementNodeResourceControlReply();
        if (!owns(msg.getServerUuid())) {
            reply.setError(notOwner(msg.getServerUuid()));
            bus.reply(msg, reply);
            return;
        }
        applyLocalResourceControl(
                msg.getCommand(), new ReturnValueCompletion<ResourceControlResponse>(null) {
            @Override
            public void success(ResourceControlResponse response) {
                reply.setResponse(response);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CollectManagementNodeManagedServicesMsg msg) {
        CollectManagementNodeManagedServicesReply reply =
                new CollectManagementNodeManagedServicesReply();
        if (!owns(msg.getServerUuid())) {
            reply.setError(notOwner(msg.getServerUuid()));
            bus.reply(msg, reply);
            return;
        }
        try {
            List<ManagedServiceResourceUsage> services =
                    localResourceControlExecutor.inspect(
                            ROLE_TYPE,
                            managementHandles(
                                    msg.isIncludeAuxiliaryServices()));
            for (ManagedServiceResourceUsage service : services) {
                service.setRoleType(ROLE_TYPE);
            }
            reply.setServices(services);
        } catch (RuntimeException error) {
            reply.setError(operr(
                    ERROR_CODE,
                    "MANAGED_SERVICE_QUERY_FAILED: %s",
                    error.getMessage()));
        }
        bus.reply(msg, reply);
    }

    private void handle(RestartManagementNodeManagedServicesMsg msg) {
        MessageReply reply = new MessageReply();
        if (!owns(msg.getServerUuid())) {
            reply.setError(notOwner(msg.getServerUuid()));
            bus.reply(msg, reply);
            return;
        }
        try {
            localResourceControlExecutor.restart(
                    ROLE_SERVICES.handlesByServiceNames(
                            msg.getServiceNames(),
                            Platform.getManagementServerId(),
                            Platform.getManagementServerId(),
                            true,
                            Collections.emptyMap()));
        } catch (RuntimeException error) {
            reply.setError(operr(
                    ERROR_CODE,
                    "MANAGED_SERVICE_RESTART_FAILED: %s",
                    error.getMessage()));
        }
        bus.reply(msg, reply);
    }

    private void collectLocalTopology(
            ReturnValueCompletion<PhysicalServerCpuTopology> completion) {
        try {
            completion.success(localTopology.collect());
        } catch (RuntimeException error) {
            completion.fail(operr(ERROR_CODE,
                    "CPU_TOPOLOGY_COLLECTION_FAILED: %s", error.getMessage()));
        }
    }

    private void applyLocalResourceControl(
            ResourceControlCommand command,
            ReturnValueCompletion<ResourceControlResponse> completion) {
        try {
            command.setSliceName(ROLE_SERVICES.getSliceName());
            command.setHandles(managementHandles(true));
            completion.success(localResourceControlExecutor.apply(command));
        } catch (RuntimeException error) {
            completion.fail(operr(ERROR_CODE,
                    "RESOURCE_CONTROL_APPLY_FAILED: %s", error.getMessage()));
        }
    }

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
        if (nodeRelations.get().isEmpty()) {
            refreshNodeRelations(Collections.emptySet());
        }
        return new LinkedHashSet<>(nodeRelations.get().keySet());
    }

    @Override
    public void refreshAssociations() {
        refreshNodeRelations(Collections.emptySet());
    }

    @Override
    public PhysicalServerResourceConsumerState getState(String serverUuid) {
        NodeRelation node = nodeRelation(serverUuid);
        if (node == null) {
            return PhysicalServerResourceConsumerState.UNAVAILABLE;
        }
        return node.state == ManagementNodeState.RUNNING
                ? PhysicalServerResourceConsumerState.AVAILABLE
                : PhysicalServerResourceConsumerState.UNAVAILABLE;
    }

    @Override
    public Map<String, PhysicalServerResourceConsumerState> getStates(
            Collection<String> serverUuids) {
        if (serverUuids == null || serverUuids.isEmpty()) {
            return Collections.emptyMap();
        }
        refreshMissingNodeRelations(serverUuids);
        Map<String, PhysicalServerResourceConsumerState> result = new LinkedHashMap<>();
        for (String serverUuid : serverUuids) {
            NodeRelation relation = nodeRelations.get().get(serverUuid);
            result.put(serverUuid, relation == null
                    ? PhysicalServerResourceConsumerState.UNAVAILABLE
                    : relation.state == ManagementNodeState.RUNNING
                    ? PhysicalServerResourceConsumerState.AVAILABLE
                    : PhysicalServerResourceConsumerState.UNAVAILABLE);
        }
        return result;
    }

    @Override
    public void collectTopology(
            String serverUuid,
            ReturnValueCompletion<PhysicalServerCpuTopology> completion) {
        String nodeUuid = nodeUuid(serverUuid, null);
        if (nodeUuid == null) {
            completion.fail(notOwner(serverUuid));
            return;
        }
        CollectManagementNodeCpuTopologyMsg msg =
                new CollectManagementNodeCpuTopologyMsg();
        msg.setServerUuid(serverUuid);
        msg.setTimeout(TimeUnit.MINUTES.toMillis(5));
        bus.makeServiceIdByManagementNodeId(msg, SERVICE_ID, nodeUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                CollectManagementNodeCpuTopologyReply topologyReply = reply.castReply();
                completion.success(topologyReply.getTopology());
            }
        });
    }

    @Override
    public void apply(
            String serverUuid,
            String consumerUuid,
            ResourceControlCommand command,
            ReturnValueCompletion<ResourceControlResponse> completion) {
        String nodeUuid = nodeUuid(serverUuid, consumerUuid);
        if (nodeUuid == null) {
            completion.fail(notOwner(serverUuid));
            return;
        }
        ApplyManagementNodeResourceControlMsg msg =
                new ApplyManagementNodeResourceControlMsg();
        msg.setServerUuid(serverUuid);
        msg.setCommand(command);
        msg.setTimeout(TimeUnit.MINUTES.toMillis(5));
        bus.makeServiceIdByManagementNodeId(msg, SERVICE_ID, nodeUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                ApplyManagementNodeResourceControlReply applyReply = reply.castReply();
                completion.success(applyReply.getResponse());
            }
        });
    }

    @Override
    public void collectManagedServiceUsage(
            String serverUuid,
            boolean includeAuxiliaryServices,
            ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion) {
        String nodeUuid = nodeUuid(serverUuid, null);
        if (nodeUuid == null) {
            completion.success(ROLE_SERVICES.managedServiceUsages(
                    includeAuxiliaryServices, "UNAVAILABLE"));
            return;
        }
        CollectManagementNodeManagedServicesMsg msg =
                new CollectManagementNodeManagedServicesMsg();
        msg.setServerUuid(serverUuid);
        msg.setIncludeAuxiliaryServices(includeAuxiliaryServices);
        msg.setTimeout(TimeUnit.MINUTES.toMillis(5));
        bus.makeServiceIdByManagementNodeId(msg, SERVICE_ID, nodeUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format(
                            "failed to query managed services on management node[uuid:%s]: %s",
                            nodeUuid, reply.getError()));
                    completion.success(ROLE_SERVICES.managedServiceUsages(
                            includeAuxiliaryServices, "UNAVAILABLE"));
                    return;
                }
                CollectManagementNodeManagedServicesReply serviceReply =
                        reply.castReply();
                completion.success(serviceReply.getServices() == null
                        ? ROLE_SERVICES.managedServiceUsages(
                        includeAuxiliaryServices, "UNAVAILABLE")
                        : serviceReply.getServices());
            }
        });
    }

    @Override
    public void restartManagedServices(
            String serverUuid,
            boolean includeAuxiliaryServices,
            Collection<String> serviceNames,
            Completion completion) {
        String nodeUuid = nodeUuid(serverUuid, null);
        if (nodeUuid == null) {
            completion.fail(notOwner(serverUuid));
            return;
        }
        RestartManagementNodeManagedServicesMsg msg =
                new RestartManagementNodeManagedServicesMsg();
        msg.setServerUuid(serverUuid);
        msg.setServiceNames(new ArrayList<>(serviceNames));
        msg.setTimeout(TimeUnit.MINUTES.toMillis(5));
        bus.makeServiceIdByManagementNodeId(msg, SERVICE_ID, nodeUuid);
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

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        if (inv.getServerUuid() != null) {
            removeNodeRelation(inv.getServerUuid());
        }
        if (physicalServerManager != null && inv.getServerUuid() != null) {
            physicalServerManager.reconcile(inv.getServerUuid(), true);
        }
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {
    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
        associateLocalNode(inv.getUuid());
    }

    @Override
    public void managementNodeReady() {
        associateLocalNode(Platform.getManagementServerId());
    }

    @Transactional
    public void associateLocalNode(String nodeUuid) {
        if (physicalServerManager == null
                || !Platform.getManagementServerId().equals(nodeUuid)) {
            return;
        }
        String current = Q.New(ManagementNodeVO.class)
                .select(ManagementNodeVO_.serverUuid)
                .eq(ManagementNodeVO_.uuid, nodeUuid)
                .findValue();
        if (current == null) {
            String serialNumber = managementServerSerialNumber();
            if (serialNumber == null) {
                logger.warn(String.format(
                        "cannot associate management node[uuid:%s] with a physical server, " +
                                "machine serial number is unavailable", nodeUuid));
                return;
            }
            current = physicalServerManager.resolveIdentities(
                    Collections.singletonList(
                            new PhysicalServerIdentitySpec(serialNumber, null)))
                    .get(serialNumber);
            if (current == null || linkedNode(current, nodeUuid) != null) {
                return;
            }
            Query update = dbf.getEntityManager().createNativeQuery(
                    "UPDATE IGNORE ManagementNodeVO SET serverUuid = :serverUuid " +
                            "WHERE uuid = :nodeUuid AND serverUuid IS NULL");
            update.setParameter("serverUuid", current);
            update.setParameter("nodeUuid", nodeUuid);
            update.executeUpdate();
            current = Q.New(ManagementNodeVO.class)
                    .select(ManagementNodeVO_.serverUuid)
                    .eq(ManagementNodeVO_.uuid, nodeUuid)
                    .findValue();
        }
        if (current != null) {
            refreshNodeRelations(Collections.singleton(current));
            physicalServerManager.ensureResourceAssignment(current, ROLE_TYPE);
            physicalServerManager.reconcile(current, true);
        }
    }

    private String linkedNode(String serverUuid, String excludedNodeUuid) {
        return Q.New(ManagementNodeVO.class)
                .select(ManagementNodeVO_.uuid)
                .eq(ManagementNodeVO_.serverUuid, serverUuid)
                .notEq(ManagementNodeVO_.uuid, excludedNodeUuid)
                .findValue();
    }

    private String nodeUuid(String serverUuid, String consumerUuid) {
        NodeRelation relation = nodeRelation(serverUuid);
        if (relation == null) {
            return null;
        }
        return consumerUuid == null || consumerUuid.equals(relation.nodeUuid)
                ? relation.nodeUuid : null;
    }

    private NodeRelation nodeRelation(String serverUuid) {
        NodeRelation relation = nodeRelations.get().get(serverUuid);
        if (relation != null) {
            return relation;
        }
        refreshNodeRelations(Collections.singleton(serverUuid));
        return nodeRelations.get().get(serverUuid);
    }

    private void refreshMissingNodeRelations(Collection<String> serverUuids) {
        Set<String> missing = new LinkedHashSet<>(serverUuids);
        missing.removeAll(nodeRelations.get().keySet());
        if (!missing.isEmpty()) {
            refreshNodeRelations(missing);
        }
    }

    private void refreshNodeRelations(Collection<String> serverUuids) {
        Q query = Q.New(ManagementNodeVO.class)
                .select(
                        ManagementNodeVO_.serverUuid,
                        ManagementNodeVO_.uuid,
                        ManagementNodeVO_.state)
                .notNull(ManagementNodeVO_.serverUuid);
        boolean partial = serverUuids != null && !serverUuids.isEmpty();
        if (partial) {
            query.in(ManagementNodeVO_.serverUuid, serverUuids);
        }
        Map<String, NodeRelation> loaded = new LinkedHashMap<>();
        for (Tuple node : (List<Tuple>) query.listTuple()) {
            loaded.put(
                    node.get(0, String.class),
                    new NodeRelation(
                            node.get(1, String.class),
                            node.get(2, ManagementNodeState.class)));
        }
        if (!partial) {
            nodeRelations.set(Collections.unmodifiableMap(loaded));
            return;
        }
        while (true) {
            Map<String, NodeRelation> current = nodeRelations.get();
            Map<String, NodeRelation> replacement = new LinkedHashMap<>(current);
            for (String serverUuid : serverUuids) {
                replacement.remove(serverUuid);
            }
            replacement.putAll(loaded);
            if (nodeRelations.compareAndSet(
                    current, Collections.unmodifiableMap(replacement))) {
                return;
            }
        }
    }

    private void removeNodeRelation(String serverUuid) {
        while (true) {
            Map<String, NodeRelation> current = nodeRelations.get();
            if (!current.containsKey(serverUuid)) {
                return;
            }
            Map<String, NodeRelation> replacement = new LinkedHashMap<>(current);
            replacement.remove(serverUuid);
            if (nodeRelations.compareAndSet(
                    current, Collections.unmodifiableMap(replacement))) {
                return;
            }
        }
    }

    private static class NodeRelation {
        private final String nodeUuid;
        private final ManagementNodeState state;

        private NodeRelation(
                String nodeUuid,
                ManagementNodeState state) {
            this.nodeUuid = nodeUuid;
            this.state = state;
        }
    }

    private boolean owns(String serverUuid) {
        return serverUuid != null && serverUuid.equals(Q.New(ManagementNodeVO.class)
                .select(ManagementNodeVO_.serverUuid)
                .eq(ManagementNodeVO_.uuid, Platform.getManagementServerId())
                .findValue());
    }

    private ErrorCode notOwner(String serverUuid) {
        return operr(ERROR_CODE,
                "management node[uuid:%s] is not associated with physical server[uuid:%s]",
                Platform.getManagementServerId(), serverUuid);
    }

    private List<ResourceConsumerHandle> managementHandles(
            boolean includeAuxiliaryServices) {
        String consumerKey = String.format(
                "management-node:%s", Platform.getManagementServerId());
        return ROLE_SERVICES.handles(
                consumerKey,
                consumerKey,
                includeAuxiliaryServices);
    }

    private String managementServerSerialNumber() {
        return CoreGlobalProperty.UNIT_TEST_ON
                ? Platform.normalizeMachineSerialNumber(testSerialNumber)
                : Platform.getManagementServerSerialNumber();
    }

    public void setTestSerialNumber(String serialNumber) {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            throw new IllegalStateException(
                    "test serial number is only available in unit-test mode");
        }
        testSerialNumber = serialNumber;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }
}
