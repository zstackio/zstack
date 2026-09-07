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
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.physicalserver.PhysicalServerManager;
import org.zstack.header.physicalserver.RoleServiceManifest;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

public class PhysicalServerManagerImpl extends AbstractService implements
        PhysicalServerManager, Component, ManagementNodeReadyExtensionPoint {
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
        } else if (msg instanceof RefreshPhysicalServerResourceAssignmentMsg) {
            handle((RefreshPhysicalServerResourceAssignmentMsg) msg);
        } else if (msg instanceof ReleasePhysicalServerResourceAssignmentMsg) {
            handle((ReleasePhysicalServerResourceAssignmentMsg) msg);
        } else if (msg instanceof ForgetPhysicalServerResourceAssignmentMsg) {
            handle((ForgetPhysicalServerResourceAssignmentMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(RefreshPhysicalServerResourceAssignmentMsg msg) {
        MessageReply reply = new MessageReply();
        assignmentService.refreshResourceAssignment(
                msg.getServerUuid(), msg.getRoleType(), replyCompletion(msg, reply));
    }

    private void handle(ReleasePhysicalServerResourceAssignmentMsg msg) {
        MessageReply reply = new MessageReply();
        assignmentService.releaseAssignment(msg.getServerUuid(), msg.getRoleType(), replyCompletion(msg, reply));
    }

    private void handle(ForgetPhysicalServerResourceAssignmentMsg msg) {
        MessageReply reply = new MessageReply();
        assignmentService.forgetAssignment(msg.getServerUuid(), msg.getRoleType(), replyCompletion(msg, reply));
    }

    private Completion replyCompletion(NeedReplyMessage msg, MessageReply reply) {
        return new Completion(msg) {
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
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdatePhysicalServerResourceAssignmentMsg) {
            handle((APIUpdatePhysicalServerResourceAssignmentMsg) msg);
        } else if (msg instanceof APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg) {
            handle((APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg) msg);
        } else if (msg instanceof APIRestartPhysicalServerManagedServicesMsg) {
            handle((APIRestartPhysicalServerManagedServicesMsg) msg);
        } else if (msg instanceof APIGetPhysicalServerManagedServicesMsg) {
            handle((APIGetPhysicalServerManagedServicesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetPhysicalServerManagedServicesMsg msg) {
        APIGetPhysicalServerManagedServicesReply reply = new APIGetPhysicalServerManagedServicesReply();
        assignmentService.collectManagedServiceUsage(
                msg.getServerUuid(), new ReturnValueCompletion<
                        PhysicalServerResourceAssignmentService.ManagedServiceUsageResult>(msg) {
                    @Override
                    public void success(PhysicalServerResourceAssignmentService.ManagedServiceUsageResult result) {
                        reply.setServices(PhysicalServerManagedServiceInventory.valueOf(result.getServices()));
                        reply.setRoleErrors(result.getRoleErrors());
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
                msg, new ReturnValueCompletion<PhysicalServerResourceAssignmentInventory>(msg) {
            @Override
            public void success(PhysicalServerResourceAssignmentInventory inventory) {
                event.setInventory(inventory);
                assignmentService.applyResourceAssignment(msg.getServerUuid(), msg.getRoleType());
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void handle(APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg msg) {
        APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent event =
                new APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent(msg.getId());
        RoleServiceManifest.reloadAll();
        assignmentService.refreshAssignmentsFromProfile(msg.getServerUuids());
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
    public Map<String, String> resolveBySerialNumbers(Collection<String> serialNumbers) {
        return identity.resolveBySerialNumbers(serialNumbers);
    }

    @Override
    public void refreshResourceAssignment(String serverUuid, String roleType, Completion completion) {
        if (serverUuid == null || roleType == null) {
            completion.fail(operr(
                    PhysicalServerConstant.ERROR_CODE, "serverUuid and roleType cannot be null"));
            return;
        }
        RefreshPhysicalServerResourceAssignmentMsg msg = new RefreshPhysicalServerResourceAssignmentMsg();
        msg.setServerUuid(serverUuid);
        msg.setRoleType(roleType);
        sendResourceAssignmentMessage(msg, completion);
    }

    @Override
    public void releaseResourceAssignment(String serverUuid, String roleType, Completion completion) {
        ReleasePhysicalServerResourceAssignmentMsg msg = new ReleasePhysicalServerResourceAssignmentMsg();
        msg.setServerUuid(serverUuid);
        msg.setRoleType(roleType);
        sendResourceAssignmentMessage(msg, completion);
    }

    @Override
    public void forgetResourceAssignment(String serverUuid, String roleType, Completion completion) {
        ForgetPhysicalServerResourceAssignmentMsg msg = new ForgetPhysicalServerResourceAssignmentMsg();
        msg.setServerUuid(serverUuid);
        msg.setRoleType(roleType);
        sendResourceAssignmentMessage(msg, completion);
    }

    private void sendResourceAssignmentMessage(NeedReplyMessage msg, Completion completion) {
        msg.setTimeout(TimeUnit.MINUTES.toMillis(5));
        bus.makeTargetServiceIdByResourceUuid(
                msg, PhysicalServerConstant.SERVICE_ID, PhysicalServerConstant.CONTROL_OWNER_KEY);
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
        PhysicalServerResourceAssignmentGlobalConfig.ENABLED.installUpdateExtension((oldConfig, newConfig) -> {
            if (!destinationMaker.isManagedByUs(PhysicalServerConstant.CONTROL_OWNER_KEY)) {
                return;
            }
            if (newConfig.value(Boolean.class)) {
                assignmentService.discoverAllAssignments();
            } else {
                assignmentService.releaseAllAssignments();
            }
        });
        return true;
    }

    @Override
    public void managementNodeReady() {
        discoverAllIfOwned();
    }

    private void discoverAllIfOwned() {
        if (PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value(Boolean.class)
                && destinationMaker.isManagedByUs(PhysicalServerConstant.CONTROL_OWNER_KEY)) {
            assignmentService.discoverAllAssignments();
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
