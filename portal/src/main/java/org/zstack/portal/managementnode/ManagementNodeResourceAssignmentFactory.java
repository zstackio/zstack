package org.zstack.portal.managementnode;

import java.util.Collections;
import java.util.List;
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.physicalserver.*;
import org.zstack.physicalserver.PhysicalServerResourceAssignmentGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_PORTAL_MANAGEMENTNODE_10000;

public class ManagementNodeResourceAssignmentFactory extends AbstractService implements
        PhysicalServerResourceAssignmentFactory,
        ManagementNodeChangeListener, ManagementNodeReadyExtensionPoint, Component {
    public static final PhysicalServerRoleType type = new PhysicalServerRoleType("MANAGEMENT");
    public static final String ROLE_SERVICE_MANIFEST_PATH = "physical-server-roles/management.yaml";
    static final String SERVICE_ID = "managementNodePhysicalServerResourceControl";
    private static final CLogger logger = Utils.getLogger(ManagementNodeResourceAssignmentFactory.class);
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
        } else if (msg instanceof ReleaseManagementNodeResourceControlMsg) {
            handle((ReleaseManagementNodeResourceControlMsg) msg);
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
        reply.setTopology(localTopology.collect());
        bus.reply(msg, reply);
    }

    private void handle(ApplyManagementNodeResourceControlMsg msg) {
        ManagementNodeResourceControlReply reply = new ManagementNodeResourceControlReply();
        if (!owns(msg.getServerUuid())) {
            reply.setError(notOwner(msg.getServerUuid()));
            bus.reply(msg, reply);
            return;
        }
        reply.setSynced(localResourceControlExecutor.apply(msg.getCommand()));
        bus.reply(msg, reply);
    }

    private void handle(ReleaseManagementNodeResourceControlMsg msg) {
        ManagementNodeResourceControlReply reply = new ManagementNodeResourceControlReply();
        if (!owns(msg.getServerUuid())) {
            reply.setError(notOwner(msg.getServerUuid()));
            bus.reply(msg, reply);
            return;
        }
        reply.setSynced(localResourceControlExecutor.release(msg.getCommand()));
        bus.reply(msg, reply);
    }

    private void handle(CollectManagementNodeManagedServicesMsg msg) {
        CollectManagementNodeManagedServicesReply reply = new CollectManagementNodeManagedServicesReply();
        if (!owns(msg.getServerUuid())) {
            reply.setError(notOwner(msg.getServerUuid()));
            bus.reply(msg, reply);
            return;
        }
        List<ManagedServiceResourceUsage> services = localResourceControlExecutor.inspect(
                type.toString(), msg.getSliceName(), msg.getHandles());
        for (ManagedServiceResourceUsage service : services) {
            service.setRoleType(type.toString());
        }
        reply.setServices(services);
        bus.reply(msg, reply);
    }

    private void handle(RestartManagementNodeManagedServicesMsg msg) {
        MessageReply reply = new MessageReply();
        if (!owns(msg.getServerUuid())) {
            reply.setError(notOwner(msg.getServerUuid()));
            bus.reply(msg, reply);
            return;
        }
        localResourceControlExecutor.restart(msg.getSliceName(), msg.getConsumers());
        bus.reply(msg, reply);
    }

    @Override
    public PhysicalServerResourceAssignmentObserver getResourceAssignment(String serverUuid) {
        return new ManagementNodeResourceAssignmentController(serverUuid);
    }

    @Override
    public PhysicalServerRoleType getRoleType() {
        return type;
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        if (physicalServerManager != null && inv.getServerUuid() != null) {
            refreshResourceAssignment(inv.getServerUuid());
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

    public void associateLocalNode(String nodeUuid) {
        if (physicalServerManager == null || !Platform.getManagementServerId().equals(nodeUuid)) {
            return;
        }
        String serverUuid = associateNode(nodeUuid);
        if (serverUuid != null) {
            refreshResourceAssignment(serverUuid);
        }
    }

    @Transactional
    private String associateNode(String nodeUuid) {
        String current = Q.New(ManagementNodeVO.class)
                .select(ManagementNodeVO_.serverUuid).eq(ManagementNodeVO_.uuid, nodeUuid).findValue();
        if (current == null) {
            if (!PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value(Boolean.class)) {
                return null;
            }
            String serialNumber = managementServerSerialNumber();
            if (serialNumber == null) {
                logger.warn(String.format("cannot associate management node[uuid:%s] with a physical server, " +
                                "machine serial number is unavailable", nodeUuid));
                return null;
            }
            current = physicalServerManager.resolveBySerialNumbers(
                    Collections.singleton(serialNumber)).get(serialNumber);
            if (current == null || linkedNode(current, nodeUuid) != null) {
                return null;
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
        return current;
    }

    private void refreshResourceAssignment(String serverUuid) {
        physicalServerManager.refreshResourceAssignment(serverUuid, type.toString(), new Completion(null) {
            @Override
            public void success() {
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format(
                        "failed to refresh MANAGEMENT resource assignment for physical server[uuid:%s]: %s",
                        serverUuid, errorCode));
            }
        });
    }

    private String linkedNode(String serverUuid, String excludedNodeUuid) {
        return Q.New(ManagementNodeVO.class)
                .select(ManagementNodeVO_.uuid)
                .eq(ManagementNodeVO_.serverUuid, serverUuid)
                .notEq(ManagementNodeVO_.uuid, excludedNodeUuid).findValue();
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

    public RoleServiceManifest roleServices() {
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
        PhysicalServerResourceAssignmentGlobalConfig.ENABLED.installUpdateExtension((oldConfig, newConfig) -> {
            if (newConfig.value(Boolean.class)) {
                associateLocalNode(Platform.getManagementServerId());
            }
        });
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
