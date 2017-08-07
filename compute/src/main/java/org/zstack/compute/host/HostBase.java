package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.host.HostCanonicalEvents.HostDeletedData;
import org.zstack.header.host.HostCanonicalEvents.HostStatusChangedData;
import org.zstack.header.host.HostErrors.Opaque;
import org.zstack.header.host.HostMaintenancePolicyExtensionPoint.HostMaintenancePolicy;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class HostBase extends AbstractHost {
    protected static final CLogger logger = Utils.getLogger(HostBase.class);
    protected HostVO self;

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected HostExtensionPointEmitter extpEmitter;
    @Autowired
    protected GlobalConfigFacade gcf;
    @Autowired
    protected HostManager hostMgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected HostTracker tracker;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected EventFacade evtf;

    protected final String id;

    protected abstract void pingHook(Completion completion);

    protected abstract int getVmMigrateQuantity();

    protected abstract void changeStateHook(HostState current, HostStateEvent stateEvent, HostState next);

    protected abstract void connectHook(ConnectHostInfo info, Completion complete);

    protected HostBase(HostVO self) {
        this.self = self;
        id = "Host-" + self.getUuid();
    }

    protected void checkStatus() {
        if (HostStatus.Connected != self.getStatus()) {
            ErrorCode cause = errf.instantiateErrorCode(HostErrors.HOST_IS_DISCONNECTED, String.format("host[uuid:%s, name:%s] is in status[%s], cannot perform required operation", self.getUuid(), self.getName(), self.getStatus()));
            throw new OperationFailureException(errf.instantiateErrorCode(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE, "unable to do the operation because the host is in status of Disconnected", cause));
        }
    }

    protected void checkState() {
        if (HostState.PreMaintenance == self.getState() || HostState.Maintenance == self.getState()) {
            throw new OperationFailureException(operr("host[uuid:%s, name:%s] is in state[%s], cannot perform required operation", self.getUuid(), self.getName(), self.getState()));
        }
    }

    protected void checkStateAndStatus() {
        checkState();
        checkStatus();
    }

    protected int getHostSyncLevel() {
        return 10;
    }

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeHostStateMsg) {
            handle((APIChangeHostStateMsg) msg);
        } else if (msg instanceof APIDeleteHostMsg) {
            handle((APIDeleteHostMsg) msg);
        } else if (msg instanceof APIReconnectHostMsg) {
            handle((APIReconnectHostMsg) msg);
        } else if (msg instanceof APIUpdateHostMsg) {
            handle((APIUpdateHostMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected HostVO updateHost(APIUpdateHostMsg msg) {
        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }
        if (msg.getManagementIp() != null) {
            self.setManagementIp(msg.getManagementIp());
            update = true;
        }

        return update ? self : null;
    }

    private void handle(APIUpdateHostMsg msg) {
        HostVO vo = updateHost(msg);
        if (vo != null) {
            self = dbf.updateAndRefresh(vo);
        }
        APIUpdateHostEvent evt = new APIUpdateHostEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    protected void maintenanceHook(final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("maintenance-mode-host-%s-ip-%s", self.getUuid(), self.getManagementIp()));

        HostMaintenancePolicy policy = HostMaintenancePolicy.MigrateVm;
        for (HostMaintenancePolicyExtensionPoint ext : pluginRgty.getExtensionList(HostMaintenancePolicyExtensionPoint.class)) {
            HostMaintenancePolicy p = ext.getHostMaintenancePolicy(getSelfInventory());
            if (p != null) {
                policy = p;
                logger.debug(String.format("HostMaintenancePolicyExtensionPoint[%s] set maintenance policy for host[uuid:%s] to %s",
                        ext.getClass(), self.getUuid(), policy));
            }
        }

        final int quantity = getVmMigrateQuantity();
        DebugUtils.Assert(quantity != 0, "getVmMigrateQuantity() cannot return 0");
        final HostMaintenancePolicy finalPolicy = policy;
        chain.then(new ShareFlow() {
            List<String> vmFailedToMigrate = new ArrayList<String>();

            @Override
            public void setup() {
                if (finalPolicy == HostMaintenancePolicy.MigrateVm) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "try-migrate-vm";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
                            q.select(VmInstanceVO_.uuid);
                            q.add(VmInstanceVO_.hostUuid, Op.EQ, self.getUuid());
                            q.add(VmInstanceVO_.state, Op.NOT_EQ, VmInstanceState.Unknown);
                            List<String> vmUuids = q.listValue();

                            if (vmUuids.isEmpty()) {
                                trigger.next();
                                return;
                            }

                            int migrateQuantity = quantity;
                            HostInventory host = getSelfInventory();
                            for (OrderVmBeforeMigrationDuringHostMaintenanceExtensionPoint ext : pluginRgty.getExtensionList(OrderVmBeforeMigrationDuringHostMaintenanceExtensionPoint.class)) {
                                List<String> ordered = ext.orderVmBeforeMigrationDuringHostMaintenance(host, vmUuids);
                                if (ordered != null) {
                                    vmUuids = ordered;

                                    logger.debug(String.format("%s ordered VMs for host maintenance, to keep the order, we will migrate VMs one by one",
                                            ext.getClass()));
                                    migrateQuantity = 1;
                                }
                            }

                            final List<MigrateVmMsg> msgs = new ArrayList<MigrateVmMsg>();
                            for (String uuid : vmUuids) {
                                MigrateVmMsg msg = new MigrateVmMsg();
                                msg.setVmInstanceUuid(uuid);
                                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, uuid);
                                msgs.add(msg);
                            }

                            bus.send(msgs, migrateQuantity, new CloudBusListCallBack(trigger) {
                                @Override
                                public void run(List<MessageReply> replies) {
                                    for (MessageReply reply : replies) {
                                        if (!reply.isSuccess()) {
                                            MigrateVmMsg msg = msgs.get(replies.indexOf(reply));
                                            logger.warn(String.format("failed to migrate vm[uuid:%s] on host[uuid:%s, name:%s, ip:%s], will try stopping it. %s",
                                                    msg.getVmInstanceUuid(), self.getUuid(), self.getName(), self.getManagementIp(), reply.getError()));
                                            vmFailedToMigrate.add(msg.getVmInstanceUuid());
                                        }
                                    }

                                    trigger.next();
                                }
                            });
                        }
                    });
                } else {
                    // the policy is not to migrate vm
                    // put all vms in vmFailedToMigrate so the next flow will stop all of them
                    SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
                    q.select(VmInstanceVO_.uuid);
                    q.add(VmInstanceVO_.hostUuid, Op.EQ, self.getUuid());
                    q.add(VmInstanceVO_.state, Op.NOT_EQ, VmInstanceState.Unknown);
                    List<String> vmUuids = q.listValue();
                    vmFailedToMigrate.addAll(vmUuids);
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "stop-vm-not-migrated";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> vmUuids = new ArrayList<String>();
                        vmUuids.addAll(vmFailedToMigrate);
                        vmUuids = CollectionUtils.removeDuplicateFromList(vmUuids);

                        if (vmUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        stopFailedToMigrateVms(vmUuids, trigger);
                    }

                    private void stopFailedToMigrateVms(List<String> vmUuids, final FlowTrigger trigger) {
                        final List<StopVmInstanceMsg> msgs = new ArrayList<StopVmInstanceMsg>();
                        for (String vmUuid : vmUuids) {
                            StopVmInstanceMsg msg = new StopVmInstanceMsg();
                            msg.setVmInstanceUuid(vmUuid);
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
                            msgs.add(msg);
                        }

                        bus.send(msgs, quantity, new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                StringBuilder sb = new StringBuilder();
                                boolean success = true;
                                for (MessageReply r : replies) {
                                    if (!r.isSuccess()) {
                                        StopVmInstanceMsg msg = msgs.get(replies.indexOf(r));
                                        String err = String.format("\nfailed to stop vm[uuid:%s] on host[uuid:%s, name:%s, ip:%s], %s",
                                                msg.getVmInstanceUuid(), self.getUuid(), self.getName(), self.getManagementIp(), r.getError());
                                        sb.append(err);
                                        success = false;
                                    }
                                }

                                if (!success) {
                                    logger.warn(sb.toString());
                                }

                                if (success || HostGlobalConfig.IGNORE_ERROR_ON_MAINTENANCE_MODE.value(Boolean.class)) {
                                    trigger.next();
                                } else {
                                    trigger.fail(operr(sb.toString()));
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(final APIReconnectHostMsg msg) {
        ReconnectHostMsg rmsg = new ReconnectHostMsg();
        rmsg.setHostUuid(self.getUuid());
        bus.makeTargetServiceIdByResourceUuid(rmsg, HostConstant.SERVICE_ID, self.getUuid());
        bus.send(rmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                APIReconnectHostEvent evt = new APIReconnectHostEvent(msg.getId());
                if (!reply.isSuccess()) {
                    evt.setError(errf.instantiateErrorCode(HostErrors.UNABLE_TO_RECONNECT_HOST, reply.getError()));
                    logger.debug(String.format("failed to reconnect host[uuid:%s] because %s", self.getUuid(), reply.getError()));
                }else{
                    self = dbf.reload(self);
                    evt.setInventory((getSelfInventory()));
                }
                bus.publish(evt);
            }
        });
    }

    private void deleteHostByApiMessage(APIDeleteHostMsg msg) {
        final APIDeleteHostEvent evt = new APIDeleteHostEvent(msg.getId());

        final String issuer = HostVO.class.getSimpleName();
        final List<HostInventory> ctx = Arrays.asList(HostInventory.valueOf(self));

        HostInventory hinv = HostInventory.valueOf(self);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-host-%s", msg.getUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                bus.publish(evt);

                HostDeletedData d = new HostDeletedData();
                d.setInventory(HostInventory.valueOf(self));
                d.setHostUuid(self.getUuid());
                evtf.fire(HostCanonicalEvents.HOST_DELETED_PATH, d);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errCode));
                bus.publish(evt);
            }
        }).start();
    }

    private void handle(final APIDeleteHostMsg msg) {
        deleteHostByApiMessage(msg);
    }

    protected HostState changeState(HostStateEvent event) {
        HostState currentState = self.getState();
        HostState next = currentState.nextState(event);
        changeStateHook(currentState, event, next);

        extpEmitter.beforeChange(self, event);
        self.setState(next);
        self = dbf.updateAndRefresh(self);
        extpEmitter.afterChange(self, event, currentState);
        logger.debug(String.format("Host[%s]'s state changed from %s to %s", self.getUuid(), currentState, self.getState()));
        return self.getState();
    }

    protected void changeStateByApiMessage(final APIChangeHostStateMsg msg, final NoErrorCompletion completion) {
        thdf.chainSubmit(new ChainTask(msg, completion) {
            @Override
            public String getSyncSignature() {
                return String.format("change-host-state-%s", self.getUuid());
            }

            private void done(SyncTaskChain chain) {
                completion.done();
                chain.next();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final APIChangeHostStateEvent evt = new APIChangeHostStateEvent(msg.getId());
                HostStateEvent stateEvent = HostStateEvent.valueOf(msg.getStateEvent());

                if (self.getStatus() == HostStatus.Disconnected && stateEvent == HostStateEvent.maintain) {
                    throw new ApiMessageInterceptionException(operr("cannot change the state of Disconnected host into Maintenance "));
                }
                stateEvent = stateEvent == HostStateEvent.maintain ? HostStateEvent.preMaintain : stateEvent;
                try {
                    extpEmitter.preChange(self, stateEvent);
                } catch (HostException he) {
                    evt.setError(errf.instantiateErrorCode(SysErrors.CHANGE_RESOURCE_STATE_ERROR, he.getMessage()));
                    bus.publish(evt);
                    done(chain);
                    return;
                }

                if (HostStateEvent.preMaintain == stateEvent) {
                    changeState(HostStateEvent.preMaintain);
                    maintenanceHook(new Completion(msg, chain) {
                        @Override
                        public void success() {
                            changeState(HostStateEvent.maintain);
                            evt.setInventory(HostInventory.valueOf(self));
                            bus.publish(evt);
                            done(chain);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            evt.setError(errf.instantiateErrorCode(HostErrors.UNABLE_TO_ENTER_MAINTENANCE_MODE, errorCode.getDetails(), errorCode));
                            changeState(HostStateEvent.enable);
                            bus.publish(evt);
                            done(chain);
                        }
                    });
                } else {
                    HostState origState = self.getState();
                    HostState state = changeState(stateEvent);

                    if (origState == HostState.Maintenance && state != HostState.Maintenance) {
                        // host is out of maintenance mode, track and reconnect it.
                        tracker.trackHost(self.getUuid());
                        ReconnectHostMsg rmsg = new ReconnectHostMsg();
                        rmsg.setHostUuid(self.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(rmsg, HostConstant.SERVICE_ID, self.getUuid());
                        bus.send(rmsg);
                    }

                    HostInventory inv = HostInventory.valueOf(self);
                    evt.setInventory(inv);
                    bus.publish(evt);
                    done(chain);
                }
            }

            @Override
            public String getName() {
                return String.format("change-host-state-%s", self.getUuid());
            }
        });
    }

    protected void handle(final APIChangeHostStateMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return "change-host-state-" + self.getUuid();
            }

            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                changeStateByApiMessage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    protected void handleLocalMessage(Message msg) {
        if (msg instanceof ChangeHostStateMsg) {
            handle((ChangeHostStateMsg) msg);
        } else if (msg instanceof HostDeletionMsg) {
            handle((HostDeletionMsg) msg);
        } else if (msg instanceof ConnectHostMsg) {
            handle((ConnectHostMsg) msg);
        } else if (msg instanceof ReconnectHostMsg) {
            handle((ReconnectHostMsg) msg);
        } else if (msg instanceof ChangeHostConnectionStateMsg) {
            handle((ChangeHostConnectionStateMsg) msg);
        } else if (msg instanceof PingHostMsg) {
            handle((PingHostMsg) msg);
        } else {
            HostBaseExtensionFactory ext = hostMgr.getHostBaseExtensionFactory(msg);
            if (ext != null) {
                Host h = ext.getHost(self);
                h.handleMessage(msg);
            } else {
                bus.dealWithUnknownMessage(msg);
            }
        }
    }

    private void handle(final PingHostMsg msg) {
        final PingHostReply reply = new PingHostReply();
        if (self.getStatus() == HostStatus.Connecting) {
            reply.setError(operr("host is connecting"));
            bus.reply(msg, reply);
            return;
        }

        pingHook(new Completion(msg) {
            @Override
            public void success() {
                reply.setConnected(true);
                reply.setCurrentHostStatus(self.getStatus().toString());
                bus.reply(msg, reply);

                extpEmitter.hostPingTask(HypervisorType.valueOf(self.getHypervisorType()), getSelfInventory());
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setConnected(false);
                reply.setCurrentHostStatus(self.getStatus().toString());
                reply.setError(errorCode);
                reply.setSuccess(true);

                Boolean noReconnect = (Boolean) errorCode.getFromOpaque(Opaque.NO_RECONNECT_AFTER_PING_FAILURE.toString());
                reply.setNoReconnect(noReconnect != null && noReconnect);

                if(!Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).isExists()){
                    reply.setNoReconnect(true);
                    bus.reply(msg, reply);
                    return;
                }

                changeConnectionState(HostStatusEvent.disconnected);
                
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final HostDeletionMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(SyncTaskChain chain) {
                HostInventory hinv = HostInventory.valueOf(self);
                extpEmitter.beforeDelete(hinv);
                deleteHook();
                extpEmitter.afterDelete(hinv);
                bus.reply(msg, new HostDeletionReply());
                tracker.untrackHost(self.getUuid());
                chain.next();
            }

            @Override
            public int getSyncLevel() {
                return getHostSyncLevel();
            }

            @Override
            public String getName() {
                return String.format("host-deletion-%s", self.getUuid());
            }
        });
    }

    private void reconnectHost(final ReconnectHostMsg msg, final NoErrorCompletion completion) {
        thdf.chainSubmit(new ChainTask(msg, completion) {
            @Override
            public String getSyncSignature() {
                return String.format("reconnect-host-%s", self.getUuid());
            }

            private void finish(SyncTaskChain chain) {
                chain.next();
                completion.done();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                checkState();
                if (msg.isSkipIfHostConnected() && HostStatus.Connected == self.getStatus()) {
                    finish(chain);
                    return;
                }

                changeConnectionState(HostStatusEvent.disconnected);
                ConnectHostMsg connectMsg = new ConnectHostMsg(self.getUuid());
                connectMsg.setNewAdd(false);
                bus.makeTargetServiceIdByResourceUuid(connectMsg, HostConstant.SERVICE_ID, self.getUuid());
                bus.send(connectMsg, new CloudBusCallBack(msg, chain, completion) {
                    @Override
                    public void run(MessageReply reply) {
                        ReconnectHostReply r = new ReconnectHostReply();
                        if (reply.isSuccess()) {
                            logger.debug(String.format("Successfully reconnect host[uuid:%s]", self.getUuid()));
                        } else {
                            r.setError(errf.instantiateErrorCode(HostErrors.UNABLE_TO_RECONNECT_HOST, reply.getError()));
                            logger.debug(String.format("Failed to reconnect host[uuid:%s] because %s",
                                    self.getUuid(), reply.getError()));
                        }
                        bus.reply(msg, r);
                    }
                });

                // no need to block the queue, because the ConnectHostMsg will be queued as well
                finish(chain);
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(final ReconnectHostMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return "reconnect-host-" + self.getUuid();
            }

            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                reconnectHost(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    private void handle(final ChangeHostConnectionStateMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return "change-host-connection-state-" + self.getUuid();
            }

            private boolean reestablishConnection() {
                try {
                    extpEmitter.connectionReestablished(HypervisorType.valueOf(self.getHypervisorType()), getSelfInventory());
                } catch (HostException e) {
                    logger.warn(String.format("unable to reestablish connection to kvm host[uuid:%s, ip:%s], %s",
                            self.getUuid(), self.getManagementIp(), e.getMessage()), e);
                    return false;
                }

                return true;
            }

            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(SyncTaskChain chain) {
                HostStatusEvent cevt = HostStatusEvent.valueOf(msg.getConnectionStateEvent());
                HostStatus next = self.getStatus().nextStatus(cevt);
                if (self.getStatus() == HostStatus.Disconnected && next == HostStatus.Connected) {
                    if (!reestablishConnection()) {
                        cevt = HostStatusEvent.disconnected;
                    }
                }

                changeConnectionState(cevt);
                bus.reply(msg, new ChangeHostConnectionStateReply());
                chain.next();
            }

            @Override
            public int getSyncLevel() {
                return getHostSyncLevel();
            }

        });
    }

    protected HostInventory getSelfInventory() {
        return HostInventory.valueOf(self);
    }

    protected boolean changeConnectionState(final HostStatusEvent event) {

        if(!Q.New(HostVO.class).eq(HostVO_.uuid, self.getUuid()).isExists()){
            throw new CloudRuntimeException(String.format("change host connection state fail, can not find the host[%s]", self.getUuid()));
        }

        HostStatus before = self.getStatus();
        HostStatus next = before.nextStatus(event);
        if (before == next) {
            return false;
        }

        self.setStatus(next);
        self = dbf.updateAndRefresh(self);
        logger.debug(String.format("Host %s [uuid:%s] changed connection state from %s to %s",
                self.getName(), self.getUuid(), before, next));

        HostStatusChangedData data = new HostStatusChangedData();
        data.setHostUuid(self.getUuid());
        data.setNewStatus(next.toString());
        data.setOldStatus(before.toString());
        data.setInventory(HostInventory.valueOf(self));
        evtf.fire(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, data);

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(AfterChangeHostStatusExtensionPoint.class),
                new ForEachFunction<AfterChangeHostStatusExtensionPoint>() {
                    @Override
                    public void run(AfterChangeHostStatusExtensionPoint arg) {
                        arg.afterChangeHostStatus(self.getUuid(), before, next);
                    }
                });
        return true;
    }

    private void handle(final ConnectHostMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("connect-host-%s", self.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                checkState();
                final ConnectHostReply reply = new ConnectHostReply();

                final FlowChain flowChain = FlowChainBuilder.newShareFlowChain();
                flowChain.setName(String.format("connect-host-%s", self.getUuid()));
                flowChain.then(new ShareFlow() {
                    @Override
                    public void setup() {
                        flow(new NoRollbackFlow() {
                            String __name__ = "connect-host";

                            @Override
                            public void run(final FlowTrigger trigger, Map data) {
                                changeConnectionState(HostStatusEvent.connecting);
                                connectHook(ConnectHostInfo.fromConnectHostMsg(msg), new Completion(trigger) {
                                    @Override
                                    public void success() {
                                        trigger.next();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.fail(errorCode);
                                    }
                                });
                            }
                        });

                        flow(new NoRollbackFlow() {
                            String __name__ = "call-post-connect-extensions";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                FlowChain postConnectChain = FlowChainBuilder.newSimpleFlowChain();
                                postConnectChain.allowEmptyFlow();

                                self = dbf.reload(self);
                                HostInventory inv = getSelfInventory();

                                for (PostHostConnectExtensionPoint p : pluginRgty.getExtensionList(PostHostConnectExtensionPoint.class)) {
                                    postConnectChain.then(p.createPostHostConnectFlow(inv));
                                }

                                postConnectChain.done(new FlowDoneHandler(trigger) {
                                    @Override
                                    public void handle(Map data) {
                                        trigger.next();
                                    }
                                }).error(new FlowErrorHandler(trigger) {
                                    @Override
                                    public void handle(ErrorCode errCode, Map data) {
                                        trigger.fail(errCode);
                                    }
                                }).start();
                            }
                        });

                        flow(new NoRollbackFlow() {
                            String __name__ = "recalculate-host-capacity";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                RecalculateHostCapacityMsg msg = new RecalculateHostCapacityMsg();
                                msg.setHostUuid(self.getUuid());
                                bus.makeLocalServiceId(msg, HostAllocatorConstant.SERVICE_ID);
                                bus.send(msg);
                                trigger.next();
                            }
                        });

                        done(new FlowDoneHandler(msg) {
                            @Override
                            public void handle(Map data) {
                                changeConnectionState(HostStatusEvent.connected);
                                tracker.trackHost(self.getUuid());

                                CollectionUtils.safeForEach(pluginRgty.getExtensionList(HostAfterConnectedExtensionPoint.class),
                                        new ForEachFunction<HostAfterConnectedExtensionPoint>() {
                                            @Override
                                            public void run(HostAfterConnectedExtensionPoint ext) {
                                                ext.afterHostConnected(getSelfInventory());
                                            }
                                        });

                                bus.reply(msg, reply);
                            }
                        });

                        error(new FlowErrorHandler(msg) {
                            @Override
                            public void handle(ErrorCode errCode, Map data) {
                                changeConnectionState(HostStatusEvent.disconnected);
                                if (!msg.isNewAdd()) {
                                    tracker.trackHost(self.getUuid());
                                }

                                reply.setError(errCode);
                                bus.reply(msg, reply);
                            }
                        });

                        Finally(new FlowFinallyHandler(msg) {
                            @Override
                            public void Finally() {
                                chain.next();
                            }
                        });
                    }
                }).start();
            }

            @Override
            public String getName() {
                return "connect-host";
            }
        });
    }

    private void changeStateByLocalMessage(final ChangeHostStateMsg msg, final NoErrorCompletion completion) {
        thdf.chainSubmit(new ChainTask(msg, completion) {
            @Override
            public String getSyncSignature() {
                return String.format("change-host-state-%s", self.getUuid());
            }

            private void done(SyncTaskChain chain) {
                completion.done();
                chain.next();
            }

            @Override
            public void run(SyncTaskChain chain) {
                ChangeHostStateReply reply = new ChangeHostStateReply();
                if (self.getState() != HostState.Enabled && self.getState() != HostState.Disabled) {
                    done(chain);
                    return;
                }

                HostStateEvent stateEvent = HostStateEvent.valueOf(msg.getStateEvent());
                changeState(stateEvent);
                HostInventory inv = HostInventory.valueOf(self);
                reply.setInventory(inv);
                bus.reply(msg, reply);
                done(chain);
            }

            @Override
            public String getName() {
                return String.format("change-host-state-%s", self.getUuid());
            }
        });
    }

    private void handle(final ChangeHostStateMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return "change-host-state-" + self.getUuid();
            }

            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                changeStateByLocalMessage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }
}
