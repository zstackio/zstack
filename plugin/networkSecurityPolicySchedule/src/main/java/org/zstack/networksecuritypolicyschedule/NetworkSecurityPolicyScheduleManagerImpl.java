package org.zstack.networksecuritypolicyschedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.networksecuritypolicyschedule.NetworkSecurityPolicyScheduleResourceBackend.Operation;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10006;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10010;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10011;

public class NetworkSecurityPolicyScheduleManagerImpl extends AbstractService {
    private static final CLogger logger = Utils.getLogger(NetworkSecurityPolicyScheduleManagerImpl.class);
    private static final String SCHEDULE_INVENTORY = "scheduleInventory";
    private static final String TIME_CHANGED = "timeChanged";
    private static final String CURRENT_SCHEDULE = "currentSchedule";

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CascadeFacade casf;
    @Autowired
    private NetworkSecurityPolicyScheduleFacade scheduleFacade;
    @Autowired
    private NetworkSecurityPolicyScheduleResourceBackendRegistry backendRegistry;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateNetworkSecurityPolicyScheduleMsg) {
            handle((APICreateNetworkSecurityPolicyScheduleMsg) msg);
        } else if (msg instanceof APIUpdateNetworkSecurityPolicyScheduleMsg) {
            handle((APIUpdateNetworkSecurityPolicyScheduleMsg) msg);
        } else if (msg instanceof APIDeleteNetworkSecurityPolicyScheduleMsg) {
            handle((APIDeleteNetworkSecurityPolicyScheduleMsg) msg);
        } else if (msg instanceof APIGetNetworkSecurityPolicyScheduleMsg) {
            handle((APIGetNetworkSecurityPolicyScheduleMsg) msg);
        } else if (msg instanceof APISetNetworkSecurityPolicyScheduleMsg) {
            handle((APISetNetworkSecurityPolicyScheduleMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetNetworkSecurityPolicyScheduleMsg msg) {
        APIGetNetworkSecurityPolicyScheduleReply reply = new APIGetNetworkSecurityPolicyScheduleReply();
        Q query = Q.New(NetworkSecurityPolicyScheduleVO.class)
                .eq(NetworkSecurityPolicyScheduleVO_.resourceUuid, msg.getResourceUuid());
        if (msg.getRepeatType() != null) {
            query.eq(NetworkSecurityPolicyScheduleVO_.repeatType,
                    NetworkSecurityPolicyScheduleRepeatType.valueOf(msg.getRepeatType()));
        }
        if (msg.getTimeType() != null) {
            query.eq(NetworkSecurityPolicyScheduleVO_.timeType,
                    NetworkSecurityPolicyScheduleTimeType.valueOf(msg.getTimeType()));
        }

        List<NetworkSecurityPolicyScheduleVO> vos = query.list();
        vos.sort(Comparator.comparing(NetworkSecurityPolicyScheduleVO::getCreateDate)
                .thenComparing(NetworkSecurityPolicyScheduleVO::getUuid));
        List<NetworkSecurityPolicyScheduleInventory> inventories =
                NetworkSecurityPolicyScheduleInventory.valueOf(vos, scheduleFacade.now());
        if (msg.getTimeStatus() != null) {
            NetworkSecurityPolicyScheduleTimeStatus status =
                    NetworkSecurityPolicyScheduleTimeStatus.valueOf(msg.getTimeStatus());
            inventories.removeIf(inventory -> inventory.getTimeStatus() != status);
        }
        reply.setInventories(inventories);
        bus.reply(msg, reply);
    }

    private String getScheduleSyncSignature(String resourceType, String resourceUuid) {
        return String.format("NetworkSecurityPolicySchedule-%s-%s", resourceType, resourceUuid);
    }

    private void handle(APICreateNetworkSecurityPolicyScheduleMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getScheduleSyncSignature(msg.getResourceType(), msg.getResourceUuid());
            }

            @Override
            public void run(SyncTaskChain taskChain) {
                APICreateNetworkSecurityPolicyScheduleEvent evt =
                        new APICreateNetworkSecurityPolicyScheduleEvent(msg.getId());
                FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                chain.setName("create-network-security-policy-schedule");
                chain.then(new Flow() {
                    String __name__ = "save-schedule";
                    private String scheduleUuid;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        NetworkSecurityPolicyScheduleVO vo = new NetworkSecurityPolicyScheduleVO();
                        scheduleUuid = Platform.getUuid();
                        vo.setUuid(scheduleUuid);
                        vo.setName(msg.getName());
                        vo.setDescription(msg.getDescription());
                        vo.setResourceType(msg.getResourceType());
                        vo.setResourceUuid(msg.getResourceUuid());
                        scheduleTimeOf(msg).applyTo(vo);
                        vo = dbf.persistAndRefresh(vo);
                        data.put(SCHEDULE_INVENTORY,
                                NetworkSecurityPolicyScheduleInventory.valueOf(vo, scheduleFacade.now()));
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (scheduleUuid != null) {
                            dbf.removeByPrimaryKey(scheduleUuid, NetworkSecurityPolicyScheduleVO.class);
                        }
                        trigger.rollback();
                    }
                }).done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory((NetworkSecurityPolicyScheduleInventory) data.get(SCHEDULE_INVENTORY));
                        bus.publish(evt);
                        taskChain.next();
                    }
                }).error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errorCode, Map data) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        taskChain.next();
                    }
                }).start();
            }

            @Override
            public String getName() {
                return "create-network-security-policy-schedule";
            }
        });
    }

    private void handle(APIUpdateNetworkSecurityPolicyScheduleMsg msg) {
        APIUpdateNetworkSecurityPolicyScheduleEvent evt =
                new APIUpdateNetworkSecurityPolicyScheduleEvent(msg.getId());
        NetworkSecurityPolicyScheduleVO schedule = dbf.findByUuid(
                msg.getUuid(), NetworkSecurityPolicyScheduleVO.class);
        if (schedule == null) {
            evt.setError(scheduleNotFound(msg.getUuid()));
            bus.publish(evt);
            return;
        }
        String resourceType = schedule.getResourceType();
        String resourceUuid = schedule.getResourceUuid();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getScheduleSyncSignature(resourceType, resourceUuid);
            }

            @Override
            public void run(SyncTaskChain taskChain) {
                FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                chain.setName("update-network-security-policy-schedule");
                chain.then(new Flow() {
                    String __name__ = "update-schedule";
                    private String oldName;
                    private String oldDescription;
                    private NetworkSecurityPolicyScheduleTime oldTime;
                    private boolean timeChanged;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        NetworkSecurityPolicyScheduleVO vo = dbf.findByUuid(
                                msg.getUuid(), NetworkSecurityPolicyScheduleVO.class);
                        if (vo == null) {
                            trigger.fail(scheduleNotFound(msg.getUuid()));
                            return;
                        }
                        oldName = vo.getName();
                        oldDescription = vo.getDescription();
                        oldTime = NetworkSecurityPolicyScheduleTime.valueOf(vo);
                        NetworkSecurityPolicyScheduleTime newTime = scheduleTimeOf(msg);
                        timeChanged = !newTime.equals(oldTime);

                        vo.setName(msg.getName());
                        vo.setDescription(msg.getDescription());
                        newTime.applyTo(vo);
                        vo = dbf.updateAndRefresh(vo);
                        data.put(SCHEDULE_INVENTORY,
                                NetworkSecurityPolicyScheduleInventory.valueOf(vo, scheduleFacade.now()));
                        data.put(TIME_CHANGED, timeChanged);
                        data.put(CURRENT_SCHEDULE, Objects.equals(
                                msg.getUuid(), scheduleUuidOf(
                                        resourceType, resourceUuid)));
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (oldTime == null) {
                            trigger.rollback();
                            return;
                        }
                        NetworkSecurityPolicyScheduleVO vo = dbf.findByUuid(
                                msg.getUuid(), NetworkSecurityPolicyScheduleVO.class);
                        if (vo == null) {
                            trigger.rollback();
                            return;
                        }
                        vo.setName(oldName);
                        vo.setDescription(oldDescription);
                        oldTime.applyTo(vo);
                        dbf.update(vo);

                        if (!timeChanged || !Boolean.TRUE.equals(data.get(CURRENT_SCHEDULE))) {
                            trigger.rollback();
                            return;
                        }
                        changeSchedule(resourceType, resourceUuid,
                                msg.getUuid(), Operation.REFRESH,
                                false, msg.getTimeout(),
                                new Completion(trigger) {
                                    @Override
                                    public void success() {
                                        trigger.rollback();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        logger.warn(String.format(
                                                "failed to restore %s[uuid:%s] after schedule update failed, %s",
                                                resourceType, resourceUuid, errorCode));
                                        trigger.rollback();
                                    }
                                });
                    }
                });
                chain.then(new NoRollbackFlow() {
                    String __name__ = "refresh-rules";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (!Boolean.TRUE.equals(data.get(TIME_CHANGED))
                                || !Boolean.TRUE.equals(data.get(CURRENT_SCHEDULE))) {
                            trigger.next();
                            return;
                        }
                        changeSchedule(resourceType, resourceUuid,
                                msg.getUuid(), Operation.REFRESH,
                                completionOf(trigger));
                    }
                }).done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory((NetworkSecurityPolicyScheduleInventory) data.get(SCHEDULE_INVENTORY));
                        bus.publish(evt);
                        taskChain.next();
                    }
                }).error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errorCode, Map data) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        taskChain.next();
                    }
                }).start();
            }

            @Override
            public String getName() {
                return "update-network-security-policy-schedule";
            }
        });
    }

    private void handle(APISetNetworkSecurityPolicyScheduleMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getScheduleSyncSignature(msg.getResourceType(), msg.getResourceUuid());
            }

            @Override
            public void run(SyncTaskChain taskChain) {
                APISetNetworkSecurityPolicyScheduleEvent evt =
                        new APISetNetworkSecurityPolicyScheduleEvent(msg.getId());
                FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                chain.setName("set-network-security-policy-schedule");
                chain.then(new NoRollbackFlow() {
                    String __name__ = "validate-schedule-owner";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        boolean current = Objects.equals(
                                msg.getScheduleUuid(),
                                scheduleUuidOf(msg.getResourceType(), msg.getResourceUuid()));
                        data.put(CURRENT_SCHEDULE, current);
                        if (current || msg.getScheduleUuid() == null) {
                            trigger.next();
                            return;
                        }

                        NetworkSecurityPolicyScheduleVO vo = dbf.findByUuid(
                                msg.getScheduleUuid(), NetworkSecurityPolicyScheduleVO.class);
                        if (vo == null) {
                            trigger.fail(scheduleNotFound(msg.getScheduleUuid()));
                            return;
                        }
                        if (!belongsTo(vo, msg.getResourceType(), msg.getResourceUuid())) {
                            trigger.fail(scheduleOwnerMismatch(
                                    msg.getScheduleUuid(),
                                    msg.getResourceType(), msg.getResourceUuid()));
                            return;
                        }
                        trigger.next();
                    }
                });
                chain.then(new NoRollbackFlow() {
                    String __name__ = "set-schedule";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (Boolean.TRUE.equals(data.get(CURRENT_SCHEDULE))) {
                            trigger.next();
                            return;
                        }
                        changeSchedule(msg.getResourceType(), msg.getResourceUuid(),
                                msg.getScheduleUuid(), Operation.SET,
                                completionOf(trigger));
                    }
                }).done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.publish(evt);
                        taskChain.next();
                    }
                }).error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errorCode, Map data) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        taskChain.next();
                    }
                }).start();
            }

            @Override
            public String getName() {
                return "set-network-security-policy-schedule";
            }
        });
    }

    private void handle(APIDeleteNetworkSecurityPolicyScheduleMsg msg) {
        APIDeleteNetworkSecurityPolicyScheduleEvent evt =
                new APIDeleteNetworkSecurityPolicyScheduleEvent(msg.getId());
        NetworkSecurityPolicyScheduleVO schedule = dbf.findByUuid(
                msg.getUuid(), NetworkSecurityPolicyScheduleVO.class);
        if (schedule == null) {
            bus.publish(evt);
            return;
        }
        doDeleteNetworkSecurityPolicySchedule(
                msg.getUuid(), schedule.getResourceType(), schedule.getResourceUuid(),
                msg.getDeletionMode(), new Completion(msg) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                    }
                });
    }

    private void doDeleteNetworkSecurityPolicySchedule(String scheduleUuid,
                                             String resourceType,
                                             String resourceUuid,
                                             APIDeleteMessage.DeletionMode mode,
                                             Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return getScheduleSyncSignature(resourceType, resourceUuid);
            }

            @Override
            public void run(SyncTaskChain taskChain) {
                String issuer = NetworkSecurityPolicyScheduleVO.class.getSimpleName();
                NetworkSecurityPolicyScheduleVO vo = dbf.findByUuid(
                        scheduleUuid, NetworkSecurityPolicyScheduleVO.class);
                if (vo == null) {
                    completion.success();
                    taskChain.next();
                    return;
                }
                if (resourceUuid != null && !belongsTo(vo, resourceType, resourceUuid)) {
                    completion.fail(scheduleOwnerMismatch(
                            scheduleUuid, resourceType, resourceUuid));
                    taskChain.next();
                    return;
                }

                List<NetworkSecurityPolicyScheduleInventory> scheduleInventories = Collections.singletonList(
                        NetworkSecurityPolicyScheduleInventory.valueOf(vo, scheduleFacade.now()));
                boolean currentSchedule = Objects.equals(
                        scheduleUuid, scheduleUuidOf(resourceType, resourceUuid));
                FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                chain.setName("delete-network-security-policy-schedule");
                chain.then(new NoRollbackFlow() {
                    String __name__ = "check-cascade";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (mode == APIDeleteMessage.DeletionMode.Enforcing) {
                            trigger.next();
                            return;
                        }
                        casf.asyncCascade(
                                CascadeConstant.DELETION_CHECK_CODE, issuer, scheduleInventories,
                                completionOf(trigger));
                    }
                });
                chain.then(new Flow() {
                    String __name__ = "unset-current-schedule";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (!currentSchedule) {
                            trigger.next();
                            return;
                        }
                        changeSchedule(resourceType, resourceUuid, null,
                                Operation.SET, true, completionOf(trigger));
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (!currentSchedule) {
                            trigger.rollback();
                            return;
                        }
                        changeSchedule(resourceType, resourceUuid, scheduleUuid,
                                Operation.SET, new Completion(trigger) {
                                    @Override
                                    public void success() {
                                        trigger.rollback();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        logger.warn(String.format(
                                                "failed to restore schedule[uuid:%s] for %s[uuid:%s], %s",
                                                scheduleUuid, resourceType, resourceUuid, errorCode));
                                        trigger.rollback();
                                    }
                                });
                    }
                });
                chain.then(new NoRollbackFlow() {
                    String __name__ = "delete-cascade";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String actionCode = mode == APIDeleteMessage.DeletionMode.Enforcing
                                ? CascadeConstant.DELETION_FORCE_DELETE_CODE
                                : CascadeConstant.DELETION_DELETE_CODE;
                        casf.asyncCascade(
                                actionCode, issuer, scheduleInventories, completionOf(trigger));
                    }
                });
                chain.then(new NoRollbackFlow() {
                    String __name__ = "delete-schedule";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        dbf.removeByPrimaryKey(scheduleUuid, NetworkSecurityPolicyScheduleVO.class);
                        trigger.next();
                    }
                }).done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        casf.asyncCascadeFull(
                                CascadeConstant.DELETION_CLEANUP_CODE,
                                issuer, scheduleInventories, new NopeCompletion());
                        completion.success();
                        taskChain.next();
                    }
                }).error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errorCode, Map data) {
                        completion.fail(errorCode);
                        taskChain.next();
                    }
                }).start();
            }

            @Override
            public String getName() {
                return "delete-network-security-policy-schedule";
            }
        });
    }

    private String scheduleUuidOf(String resourceType, String resourceUuid) {
        return resourceBackend(resourceType).getScheduleUuid(resourceUuid);
    }

    private boolean belongsTo(NetworkSecurityPolicyScheduleVO vo,
                              String resourceType,
                              String resourceUuid) {
        return resourceType.equals(vo.getResourceType())
                && resourceUuid.equals(vo.getResourceUuid());
    }

    private OperationFailureException unsupportedResourceType(String resourceType) {
        return new OperationFailureException(operr(
                ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10011,
                "unsupported resourceType[%s]", resourceType));
    }

    private ErrorCode scheduleOwnerMismatch(String scheduleUuid,
                                             String resourceType,
                                             String resourceUuid) {
        return argerr(
                ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10010,
                "network security policy schedule[uuid:%s] does not belong to %s[uuid:%s]",
                scheduleUuid, resourceType, resourceUuid);
    }

    private NeedReplyMessage makeScheduleMessage(String resourceType,
                                                  String resourceUuid,
                                                  String scheduleUuid,
                                                  Operation operation,
                                                  boolean ignoreRefreshFailure) {
        return resourceBackend(resourceType).makeChangeScheduleMessage(
                resourceUuid, scheduleUuid, operation, ignoreRefreshFailure);
    }

    private NetworkSecurityPolicyScheduleResourceBackend resourceBackend(String resourceType) {
        NetworkSecurityPolicyScheduleResourceBackend backend = backendRegistry.getBackend(resourceType);
        if (backend == null) {
            throw unsupportedResourceType(resourceType);
        }
        return backend;
    }

    private void changeSchedule(String resourceType,
                                 String resourceUuid,
                                 String scheduleUuid,
                                 Operation operation,
                                 Completion completion) {
        changeSchedule(resourceType, resourceUuid, scheduleUuid, operation, false, completion);
    }

    private void changeSchedule(String resourceType,
                                 String resourceUuid,
                                 String scheduleUuid,
                                 Operation operation,
                                 boolean ignoreRefreshFailure,
                                 Completion completion) {
        changeSchedule(resourceType, resourceUuid, scheduleUuid,
                operation, ignoreRefreshFailure, null, completion);
    }

    private void changeSchedule(String resourceType,
                                 String resourceUuid,
                                 String scheduleUuid,
                                 Operation operation,
                                 boolean ignoreRefreshFailure,
                                 Long timeout,
                                 Completion completion) {
        NeedReplyMessage msg = makeScheduleMessage(
                resourceType, resourceUuid, scheduleUuid,
                operation, ignoreRefreshFailure);
        if (timeout != null) {
            msg.setTimeout(timeout);
        }
        bus.send(msg,
                new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            completion.success();
                        } else {
                            completion.fail(reply.getError());
                        }
                    }
                });
    }

    private Completion completionOf(FlowTrigger trigger) {
        return new Completion(trigger) {
            @Override
            public void success() {
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        };
    }

    private NetworkSecurityPolicyScheduleTime scheduleTimeOf(APICreateNetworkSecurityPolicyScheduleMsg msg) {
        return NetworkSecurityPolicyScheduleTime.valueOf(
                msg.getTimeType(), msg.getRepeatType(), msg.getStartDate(), msg.getEndDate(),
                msg.getStartTime(), msg.getEndTime(), msg.getWeekDays());
    }

    private NetworkSecurityPolicyScheduleTime scheduleTimeOf(APIUpdateNetworkSecurityPolicyScheduleMsg msg) {
        return NetworkSecurityPolicyScheduleTime.valueOf(
                msg.getTimeType(), msg.getRepeatType(), msg.getStartDate(), msg.getEndDate(),
                msg.getStartTime(), msg.getEndTime(), msg.getWeekDays());
    }

    private ErrorCode scheduleNotFound(String scheduleUuid) {
        return err(
                ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10006,
                SysErrors.RESOURCE_NOT_FOUND,
                "cannot find network security policy schedule[uuid:%s]", scheduleUuid);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(NetworkSecurityPolicyScheduleConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

}
