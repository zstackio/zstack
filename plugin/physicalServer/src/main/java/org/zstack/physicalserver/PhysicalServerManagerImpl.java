package org.zstack.physicalserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.physicalserver.PhysicalServerManager;
import org.zstack.header.physicalserver.ManagedServiceResourceUsage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PhysicalServerManagerImpl extends AbstractService implements
        PhysicalServerManager,
        Component,
        ManagementNodeReadyExtensionPoint {
    @Autowired
    private CloudBus bus;
    @Autowired
    private PhysicalServerIdentityService identity;
    @Autowired
    private PhysicalServerResourceAssignmentService assignmentService;
    @Autowired
    private ResourceDestinationMaker destinationMaker;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else if (msg instanceof PhysicalServerAssociationChangedMsg) {
            assignmentService.refreshAssignments(
                    ((PhysicalServerAssociationChangedMsg) msg).getServerUuid());
        } else if (msg instanceof ReleasePhysicalServerResourceAssignmentMsg) {
            handle((ReleasePhysicalServerResourceAssignmentMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(ReleasePhysicalServerResourceAssignmentMsg msg) {
        MessageReply reply = new MessageReply();
        Completion completion = new Completion(msg) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                };
        if (msg.getOperation()
                == ReleasePhysicalServerResourceAssignmentMsg.Operation.FORCE_RELEASE) {
            assignmentService.forceReleaseAssignment(
                    msg.getServerUuid(),
                    msg.getRoleType(),
                    msg.getConsumerUuid(),
                    completion);
        } else {
            assignmentService.releaseAssignment(
                    msg.getServerUuid(),
                    msg.getRoleType(),
                    msg.getConsumerUuid(),
                    completion);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdatePhysicalServerResourceAssignmentMsg) {
            handle((APIUpdatePhysicalServerResourceAssignmentMsg) msg);
        } else if (msg instanceof APIRefreshPhysicalServerResourceAssignmentsMsg) {
            handle((APIRefreshPhysicalServerResourceAssignmentsMsg) msg);
        } else if (msg instanceof APIRestartPhysicalServerManagedServicesMsg) {
            handle((APIRestartPhysicalServerManagedServicesMsg) msg);
        } else if (msg instanceof APIGetPhysicalServerManagedServicesMsg) {
            handle((APIGetPhysicalServerManagedServicesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(
            APIGetPhysicalServerManagedServicesMsg msg) {
        APIGetPhysicalServerManagedServicesReply reply =
                new APIGetPhysicalServerManagedServicesReply();
        assignmentService.collectManagedServiceUsage(
                msg.getServerUuid(),
                new ReturnValueCompletion<List<ManagedServiceResourceUsage>>(msg) {
                    @Override
                    public void success(
                            List<ManagedServiceResourceUsage> usages) {
                        reply.setServices(
                                PhysicalServerManagedServiceInventory.valueOf(usages));
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
    }

    private void handle(APIUpdatePhysicalServerResourceAssignmentMsg msg) {
        APIUpdatePhysicalServerResourceAssignmentEvent event =
                new APIUpdatePhysicalServerResourceAssignmentEvent(msg.getId());
        assignmentService.updateAssignment(
                msg,
                new ReturnValueCompletion<PhysicalServerResourceAssignmentInventory>(msg) {
            @Override
            public void success(PhysicalServerResourceAssignmentInventory inventory) {
                event.setInventory(inventory);
                assignmentService.requestAssignmentProcessing(
                        msg.getServerUuid());
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void handle(APIRefreshPhysicalServerResourceAssignmentsMsg msg) {
        APIRefreshPhysicalServerResourceAssignmentsEvent event =
                new APIRefreshPhysicalServerResourceAssignmentsEvent(msg.getId());
        assignmentService.refreshAssignments(msg.getServerUuid());
        bus.publish(event);
    }

    private void handle(APIRestartPhysicalServerManagedServicesMsg msg) {
        APIRestartPhysicalServerManagedServicesEvent event =
                new APIRestartPhysicalServerManagedServicesEvent(msg.getId());
        assignmentService.restartManagedServices(msg, new Completion(msg) {
            @Override
            public void success() {
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    @Override
    public Map<String, String> resolveBySerialNumbers(
            Collection<String> serialNumbers) {
        return identity.resolveBySerialNumbers(serialNumbers);
    }

    @Override
    public Map<String, String> findSerialNumbersByServerUuids(
            Collection<String> serverUuids) {
        return identity.findSerialNumbersByServerUuids(serverUuids);
    }

    @Override
    public void associationChanged(String serverUuid) {
        if (serverUuid == null) {
            return;
        }
        PhysicalServerAssociationChangedMsg msg =
                new PhysicalServerAssociationChangedMsg();
        msg.setServerUuid(serverUuid);
        bus.makeTargetServiceIdByResourceUuid(
                msg,
                PhysicalServerConstant.SERVICE_ID,
                PhysicalServerConstant.CONTROL_OWNER_KEY);
        bus.send(msg);
    }

    @Override
    public void releaseResourceAssignment(
            String serverUuid,
            String roleType,
            String consumerUuid,
            Completion completion) {
        ReleasePhysicalServerResourceAssignmentMsg msg =
                new ReleasePhysicalServerResourceAssignmentMsg();
        msg.setServerUuid(serverUuid);
        msg.setRoleType(roleType);
        msg.setConsumerUuid(consumerUuid);
        sendRelease(msg, completion);
    }

    @Override
    public void forceReleaseResourceAssignment(
            String serverUuid,
            String roleType,
            String consumerUuid,
            Completion completion) {
        ReleasePhysicalServerResourceAssignmentMsg msg =
                new ReleasePhysicalServerResourceAssignmentMsg();
        msg.setServerUuid(serverUuid);
        msg.setRoleType(roleType);
        msg.setConsumerUuid(consumerUuid);
        msg.setOperation(
                ReleasePhysicalServerResourceAssignmentMsg.Operation.FORCE_RELEASE);
        sendRelease(msg, completion);
    }

    private void sendRelease(
            ReleasePhysicalServerResourceAssignmentMsg msg,
            Completion completion) {
        msg.setTimeout(TimeUnit.MINUTES.toMillis(5));
        bus.makeTargetServiceIdByResourceUuid(
                msg,
                PhysicalServerConstant.SERVICE_ID,
                PhysicalServerConstant.CONTROL_OWNER_KEY);
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
    public boolean start() {
        PhysicalServerResourceAssignmentGlobalConfig.ENABLED
                .installUpdateExtension((oldConfig, newConfig) -> discoverAllIfOwned());
        return true;
    }

    @Override
    public void managementNodeReady() {
        discoverAllIfOwned();
    }

    private void discoverAllIfOwned() {
        if (destinationMaker.isManagedByUs(PhysicalServerConstant.CONTROL_OWNER_KEY)) {
            assignmentService.refreshAllAssignments();
        }
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(PhysicalServerConstant.SERVICE_ID);
    }
}
