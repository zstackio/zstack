package org.zstack.physicalserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
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
import java.util.concurrent.Future;
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
    private PhysicalServerResourceAssignmentReconciler reconciler;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private PluginRegistry pluginRgty;
    private Future<Void> reconcileTask;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else if (msg instanceof ReconcilePhysicalServerMsg) {
            ReconcilePhysicalServerMsg rmsg = (ReconcilePhysicalServerMsg) msg;
            if (rmsg.getOperation()
                    == ReconcilePhysicalServerMsg.Operation.REFRESH_AND_RECONCILE) {
                reconciler.refreshAndEnqueue(rmsg.getServerUuid());
            } else {
                reconciler.enqueue(rmsg.getServerUuid());
            }
        } else if (msg instanceof ReconcileAllPhysicalServersMsg) {
            reconciler.enqueueAll();
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
            reconciler.forceReleaseAssignment(
                    msg.getServerUuid(),
                    msg.getRoleType(),
                    msg.getConsumerUuid(),
                    completion);
        } else {
            reconciler.releaseAssignment(
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
        reconciler.collectManagedServiceUsage(
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
        reconciler.updateAssignment(
                msg,
                new ReturnValueCompletion<PhysicalServerResourceAssignmentInventory>(msg) {
            @Override
            public void success(PhysicalServerResourceAssignmentInventory inventory) {
                event.setInventory(inventory);
                refreshAndReconcile(msg.getServerUuid());
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
        if (msg.getServiceNames() != null && !msg.getServiceNames().isEmpty()) {
            reconciler.restartManagedServices(msg, new Completion(msg) {
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
            return;
        }
        refreshAndReconcile(msg.getServerUuid());
        bus.publish(event);
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
    public void ensureResourceAssignments(
            Collection<String> serverUuids, String roleType) {
        reconciler.ensureResourceAssignments(serverUuids, roleType);
    }

    @Override
    public void reconcile(String serverUuid) {
        if (serverUuid == null) {
            return;
        }
        ReconcilePhysicalServerMsg msg = new ReconcilePhysicalServerMsg();
        msg.setServerUuid(serverUuid);
        sendReconcile(msg);
    }

    @Override
    public void refreshAndReconcile(String serverUuid) {
        if (serverUuid == null) {
            return;
        }
        ReconcilePhysicalServerMsg msg = new ReconcilePhysicalServerMsg();
        msg.setServerUuid(serverUuid);
        msg.setOperation(
                ReconcilePhysicalServerMsg.Operation.REFRESH_AND_RECONCILE);
        sendReconcile(msg);
    }

    private void sendReconcile(ReconcilePhysicalServerMsg msg) {
        bus.makeTargetServiceIdByResourceUuid(
                msg,
                PhysicalServerConstant.SERVICE_ID,
                PhysicalServerConstant.CONTROL_OWNER_KEY);
        bus.send(msg);
    }

    @Override
    public void reconcileAll() {
        ReconcileAllPhysicalServersMsg msg = new ReconcileAllPhysicalServersMsg();
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
                .installUpdateExtension((oldConfig, newConfig) -> reconcileAll());
        return true;
    }

    @Override
    public void managementNodeReady() {
        reconcileAllIfOwned();
        reconcileTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.MILLISECONDS;
            }

            @Override
            public long getInterval() {
                return PhysicalServerConstant.RECONCILE_INTERVAL_MILLIS;
            }

            @Override
            public String getName() {
                return "physical-server-resource-assignment-reconcile";
            }

            @Override
            public void run() {
                reconcileAllIfOwned();
            }
        });
    }

    private void reconcileAllIfOwned() {
        if (destinationMaker.isManagedByUs(PhysicalServerConstant.CONTROL_OWNER_KEY)) {
            reconcileAll();
        }
    }

    @Override
    public boolean stop() {
        if (reconcileTask != null) {
            reconcileTask.cancel(true);
        }
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(PhysicalServerConstant.SERVICE_ID);
    }
}
