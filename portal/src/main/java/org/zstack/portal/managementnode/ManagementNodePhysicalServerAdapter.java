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
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentController;
import org.zstack.header.physicalserver.PhysicalServerResourceBoundary;
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode;
import org.zstack.header.physicalserver.ManagedServiceResourceUsage;
import org.zstack.header.physicalserver.PhysicalServerCpuTopology;
import org.zstack.header.physicalserver.PhysicalServerManager;
import org.zstack.header.physicalserver.PhysicalServerResourceUsageObserver;
import org.zstack.header.physicalserver.PhysicalServerRoleAssociationProvider;
import org.zstack.header.physicalserver.PhysicalServerRoleType;
import org.zstack.header.physicalserver.ResourceControlCommand;
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
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_PORTAL_MANAGEMENTNODE_10000;

public class ManagementNodePhysicalServerAdapter extends AbstractService implements
        PhysicalServerResourceAssignmentController,
        PhysicalServerResourceUsageObserver,
        PhysicalServerRoleAssociationProvider,
        ManagementNodeChangeListener, ManagementNodeReadyExtensionPoint, Component {
    public static final PhysicalServerRoleType type = new PhysicalServerRoleType("MANAGEMENT");
    public static final String ROLE_SERVICE_MANIFEST_PATH = "physical-server-roles/management.yaml";
    private static final String SERVICE_ID = "managementNodePhysicalServerResourceControl";
    private static final CLogger logger = Utils.getLogger(ManagementNodePhysicalServerAdapter.class);
    private final AtomicReference<Map<String, String>> nodeRelations = new AtomicReference<>(Collections.emptyMap());
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
        CollectManagementNodeCpuTopologyReply reply = new CollectManagementNodeCpuTopologyReply();
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
        ApplyManagementNodeResourceControlReply reply = new ApplyManagementNodeResourceControlReply();
        if (!owns(msg.getServerUuid())) {
            reply.setError(notOwner(msg.getServerUuid()));
            bus.reply(msg, reply);
            return;
        }
        applyLocalResourceControl(
                msg.getCommand(), new ReturnValueCompletion<Boolean>(null) {
            @Override
            public void success(Boolean synced) {
                reply.setSynced(Boolean.TRUE.equals(synced));
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
        CollectManagementNodeManagedServicesReply reply = new CollectManagementNodeManagedServicesReply();
        if (!owns(msg.getServerUuid())) {
            reply.setError(notOwner(msg.getServerUuid()));
            bus.reply(msg, reply);
            return;
        }
        try {
            List<ManagedServiceResourceUsage> services =
                    localResourceControlExecutor.inspect(type.toString(), managementHandles());
            for (ManagedServiceResourceUsage service : services) {
                service.setRoleType(type.toString());
            }
            reply.setServices(services);
        } catch (RuntimeException error) {
            reply.setError(operr(
                    ORG_ZSTACK_PORTAL_MANAGEMENTNODE_10000,
                    "Failed to query managed service usage: %s", error.getMessage()));
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
            localResourceControlExecutor.restart(roleServices().getSliceName(), msg.getConsumers());
        } catch (RuntimeException error) {
            reply.setError(operr(
                    ORG_ZSTACK_PORTAL_MANAGEMENTNODE_10000,
                    "Failed to restart managed services: %s", error.getMessage()));
        }
        bus.reply(msg, reply);
    }

    private void collectLocalTopology(ReturnValueCompletion<PhysicalServerCpuTopology> completion) {
        try {
            completion.success(localTopology.collect());
        } catch (RuntimeException error) {
            completion.fail(operr(ORG_ZSTACK_PORTAL_MANAGEMENTNODE_10000,
                    "Failed to collect management node CPU topology: %s", error.getMessage()));
        }
    }

    private void applyLocalResourceControl(ResourceControlCommand command, ReturnValueCompletion<Boolean> completion) {
        try {
            command.setSliceName(roleServices().getSliceName());
            if ("APPLY".equals(command.getOperation())) {
                completion.success(localResourceControlExecutor.apply(command));
            } else if ("RELEASE".equals(command.getOperation())) {
                completion.success(localResourceControlExecutor.release(command));
            } else {
                completion.fail(operr(
                        ORG_ZSTACK_PORTAL_MANAGEMENTNODE_10000,
                        "Resource control operation[%s] is unsupported", command.getOperation()));
            }
        } catch (RuntimeException error) {
            completion.fail(operr(ORG_ZSTACK_PORTAL_MANAGEMENTNODE_10000,
                    "Failed to apply management node resource assignment: %s", error.getMessage()));
        }
    }

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
        return discoverNodeRelations(serverUuids);
    }

    @Override
    public List<ResourceConsumerHandle> getResourceConsumers(String serverUuid) {
        String nodeUuid = nodeUuid(serverUuid);
        if (nodeUuid == null) {
            throw new IllegalStateException(String.format(
                    "Physical server[uuid:%s] has no management node", serverUuid));
        }
        return managementHandles();
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
                            completion.fail(operr(ORG_ZSTACK_PORTAL_MANAGEMENTNODE_10000, "%s", error.getMessage()));
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
        String nodeUuid = nodeUuid(serverUuid);
        if (nodeUuid == null) {
            completion.fail(notOwner(serverUuid));
            return;
        }
        CollectManagementNodeCpuTopologyMsg msg = new CollectManagementNodeCpuTopologyMsg();
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
    public void apply(String serverUuid, ResourceControlCommand command, ReturnValueCompletion<Boolean> completion) {
        command.setOperation("APPLY");
        sendResourceControl(serverUuid, command, completion);
    }

    @Override
    public void release(String serverUuid, ResourceControlCommand command, ReturnValueCompletion<Boolean> completion) {
        command.setOperation("RELEASE");
        sendResourceControl(serverUuid, command, completion);
    }

    private void sendResourceControl(String serverUuid, ResourceControlCommand command,
                                     ReturnValueCompletion<Boolean> completion) {
        String nodeUuid = nodeUuid(serverUuid);
        if (nodeUuid == null) {
            completion.fail(notOwner(serverUuid));
            return;
        }
        ApplyManagementNodeResourceControlMsg msg = new ApplyManagementNodeResourceControlMsg();
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
                completion.success(applyReply.isSynced());
            }
        });
    }

    @Override
    public void collectManagedServiceUsage(
            String serverUuid, ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion) {
        String nodeUuid = nodeUuid(serverUuid);
        if (nodeUuid == null) {
            completion.fail(notOwner(serverUuid));
            return;
        }
        CollectManagementNodeManagedServicesMsg msg = new CollectManagementNodeManagedServicesMsg();
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
                CollectManagementNodeManagedServicesReply serviceReply = reply.castReply();
                if (serviceReply.getServices() == null) {
                    completion.fail(operr(ORG_ZSTACK_PORTAL_MANAGEMENTNODE_10000,
                            "Management node[uuid:%s] returned no managed service usage", nodeUuid));
                    return;
                }
                completion.success(serviceReply.getServices());
            }
        });
    }

    @Override
    public void restartManagedServices(
            String serverUuid, Collection<ResourceConsumerHandle> consumers, Completion completion) {
        String nodeUuid = nodeUuid(serverUuid);
        if (nodeUuid == null) {
            completion.fail(notOwner(serverUuid));
            return;
        }
        RestartManagementNodeManagedServicesMsg msg = new RestartManagementNodeManagedServicesMsg();
        msg.setServerUuid(serverUuid);
        msg.setConsumers(new ArrayList<>(consumers));
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
            physicalServerManager.associationChanged(inv.getServerUuid());
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
        if (physicalServerManager == null || !Platform.getManagementServerId().equals(nodeUuid)) {
            return;
        }
        String current = Q.New(ManagementNodeVO.class)
                .select(ManagementNodeVO_.serverUuid).eq(ManagementNodeVO_.uuid, nodeUuid).findValue();
        if (current == null) {
            String serialNumber = managementServerSerialNumber();
            if (serialNumber == null) {
                logger.warn(String.format(
                        "cannot associate management node[uuid:%s] with a physical server, " +
                                "machine serial number is unavailable", nodeUuid));
                return;
            }
            current = physicalServerManager.resolveBySerialNumbers(
                    Collections.singleton(serialNumber)).get(serialNumber);
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
                    .select(ManagementNodeVO_.serverUuid).eq(ManagementNodeVO_.uuid, nodeUuid).findValue();
        }
        if (current != null) {
            physicalServerManager.associationChanged(current);
        }
    }

    private String linkedNode(String serverUuid, String excludedNodeUuid) {
        return Q.New(ManagementNodeVO.class)
                .select(ManagementNodeVO_.uuid)
                .eq(ManagementNodeVO_.serverUuid, serverUuid)
                .notEq(ManagementNodeVO_.uuid, excludedNodeUuid).findValue();
    }

    private String nodeUuid(String serverUuid) {
        return nodeRelations.get().get(serverUuid);
    }

    private Set<String> discoverNodeRelations(Collection<String> serverUuids) {
        Q query = Q.New(ManagementNodeVO.class)
                .select(ManagementNodeVO_.serverUuid, ManagementNodeVO_.uuid).notNull(ManagementNodeVO_.serverUuid);
        boolean partial = serverUuids != null && !serverUuids.isEmpty();
        if (partial) {
            query.in(ManagementNodeVO_.serverUuid, serverUuids);
        }
        Map<String, String> loaded = new LinkedHashMap<>();
        for (Tuple node : (List<Tuple>) query.listTuple()) {
            loaded.put(node.get(0, String.class), node.get(1, String.class));
        }
        if (!partial) {
            nodeRelations.set(Collections.unmodifiableMap(loaded));
            return new LinkedHashSet<>(loaded.keySet());
        }
        nodeRelations.updateAndGet(current -> {
            Map<String, String> replacement = new LinkedHashMap<>(current);
            serverUuids.forEach(replacement::remove);
            replacement.putAll(loaded);
            return Collections.unmodifiableMap(replacement);
        });
        return new LinkedHashSet<>(loaded.keySet());
    }

    private void removeNodeRelation(String serverUuid) {
        nodeRelations.updateAndGet(current -> {
            if (!current.containsKey(serverUuid)) {
                return current;
            }
            Map<String, String> replacement = new LinkedHashMap<>(current);
            replacement.remove(serverUuid);
            return Collections.unmodifiableMap(replacement);
        });
    }

    private boolean owns(String serverUuid) {
        return serverUuid != null && serverUuid.equals(Q.New(ManagementNodeVO.class)
                .select(ManagementNodeVO_.serverUuid)
                .eq(ManagementNodeVO_.uuid, Platform.getManagementServerId()).findValue());
    }

    private ErrorCode notOwner(String serverUuid) {
        return operr(ORG_ZSTACK_PORTAL_MANAGEMENTNODE_10000,
                "management node[uuid:%s] is not associated with physical server[uuid:%s]",
                Platform.getManagementServerId(), serverUuid);
    }

    private List<ResourceConsumerHandle> managementHandles() {
        return roleServices().handles();
    }

    private RoleServiceManifest roleServices() {
        return RoleServiceManifest.load(ROLE_SERVICE_MANIFEST_PATH, type.toString());
    }

    private String managementServerSerialNumber() {
        return CoreGlobalProperty.UNIT_TEST_ON
                ? Platform.normalizeMachineSerialNumber(testSerialNumber) : Platform.getManagementServerSerialNumber();
    }

    public void setTestSerialNumber(String serialNumber) {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            throw new IllegalStateException("test serial number is only available in unit-test mode");
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
