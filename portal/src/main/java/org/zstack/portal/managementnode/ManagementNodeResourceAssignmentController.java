package org.zstack.portal.managementnode;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.physicalserver.*;
import static org.zstack.core.Platform.operr;
import static org.zstack.portal.managementnode.ManagementNodeResourceAssignmentFactory.*;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_PORTAL_MANAGEMENTNODE_10000;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class ManagementNodeResourceAssignmentController implements PhysicalServerResourceAssignmentController,
        PhysicalServerResourceUsageObserver {
    @Autowired
    private CloudBus bus;

    private final String nodeUuid;

    public ManagementNodeResourceAssignmentController(String serverUuid) {
        nodeUuid = Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid)
                .eq(ManagementNodeVO_.serverUuid, serverUuid).findValue();
    }

    @Override
    public PhysicalServerRoleType getRoleType() {
        return type;
    }

    @Override
    public boolean resourceExists() {
        return nodeUuid != null;
    }

    @Override
    public void collectTopology(String serverUuid, ReturnValueCompletion<PhysicalServerCpuTopology> completion) {
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
        ApplyManagementNodeResourceControlMsg msg = new ApplyManagementNodeResourceControlMsg();
        msg.setServerUuid(serverUuid);
        msg.setCommand(command);
        sendResourceControl(serverUuid, msg, completion);
    }

    @Override
    public void release(String serverUuid, ResourceControlCommand command, ReturnValueCompletion<Boolean> completion) {
        ReleaseManagementNodeResourceControlMsg msg = new ReleaseManagementNodeResourceControlMsg();
        msg.setServerUuid(serverUuid);
        msg.setCommand(command);
        sendResourceControl(serverUuid, msg, completion);
    }

    @Override
    public void collectManagedServiceUsage(
            String serverUuid, ResourceControlCommand requested,
            ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion) {
        if (nodeUuid == null) {
            completion.fail(notOwner(serverUuid));
            return;
        }
        CollectManagementNodeManagedServicesMsg msg = new CollectManagementNodeManagedServicesMsg();
        msg.setServerUuid(serverUuid);
        msg.setSliceName(requested.getSliceName());
        msg.setHandles(requested.getHandles());
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
    public void restartManagedServices(String serverUuid, ResourceControlCommand requested, Completion completion) {
        if (nodeUuid == null) {
            completion.fail(notOwner(serverUuid));
            return;
        }
        RestartManagementNodeManagedServicesMsg msg = new RestartManagementNodeManagedServicesMsg();
        msg.setServerUuid(serverUuid);
        msg.setSliceName(requested.getSliceName());
        msg.setConsumers(requested.getHandles());
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


    private void sendResourceControl(
            String serverUuid, NeedReplyMessage msg, ReturnValueCompletion<Boolean> completion) {
        if (nodeUuid == null) {
            completion.fail(notOwner(serverUuid));
            return;
        }
        msg.setTimeout(TimeUnit.MINUTES.toMillis(5));
        bus.makeServiceIdByManagementNodeId(msg, SERVICE_ID, nodeUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                ManagementNodeResourceControlReply applyReply = reply.castReply();
                completion.success(applyReply.isSynced());
            }
        });
    }

    private ErrorCode notOwner(String serverUuid) {
        return operr(ORG_ZSTACK_PORTAL_MANAGEMENTNODE_10000,
                "management node[uuid:%s] is not associated with physical server[uuid:%s]",
                Platform.getManagementServerId(), serverUuid);
    }
}
