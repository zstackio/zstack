package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.With;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.*;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.*;
import org.zstack.header.allocator.AllocateHostDryRunReply;
import org.zstack.header.allocator.DesignatedAllocateHostMsg;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.configuration.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageEO;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.*;
import org.zstack.header.network.l3.L3Network;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.storage.primary.CreateTemplateFromVolumeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.CreateTemplateFromVolumeOnPrimaryStorageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.vm.*;
import org.zstack.header.vm.ChangeVmMetaDataMsg.AtomicHostUuid;
import org.zstack.header.vm.ChangeVmMetaDataMsg.AtomicVmState;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec.HostName;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class VmInstanceBase extends AbstractVmInstance {
    protected static final CLogger logger = Utils.getLogger(VmInstanceBase.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected VmInstanceManager vmMgr;
    @Autowired
    protected VmInstanceExtensionPointEmitter extEmitter;
    @Autowired
    protected VmInstanceNotifyPointEmitter notfiyEmitter;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected AccountManager acntMgr;
    @Autowired
    protected EventFacade evtf;


    protected VmInstanceVO self;
    protected String syncThreadName;

    public void destroy(final Completion completion){
        self = dbf.findByUuid(self.getUuid(), VmInstanceVO.class);
        if (self == null) {
            // the vm has been destroyed, most likely by rollback
            completion.success();
            return;
        }

        if (VmInstanceState.Created == self.getState()) {
            // the vm is only created in DB, no need to go through normal destroying process
            completion.success();
            return;
        }

        self = changeVmStateInDb(VmInstanceStateEvent.destroying);
        final VmInstanceInventory inv = VmInstanceInventory.valueOf(self);

        FlowChain chain = getDestroyVmWorkFlowChain(inv);
        chain.setName(String.format("destroy-vm-%s", self.getUuid()));
        VmInstanceSpec spec = buildSpecFromInventory(inv);
        spec.setCurrentVmOperation(VmOperation.Destroy);
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                self = dbf.reload(self);
                changeVmStateInDb(VmInstanceStateEvent.unknown);
                completion.fail(errCode);
            }
        }).start();
    }

    protected VmInstanceVO getSelf() {
        return self;
    }

    protected VmInstanceInventory getSelfInventory() {
        return VmInstanceInventory.valueOf(self);
    }

    public VmInstanceBase(VmInstanceVO vo) {
        this.self = vo;
        this.syncThreadName = "Vm-" + vo.getUuid();
    }

    protected VmInstanceVO refreshVO() {
        VmInstanceVO vo = self;
        self = dbf.findByUuid(self.getUuid(), VmInstanceVO.class);
        if (self == null) {
            throw new OperationFailureException(errf.stringToOperationError(String.format("vm[uuid:%s, name:%s] has been deleted", vo.getUuid(), vo.getName())));
        }
        return self;
    }

    protected FlowChain getCreateVmWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getCreateVmWorkFlowChain(inv);
    }

    protected FlowChain getStopVmWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getStopVmWorkFlowChain(inv);
    }

    protected FlowChain getRebootVmWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getRebootVmWorkFlowChain(inv);
    }

    protected FlowChain getStartVmWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getStartVmWorkFlowChain(inv);
    }

    protected FlowChain getDestroyVmWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getDestroyVmWorkFlowChain(inv);
    }

    protected FlowChain getMigrateVmWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getMigrateVmWorkFlowChain(inv);
    }

    protected FlowChain getAttachUninstantiatedVolumeWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getAttachUninstantiatedVolumeWorkFlowChain(inv);
    }

    protected VmInstanceVO changeVmStateInDb(VmInstanceStateEvent stateEvent) {
        VmInstanceState bs = self.getState();
        final VmInstanceState state = self.getState().nextState(stateEvent);
        if (bs == state) {
            // vm tracer may detect vm state change before start/stop/reboot/destroy flow change
            // vm state in db. so vm state may have been changed by vm tracer, we return
            // quickly for this case
            return self;
        }

        self.setState(state);
        self = dbf.updateAndRefresh(self);
        logger.debug(String.format("vm[uuid:%s] changed state from %s to %s", self.getUuid(), bs, self.getState()));
        notfiyEmitter.notifyVmStateChange(VmInstanceInventory.valueOf(self), bs, state);
        return self;
    }

    @Override
    @MessageSafe
    public void handleMessage(final Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    protected void handleLocalMessage(Message msg) {
        if (msg instanceof StartNewCreatedVmInstanceMsg) {
            handle((StartNewCreatedVmInstanceMsg) msg);
        } else if (msg instanceof StartVmInstanceMsg) {
            handle((StartVmInstanceMsg) msg);
        } else if (msg instanceof StopVmInstanceMsg) {
            handle((StopVmInstanceMsg) msg);
        } else if (msg instanceof RebootVmInstanceMsg) {
            handle((RebootVmInstanceMsg) msg);
        } else if (msg instanceof ChangeVmStateMsg) {
            handle((ChangeVmStateMsg) msg);
        } else if (msg instanceof DestroyVmInstanceMsg) {
            handle((DestroyVmInstanceMsg)msg);
        } else if (msg instanceof AttachNicToVmMsg) {
            handle((AttachNicToVmMsg)msg);
        } else if (msg instanceof CreateTemplateFromVmRootVolumeMsg) {
            handle((CreateTemplateFromVmRootVolumeMsg) msg);
        } else if (msg instanceof VmInstanceDeletionMsg) {
            handle((VmInstanceDeletionMsg) msg);
        } else if (msg instanceof VmAttachNicMsg) {
            handle((VmAttachNicMsg) msg);
        } else if (msg instanceof MigrateVmMsg) {
            handle((MigrateVmMsg) msg);
        } else if (msg instanceof DetachDataVolumeFromVmMsg) {
            handle((DetachDataVolumeFromVmMsg) msg);
        } else if (msg instanceof AttachDataVolumeToVmMsg) {
            handle((AttachDataVolumeToVmMsg) msg);
        } else if (msg instanceof GetVmMigrationTargetHostMsg) {
            handle((GetVmMigrationTargetHostMsg) msg);
        } else if (msg instanceof ChangeVmMetaDataMsg) {
            handle((ChangeVmMetaDataMsg) msg);
        } else if (msg instanceof LockVmInstanceMsg) {
            handle((LockVmInstanceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final LockVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                logger.debug(String.format("locked vm[uuid:%s] for %s", self.getUuid(), msg.getReason()));
                evtf.on(LockResourceMessage.UNLOCK_CANONICAL_EVENT_PATH, new AutoOffEventCallback() {
                    @Override
                    public boolean run(Map tokens, Object data) {
                        if (msg.getUnlockKey().equals(data)) {
                            logger.debug(String.format("unlocked vm[uuid:%s] that was locked by %s", self.getUuid(), msg.getReason()));
                            chain.next();
                            return true;
                        }

                        return false;
                    }
                });

                LockVmInstanceReply reply = new LockVmInstanceReply();
                bus.reply(msg, reply);
            }

            @Override
            public String getName() {
                return String.format("lock-vm-%s", self.getUuid());
            }
        });
    }

    private void handle(final ChangeVmMetaDataMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                changeMetaData(msg);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("change-meta-data-of-vm-%s", self.getUuid());
            }
        });
    }

    private void changeMetaData(ChangeVmMetaDataMsg msg) {
        ChangeVmMetaDataReply reply = new ChangeVmMetaDataReply();
        refreshVO();
        if (self == null) {
            bus.reply(msg, reply);
            return;
        }

        AtomicVmState s = msg.getState();
        AtomicHostUuid h = msg.getHostUuid();

        if (msg.isNeedHostAndStateBothMatch()) {
            if (s != null && h != null && s.getExpected() == self.getState()) {
                if ((h.getExpected() == null && self.getHostUuid() == null) ||
                        (h.getExpected() != null && h.getExpected().equals(self.getHostUuid()))) {
                    changeVmStateInDb(s.getValue().getDrivenEvent());
                    reply.setChangeStateDone(true);

                    self.setHostUuid(h.getValue());
                    dbf.update(self);
                    reply.setChangeHostUuidDone(true);
                }
            }
        } else {
            if (s != null && s.getExpected() == self.getState()) {
                changeVmStateInDb(s.getValue().getDrivenEvent());
                reply.setChangeStateDone(true);
            }

            if (h != null) {
                if ((h.getExpected() == null && self.getHostUuid() == null) ||
                        (h.getExpected() != null && h.getExpected().equals(self.getHostUuid()))) {
                    self.setHostUuid(h.getValue());
                    dbf.update(self);
                    reply.setChangeHostUuidDone(true);
                }
            }
        }


        bus.reply(msg, reply);
    }

    private void getVmMigrationTargetHost(Message msg, final ReturnValueCompletion<List<HostInventory>> completion) {
        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), VmErrors.MIGRATE_ERROR);
        if (allowed != null) {
            completion.fail(allowed);
            return;
        }

        final DesignatedAllocateHostMsg amsg = new DesignatedAllocateHostMsg();
        amsg.setCpuCapacity(self.getCpuNum() * self.getCpuSpeed());
        amsg.setMemoryCapacity(self.getMemorySize());
        amsg.getAvoidHostUuids().add(self.getHostUuid());
        if (msg instanceof GetVmMigrationTargetHostMsg) {
            GetVmMigrationTargetHostMsg gmsg = (GetVmMigrationTargetHostMsg) msg;
            if (gmsg.getAvoidHostUuids() != null) {
                amsg.getAvoidHostUuids().addAll(gmsg.getAvoidHostUuids());
            }
        }
        amsg.setVmInstance(VmInstanceInventory.valueOf(self));
        amsg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        amsg.setAllocatorStrategy(HostAllocatorConstant.MIGRATE_VM_ALLOCATOR_TYPE);
        amsg.setVmOperation(VmOperation.Migrate.toString());
        amsg.setL3NetworkUuids(CollectionUtils.transformToList(self.getVmNics(), new Function<String, VmNicVO>() {
            @Override
            public String call(VmNicVO arg) {
                return arg.getL3NetworkUuid();
            }
        }));
        amsg.setDryRun(true);

        bus.send(amsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply re) {
                if (!re.isSuccess()) {
                    completion.fail(re.getError());
                } else {
                    completion.success(((AllocateHostDryRunReply) re).getHosts());
                }
            }
        });
    }

    private void handle(final GetVmMigrationTargetHostMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final GetVmMigrationTargetHostReply reply = new GetVmMigrationTargetHostReply();
                getVmMigrationTargetHost(msg, new ReturnValueCompletion<List<HostInventory>>(msg, chain) {
                    @Override
                    public void success(List<HostInventory> returnValue) {
                        reply.setHosts(returnValue);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("get-migration-target-host-for-vm-%s", self.getUuid());
            }
        });
    }

    private void handle(final AttachDataVolumeToVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                attachVolume(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("attach-volume-%s-to-vm-%s", msg.getVolume().getUuid(), msg.getVmInstanceUuid());
            }
        });
    }

    private void handle(final DetachDataVolumeFromVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                detachVolume(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("detach-volume-%s-from-vm-%s", msg.getVolume().getUuid(), msg.getVmInstanceUuid());
            }
        });
    }


    private void handle(final MigrateVmMsg msg) {
        final MigrateVmReply reply = new MigrateVmReply();
        thdf.chainSubmit(new ChainTask() {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                migrateVm(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("migrate-vm-%s", self.getUuid());
            }
        });
    }

    private void attachNic(final Message msg, final String l3Uuid, final ReturnValueCompletion<VmNicInventory> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                refreshVO();
                ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    completion.fail(allowed);
                    return;
                }

                final VmInstanceSpec spec = new VmInstanceSpec();
                spec.setVmInventory(VmInstanceInventory.valueOf(self));
                L3NetworkVO l3vo = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
                spec.setL3Networks(Arrays.asList(L3NetworkInventory.valueOf(l3vo)));

                FlowChain flowChain = FlowChainBuilder.newSimpleFlowChain();
                flowChain.setName(String.format("attachNic-vm-%s-l3-%s", self.getUuid(), l3Uuid));
                flowChain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
                flowChain.then(new VmAllocateNicFlow());
                if (self.getState() == VmInstanceState.Running) {
                    flowChain.then(new VmAttachNicOnHypervisorFlow());
                }

                flowChain.done(new FlowDoneHandler(chain) {
                    @Override
                    public void handle(Map data) {
                        VmNicInventory nic = spec.getDestNics().get(0);
                        completion.success(nic);
                        chain.next();
                    }
                }).error(new FlowErrorHandler(chain) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                        chain.next();
                    }
                }).start();
            }

            @Override
            public String getName() {
                return String.format("attachNic-vm-%s-l3-%s", self.getUuid(), l3Uuid);
            }
        });
    }

    private void handle(final VmAttachNicMsg msg) {
        final VmAttachNicReply reply = new VmAttachNicReply();
        attachNic(msg, msg.getL3NetworkUuid(), new ReturnValueCompletion<VmNicInventory>() {
            @Override
            public void success(VmNicInventory nic) {
                reply.setInventroy(nic);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final VmInstanceDeletionMsg msg) {
        final VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
        extEmitter.beforeDestroyVm(inv);
        destroy(new Completion(msg) {
            @Override
            public void success() {
                extEmitter.afterDeleteVm(inv);
                logger.debug(String.format("successfully deleted vm instance[name:%s, uuid:%s]", self.getName(), self.getUuid()));
                dbf.remove(getSelf());
                bus.reply(msg, new VmInstanceDeletionReply());
            }

            @Override
            public void fail(ErrorCode errorCode) {
                extEmitter.failedToDestroyVm(inv, errorCode);
                logger.debug(String.format("failed to delete vm instance[name:%s, uuid:%s], because %s", self.getName(), self.getUuid(), errorCode));
                VmInstanceDeletionReply r = new VmInstanceDeletionReply();
                r.setError(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, errorCode));
                bus.reply(msg, r);
            }
        });
    }

    private void handle(final RebootVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("reboot-vm-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                rebootVm(msg, chain);
            }
        });
    }

    private void rebootVm(final RebootVmInstanceMsg msg, final SyncTaskChain chain) {
        rebootVm(msg, new Completion(chain) {
            @Override
            public void success() {
                RebootVmInstanceReply reply = new RebootVmInstanceReply();
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                reply.setInventory(inv);
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                RebootVmInstanceReply reply = new RebootVmInstanceReply();
                reply.setError(errf.instantiateErrorCode(VmErrors.REBOOT_ERROR, errorCode));
                bus.reply(msg, reply);
                chain.next();
            }
        });
    }

    private void handle(final StopVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("stop-vm-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                stopVm(msg, chain);
            }
        });
    }

    private void stopVm(final StopVmInstanceMsg msg, final SyncTaskChain chain) {
        stopVm(msg, new Completion(chain) {
            @Override
            public void success() {
                StopVmInstanceReply reply = new StopVmInstanceReply();
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                reply.setInventory(inv);
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                StopVmInstanceReply reply = new StopVmInstanceReply();
                reply.setError(errf.instantiateErrorCode(VmErrors.STOP_ERROR, errorCode));
                bus.reply(msg, reply);
                chain.next();
            }
        });
    }

    private void handle(final StartVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask (msg) {
            @Override
            public String getName() {
                return String.format("start-vm-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                startVm(msg, chain);
            }
        });
    }

    private void createTemplateFromRootVolume(final CreateTemplateFromVmRootVolumeMsg msg, final SyncTaskChain chain) {
        boolean callNext = true;
        try {
            refreshVO();
            ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
            if (allowed != null) {
                bus.replyErrorByMessageType(msg, allowed);
                return;
            }

            final CreateTemplateFromVmRootVolumeReply reply = new CreateTemplateFromVmRootVolumeReply();

            CreateTemplateFromVolumeOnPrimaryStorageMsg cmsg = new CreateTemplateFromVolumeOnPrimaryStorageMsg();
            cmsg.setVolumeInventory(msg.getRootVolumeInventory());
            cmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
            cmsg.setImageInventory(msg.getImageInventory());
            bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, msg.getRootVolumeInventory().getPrimaryStorageUuid());
            bus.send(cmsg, new CloudBusCallBack(chain) {
                private void fail(ErrorCode errorCode) {
                    String err = String.format("failed to create template from root volume[uuid:%s] on primary storage[uuid:%s]", msg.getRootVolumeInventory().getUuid(), msg.getRootVolumeInventory().getPrimaryStorageUuid());
                    logger.warn(err);
                    reply.setError(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, err, errorCode));
                    bus.reply(msg, reply);
                }

                @Override
                public void run(MessageReply r) {
                    if (!r.isSuccess()) {
                        fail(r.getError());
                    } else {
                        CreateTemplateFromVolumeOnPrimaryStorageReply creply = (CreateTemplateFromVolumeOnPrimaryStorageReply) r;
                        reply.setInstallPath(creply.getTemplateBackupStorageInstallPath());
                        reply.setFormat(creply.getFormat());
                        bus.reply(msg, reply);
                    }
                    chain.next();
                }
            });

            callNext = false;
        } finally {
            if (callNext) {
                chain.next();
            }
        }
    }

    private void handle(final CreateTemplateFromVmRootVolumeMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("create-template-from-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                createTemplateFromRootVolume(msg, chain);
            }
        });
    }

    private void handle(final AttachNicToVmMsg msg) {
        ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
        if (allowed != null) {
            bus.replyErrorByMessageType(msg, allowed);
            return;
        }

        AttachNicToVmOnHypervisorMsg amsg = new AttachNicToVmOnHypervisorMsg();
        amsg.setVmUuid(self.getUuid());
        amsg.setHostUuid(self.getHostUuid());
        amsg.setNics(msg.getNics());
        bus.makeTargetServiceIdByResourceUuid(amsg, HostConstant.SERVICE_ID, self.getHostUuid());
        bus.send(amsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                AttachNicToVmReply r = new AttachNicToVmReply();
                if (!reply.isSuccess()) {
                    r.setError(errf.instantiateErrorCode(VmErrors.ATTACH_NETWORK_ERROR, r.getError()));
                }
                bus.reply(msg, r);
            }
        });
    }

    private void handle(final DestroyVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask (msg) {
            @Override
            public String getName() {
                return String.format("destroy-vm-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain taskChain) {
                final DestroyVmInstanceReply reply = new DestroyVmInstanceReply();
                final String issuer = VmInstanceVO.class.getSimpleName();
                final List<VmInstanceInventory> ctx = VmInstanceInventory.valueOf(Arrays.asList(self));
                final FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                chain.setName(String.format("destory-vm-%s", self.getUuid()));
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
                }).done(new FlowDoneHandler(msg, taskChain) {
                    @Override
                    public void handle(Map data) {
                        casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                        bus.reply(msg, reply);
                        taskChain.next();
                    }
                }).error(new FlowErrorHandler(msg, taskChain) {
                    @Override
                    public void handle(final ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                        taskChain.next();
                    }
                }).start();
            }
        });
    }


    protected void handle(final ChangeVmStateMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("change-vm-state-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                refreshVO();
                if (self == null) {
                    // vm has been deleted by previous request
                    // this happens when delete vm request queued before
                    // change state request from vm tracer.
                    // in this case, ignore change state request
                    logger.debug(String.format(String.format("vm[uuid:%s] has been deleted, ignore change vm state request from vm tracer", msg.getVmInstanceUuid())));
                    return;
                }

                changeVmStateInDb(VmInstanceStateEvent.valueOf(msg.getStateEvent()));
                chain.next();
            }
        });
    }

    protected void startVmFromNewCreate(final StartNewCreatedVmInstanceMsg msg, final SyncTaskChain taskChain) {
        boolean callNext = true;
        try {
            refreshVO();
            ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
            if (allowed != null) {
                bus.replyErrorByMessageType(msg, allowed);
                return;
            }
            ErrorCode preCreated = extEmitter.preStartNewCreatedVm(msg.getVmInstanceInventory());
            if (preCreated != null) {
                bus.replyErrorByMessageType(msg, errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, preCreated));
                return;
            }

            final VmInstanceSpec spec = new VmInstanceSpec();
            spec.setMessage(msg);
            spec.setVmInventory(msg.getVmInstanceInventory());
            if (msg.getL3NetworkUuids() != null && !msg.getL3NetworkUuids().isEmpty()) {
                SimpleQuery<L3NetworkVO> nwquery = dbf.createQuery(L3NetworkVO.class);
                nwquery.add(L3NetworkVO_.uuid, Op.IN, msg.getL3NetworkUuids());
                List<L3NetworkVO> vos = nwquery.list();
                List<L3NetworkInventory> nws = L3NetworkInventory.valueOf(vos);
                spec.setL3Networks(nws);
            } else {
                spec.setL3Networks(new ArrayList<L3NetworkInventory>(0));
            }
            if (msg.getDataDiskOfferingUuids() != null && !msg.getDataDiskOfferingUuids().isEmpty()) {
                SimpleQuery<DiskOfferingVO> dquery = dbf.createQuery(DiskOfferingVO.class);
                dquery.add(DiskOfferingVO_.uuid, SimpleQuery.Op.IN, msg.getDataDiskOfferingUuids());
                List<DiskOfferingVO> vos = dquery.list();

                // allow create multiple data volume from the same disk offering
                List<DiskOfferingInventory> disks = new ArrayList<DiskOfferingInventory>();
                for (final String duuid : msg.getDataDiskOfferingUuids()) {
                    DiskOfferingVO dvo = CollectionUtils.find(vos, new Function<DiskOfferingVO, DiskOfferingVO>() {
                        @Override
                        public DiskOfferingVO call(DiskOfferingVO arg) {
                            if (duuid.equals(arg.getUuid())) {
                                return arg;
                            }
                            return null;
                        }
                    });
                    disks.add(DiskOfferingInventory.valueOf(dvo));
                }
                spec.setDataDiskOfferings(disks);
            } else {
                spec.setDataDiskOfferings(new ArrayList<DiskOfferingInventory>(0));
            }
            if (msg.getRootDiskOfferingUuid() != null) {
                DiskOfferingVO rootDisk = dbf.findByUuid(msg.getRootDiskOfferingUuid(), DiskOfferingVO.class);
                spec.setRootDiskOffering(DiskOfferingInventory.valueOf(rootDisk));
            }
            ImageVO imvo = dbf.findByUuid(spec.getVmInventory().getImageUuid(), ImageVO.class);
            spec.getImageSpec().setInventory(ImageInventory.valueOf(imvo));
            spec.setCurrentVmOperation(VmOperation.NewCreate);
            if (self.getZoneUuid() != null || self.getClusterUuid() != null || self.getHostUuid() != null) {
                spec.setHostAllocatorStrategy(HostAllocatorConstant.DESIGNATED_HOST_ALLOCATOR_STRATEGY_TYPE);
            }
            buildHostname(spec);

            changeVmStateInDb(VmInstanceStateEvent.starting);

            extEmitter.beforeStartNewCreatedVm(VmInstanceInventory.valueOf(self));
            FlowChain chain = getCreateVmWorkFlowChain(msg.getVmInstanceInventory());
            chain.setName(String.format("create-vm-%s", self.getUuid()));
            chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
            chain.done(new FlowDoneHandler(msg, taskChain) {
                @Override
                public void handle(final Map data) {
                    VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                    self.setLastHostUuid(spec.getDestHost().getUuid());
                    self.setHostUuid(spec.getDestHost().getUuid());
                    self.setClusterUuid(spec.getDestHost().getClusterUuid());
                    self.setZoneUuid(spec.getDestHost().getZoneUuid());
                    self.setHypervisorType(spec.getDestHost().getHypervisorType());
                    self.setRootVolumeUuid(spec.getDestRootVolume().getUuid());
                    changeVmStateInDb(VmInstanceStateEvent.running);
                    self = dbf.reload(self);
                    logger.debug(String.format("vm[uuid:%s] is running ..", self.getUuid()));
                    VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                    extEmitter.afterStartNewCreatedVm(inv);
                    StartNewCreatedVmInstanceReply reply = new StartNewCreatedVmInstanceReply();
                    reply.setVmInventory(inv);
                    bus.reply(msg, reply);
                    taskChain.next();
                }
            }).error(new FlowErrorHandler(msg, taskChain) {
                @Override
                public void handle(final ErrorCode errCode, Map data) {
                    extEmitter.failedToStartNewCreatedVm(VmInstanceInventory.valueOf(self), errCode);
                    dbf.remove(self);
                    StartNewCreatedVmInstanceReply reply = new StartNewCreatedVmInstanceReply();
                    reply.setError(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, errCode));
                    bus.reply(msg, reply);
                    taskChain.next();
                }
            }).start();

            callNext = false;
        } finally {
            if (callNext) {
                taskChain.next();
            }
        }
    }

    protected void handle(final StartNewCreatedVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("create-vm-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                startVmFromNewCreate(msg, chain);
            }

        });
    }

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIStopVmInstanceMsg) {
            handle((APIStopVmInstanceMsg) msg);
        } else if (msg instanceof APIRebootVmInstanceMsg) {
            handle((APIRebootVmInstanceMsg) msg);
        } else if (msg instanceof APIDestroyVmInstanceMsg) {
            handle((APIDestroyVmInstanceMsg) msg);
        } else if (msg instanceof APIStartVmInstanceMsg) {
            handle((APIStartVmInstanceMsg) msg);
        } else if (msg instanceof APIMigrateVmMsg) {
            handle((APIMigrateVmMsg) msg);
        } else if (msg instanceof APIAttachNicToVmMsg) {
            handle((APIAttachNicToVmMsg) msg);
        } else if (msg instanceof APIGetVmMigrationCandidateHostsMsg) {
            handle((APIGetVmMigrationCandidateHostsMsg) msg);
        } else if (msg instanceof APIGetVmAttachableDataVolumeMsg) {
            handle((APIGetVmAttachableDataVolumeMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Transactional(readOnly = true)
    private List<VolumeVO> getAttachableVolume() {
        List<String> volUuids = acntMgr.getSiblingResourceUuids(self.getUuid(), VmInstanceVO.class.getSimpleName(), VolumeVO.class.getSimpleName());
        if (volUuids.isEmpty()) {
            return new ArrayList<VolumeVO>();
        }

        List<String> formats = VolumeFormat.getVolumeFormatSupportedByHypervisorTypeInString(self.getHypervisorType());

        String sql = "select vol from VolumeVO vol, VmInstanceVO vm, PrimaryStorageClusterRefVO ref where vol.type = :type and vol.state = :volState and vol.status = :volStatus and vol.uuid in (:volUuids) and vol.format in (:formats) and vol.vmInstanceUuid is null and vm.clusterUuid = ref.clusterUuid and ref.primaryStorageUuid = vol.primaryStorageUuid group by vol.uuid";
        TypedQuery<VolumeVO> q = dbf.getEntityManager().createQuery(sql, VolumeVO.class);
        q.setParameter("volState", VolumeState.Enabled);
        q.setParameter("volStatus", VolumeStatus.Ready);
        q.setParameter("formats", formats);
        q.setParameter("volUuids", volUuids);
        q.setParameter("type", VolumeType.Data);
        List<VolumeVO> vos = q.getResultList();

        sql = "select vol from VolumeVO vol where vol.type = :type and vol.status = :volStatus and vol.state = :volState and vol.uuid in (:volUuids) group by vol.uuid";
        q = dbf.getEntityManager().createQuery(sql, VolumeVO.class);
        q.setParameter("type", VolumeType.Data);
        q.setParameter("volState", VolumeState.Enabled);
        q.setParameter("volStatus", VolumeStatus.NotInstantiated);
        q.setParameter("volUuids", volUuids);
        vos.addAll(q.getResultList());
        return vos;
    }

    private void handle(APIGetVmAttachableDataVolumeMsg msg) {
        APIGetVmAttachableDataVolumeReply reply = new APIGetVmAttachableDataVolumeReply();
        reply.setInventories(VolumeInventory.valueOf(getAttachableVolume()));
        bus.reply(msg, reply);
    }

    private void handle(final APIGetVmMigrationCandidateHostsMsg msg) {
        final APIGetVmMigrationCandidateHostsReply reply = new APIGetVmMigrationCandidateHostsReply();
        getVmMigrationTargetHost(msg, new ReturnValueCompletion<List<HostInventory>>(msg) {
            @Override
            public void success(List<HostInventory> returnValue) {
                reply.setInventories(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final APIAttachNicToVmMsg msg) {
        final APIAttachNicToVmEvent evt = new APIAttachNicToVmEvent(msg.getId());
        attachNic(msg, msg.getL3NetworkUuid(), new ReturnValueCompletion<VmNicInventory>(msg) {
            @Override
            public void success(VmNicInventory returnValue) {
                self = dbf.reload(self);
                evt.setInventory(VmInstanceInventory.valueOf(self));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setErrorCode(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void detachVolume(final DetachDataVolumeFromVmMsg msg, final NoErrorCompletion completion) {
        final DetachDataVolumeFromVmReply reply = new DetachDataVolumeFromVmReply();
        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), VmErrors.DETACH_VOLUME_ERROR);
        if (allowed != null) {
            reply.setError(allowed);
            bus.reply(msg, reply);
            return;
        }

        final VolumeInventory volume = msg.getVolume();
        extEmitter.preDetachVolume(getSelfInventory(), volume);
        extEmitter.beforeAttachVolume(getSelfInventory(), volume);

        if (self.getState() == VmInstanceState.Stopped) {
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        // VmInstanceState.Running
        String hostUuid = self.getHostUuid();
        DetachVolumeFromVmOnHypervisorMsg dmsg = new DetachVolumeFromVmOnHypervisorMsg();
        dmsg.setVmInventory(VmInstanceInventory.valueOf(self));
        dmsg.setInventory(volume);
        dmsg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(dmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(dmsg, new CloudBusCallBack(msg, completion) {
            @Override
            public void run(final MessageReply r) {
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                    extEmitter.failedToAttachVolume(getSelfInventory(), volume, r.getError());
                } else {
                    extEmitter.afterAttachVolume(getSelfInventory(), volume);
                }

                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    protected void attachVolume(final AttachDataVolumeToVmMsg msg, final NoErrorCompletion completion) {
        final AttachDataVolumeToVmReply reply = new AttachDataVolumeToVmReply();
        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), VmErrors.ATTACH_VOLUME_ERROR);
        if (allowed != null) {
            reply.setError(allowed);
            bus.reply(msg, reply);
            return;
        }

        final VolumeInventory volume = msg.getVolume();

        extEmitter.preAttachVolume(getSelfInventory(), volume);
        extEmitter.beforeAttachVolume(getSelfInventory(), volume);

        VmInstanceSpec spec = new VmInstanceSpec();
        spec.setMessage(msg);
        spec.setVmInventory(VmInstanceInventory.valueOf(self));
        FlowChain chain;
        if (volume.getStatus().equals(VolumeStatus.Ready.toString())) {
            chain = FlowChainBuilder.newSimpleFlowChain();
            chain.then(new VmAssignDeviceIdToAttachingVolumeFlow());
            chain.then(new VmAttachVolumeOnHypervisorFlow());
        } else {
            chain = getAttachUninstantiatedVolumeWorkFlowChain(spec.getVmInventory());
        }

        chain.setName(String.format("vm-%s-attach-volume-%s", self.getUuid(), volume.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.getData().put(VmInstanceConstant.Params.AttachingVolumeInventory.toString(), volume);
        chain.done(new FlowDoneHandler(msg, completion) {
            @Override
            public void handle(Map data) {
                extEmitter.afterAttachVolume(getSelfInventory(), volume);
                reply.setHypervisorType(self.getHypervisorType());
                bus.reply(msg, reply);
                completion.done();
            }
        }).error(new FlowErrorHandler(msg, completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                extEmitter.failedToAttachVolume(getSelfInventory(), volume, errCode);
                reply.setError(errf.instantiateErrorCode(VmErrors.ATTACH_VOLUME_ERROR, errCode));
                bus.reply(msg, reply);
                completion.done();
            }
        }).start();

    }

    protected void migrateVm(final Message msg, final Completion completion) {
        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), VmErrors.MIGRATE_ERROR);
        if (allowed != null) {
            completion.fail(allowed);
            return;
        }

        final VmInstanceState originState = self.getState();
        changeVmStateInDb(VmInstanceStateEvent.migrating);
        VmInstanceInventory inv = VmInstanceInventory.valueOf(self);

        final VmInstanceSpec spec = buildSpecFromInventory(inv);
        spec.setMessage(msg);
        spec.setCurrentVmOperation(VmOperation.Migrate);
        FlowChain chain = getMigrateVmWorkFlowChain(inv);
        chain.setName(String.format("migrate-vm-%s", self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(final Map data) {
                VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                HostInventory host = spec.getDestHost();
                self.setZoneUuid(host.getZoneUuid());
                self.setClusterUuid(host.getClusterUuid());
                self.setLastHostUuid(self.getHostUuid());
                self.setHostUuid(host.getUuid());
                self = changeVmStateInDb(VmInstanceStateEvent.running);
                VmInstanceInventory vm = VmInstanceInventory.valueOf(self);
                extEmitter.afterMigrateVm(vm, vm.getHostUuid());
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                if (HostErrors.FAILED_TO_MIGRATE_VM_ON_HYPERVISOR.isEqual(errCode.getCode())) {
                    self = changeVmStateInDb(VmInstanceStateEvent.unknown);
                } else {
                    self.setState(originState);
                    self = dbf.updateAndRefresh(self);
                }
                extEmitter.failedToMigrateVm(VmInstanceInventory.valueOf(self), spec.getDestHost().getUuid(), errCode);
                completion.fail(errCode);
            }
        }).start();
    }

    protected void handle(final APIMigrateVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("migrate-vm-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                migrateVm(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        APIMigrateVmEvent evt = new APIMigrateVmEvent(msg.getId());
                        evt.setInventory(VmInstanceInventory.valueOf(self));
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        APIMigrateVmEvent evt = new APIMigrateVmEvent(msg.getId());
                        evt.setErrorCode(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }
        });
    }

    protected void startVm(final Message msg, final Completion completion) {
        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), null);
        if (allowed != null) {
            completion.fail(allowed);
            return;
        }

        VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
        ErrorCode preStart = extEmitter.preStartVm(inv);
        if (preStart != null) {
            completion.fail(preStart);
            return;
        }

        final VmInstanceState originState = self.getState();
        changeVmStateInDb(VmInstanceStateEvent.starting);

        extEmitter.beforeStartVm(VmInstanceInventory.valueOf(self));
        final VmInstanceSpec spec = buildSpecFromInventory(inv);
        spec.setMessage(msg);
        spec.setCurrentVmOperation(VmOperation.Start);

        FlowChain chain = getStartVmWorkFlowChain(inv);
        chain.setName(String.format("start-vm-%s", self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(final Map data) {
                VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                // reload self because some nics may have been deleted in start phase because a former L3Network deletion.
                // reload to avoid JPA EntityNotFoundException
                self = dbf.reload(self);
                self.setLastHostUuid(self.getHostUuid());
                self.setHostUuid(spec.getDestHost().getUuid());
                self.setClusterUuid(spec.getDestHost().getClusterUuid());
                self.setZoneUuid(spec.getDestHost().getZoneUuid());
                self = changeVmStateInDb(VmInstanceStateEvent.running);
                logger.debug(String.format("vm[uuid:%s] is running ..", self.getUuid()));
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                extEmitter.afterStartVm(inv);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                // reload self because some nics may have been deleted in start phase because a former L3Network deletion.
                // reload to avoid JPA EntityNotFoundException
                self = dbf.reload(self);
                if (HostErrors.FAILED_TO_START_VM_ON_HYPERVISOR.isEqual(errCode.getCode())) {
                    self = changeVmStateInDb(VmInstanceStateEvent.unknown);
                } else {
                    self.setState(originState);
                    self = dbf.updateAndRefresh(self);
                }
                extEmitter.failedToStartVm(VmInstanceInventory.valueOf(self), errCode);
                completion.fail(errCode);
            }
        }).start();
    }

    protected void startVm(final StartVmInstanceMsg msg, final SyncTaskChain taskChain) {
        startVm(msg, new Completion(taskChain) {
            @Override
            public void success() {
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                StartVmInstanceReply reply = new StartVmInstanceReply();
                reply.setInventory(inv);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                StartVmInstanceReply reply = new StartVmInstanceReply();
                reply.setError(errf.instantiateErrorCode(VmErrors.START_ERROR, errorCode));
                bus.reply(msg, reply);
                taskChain.next();
            }
        });
    }

    protected void startVm(final APIStartVmInstanceMsg msg, final SyncTaskChain taskChain) {
        startVm(msg, new Completion(taskChain) {
            @Override
            public void success() {
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                APIStartVmInstanceEvent evt = new APIStartVmInstanceEvent(msg.getId());
                evt.setInventory(inv);
                bus.publish(evt);
                taskChain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                APIStartVmInstanceEvent evt = new APIStartVmInstanceEvent(msg.getId());
                evt.setErrorCode(errf.instantiateErrorCode(VmErrors.START_ERROR, errorCode));
                bus.publish(evt);
                taskChain.next();
            }
        });
    }

    protected void handle(final APIStartVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("start-vm-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                startVm(msg, chain);
            }
        });
    }

    protected void handle(final APIDestroyVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("destroy-vm-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final APIDestroyVmInstanceEvent evt = new APIDestroyVmInstanceEvent(msg.getId());
                destroyVm(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setErrorCode(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }
        });
    }

    private void destroyVm(APIDestroyVmInstanceMsg msg, final Completion completion) {
        final String issuer = VmInstanceVO.class.getSimpleName();
        final List<VmInstanceInventory> ctx = VmInstanceInventory.valueOf(Arrays.asList(self));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-vm-%s", msg.getUuid()));
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
                completion.success();
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errCode));
            }
        }).start();
    }

    protected void buildHostname(VmInstanceSpec spec) {
        String defaultHostname = VmSystemTags.HOSTNAME.getTag(self.getUuid());
        if (defaultHostname == null) {
            return;
        }

        HostName dhname = new HostName();
        dhname.setL3NetworkUuid(self.getDefaultL3NetworkUuid());
        dhname.setHostname(VmSystemTags.HOSTNAME.getTokenByTag(defaultHostname, VmSystemTags.HOSTNAME_TOKEN));
        spec.getHostnames().add(dhname);
    }

    protected VmInstanceSpec buildSpecFromInventory(VmInstanceInventory inv) {
        VmInstanceSpec spec = new VmInstanceSpec();

        // for L3Network that has been deleted
        List<String> nicUuidToDel = CollectionUtils.transformToList(inv.getVmNics(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                return arg.getL3NetworkUuid() == null ? arg.getUuid() : null;
            }
        });


        if (!nicUuidToDel.isEmpty()) {
            dbf.removeByPrimaryKeys(nicUuidToDel, VmNicVO.class);
            self = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);
            inv = VmInstanceInventory.valueOf(self);
        }

        spec.setDestNics(inv.getVmNics());
        List<String> l3Uuids = CollectionUtils.transformToList(inv.getVmNics(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                return arg.getL3NetworkUuid();
            }
        });
        spec.setL3Networks(L3NetworkInventory.valueOf(dbf.listByPrimaryKeys(l3Uuids, L3NetworkVO.class)));

        String huuid = inv.getHostUuid() == null ? inv.getLastHostUuid() : inv.getHostUuid();
        if (huuid != null) {
            HostVO hvo = dbf.findByUuid(huuid, HostVO.class);
            if (hvo != null) {
                spec.setDestHost(HostInventory.valueOf(hvo));
            }
        }

        List<VolumeInventory> dataVols = new ArrayList<VolumeInventory>();
        for (VolumeInventory vol : inv.getAllVolumes()) {
            if (vol.getUuid().equals(inv.getRootVolumeUuid())) {
                spec.setDestRootVolume(vol);
            } else {
                dataVols.add(vol);
            }
        }
        spec.setDestDataVolumes(dataVols);

        ImageVO imgvo = dbf.findByUuid(inv.getImageUuid(), ImageVO.class);
        ImageInventory imginv = null;
        if (imgvo == null) {
            // image has been deleted, use EO instead
            ImageEO imgeo = dbf.findByUuid(inv.getImageUuid(), ImageEO.class);
            imginv = ImageInventory.valueOf(imgeo);
        } else {
            imginv = ImageInventory.valueOf(imgvo);
        }
        spec.getImageSpec().setInventory(imginv);
        spec.setVmInventory(inv);
        buildHostname(spec);

        return spec;
    }


    protected void rebootVm(final Message msg, final Completion completion) {
        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), null);
        if (allowed != null) {
            completion.fail(allowed);
            return;
        }

        VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
        ErrorCode preReboot = extEmitter.preRebootVm(inv);
        if (preReboot != null) {
            completion.fail(preReboot);
            return;
        }

        final VmInstanceState originState = self.getState();
        changeVmStateInDb(VmInstanceStateEvent.rebooting);

        extEmitter.beforeRebootVm(VmInstanceInventory.valueOf(self));
        final VmInstanceSpec spec = buildSpecFromInventory(inv);
        spec.setMessage(msg);
        spec.setCurrentVmOperation(VmOperation.Reboot);
        FlowChain chain = getRebootVmWorkFlowChain(inv);
        chain.setName(String.format("reboot-vm-%s", self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                self = changeVmStateInDb(VmInstanceStateEvent.running);
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                extEmitter.afterRebootVm(inv);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                if (HostErrors.FAILED_TO_REBOOT_VM_ON_HYPERVISOR.isEqual(errCode.getCode())) {
                    self = changeVmStateInDb(VmInstanceStateEvent.unknown);
                } else {
                    self.setState(originState);
                    self = dbf.updateAndRefresh(self);
                }
                extEmitter.failedToRebootVm(VmInstanceInventory.valueOf(self), errCode);
                completion.fail(errCode);
            }
        }).start();
    }

    protected void rebootVm(final APIRebootVmInstanceMsg msg, final SyncTaskChain taskChain) {
        rebootVm(msg, new Completion(taskChain) {
            @Override
            public void success() {
                APIRebootVmInstanceEvent evt = new APIRebootVmInstanceEvent(msg.getId());
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                evt.setInventory(inv);
                bus.publish(evt);
                taskChain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                APIRebootVmInstanceEvent evt = new APIRebootVmInstanceEvent(msg.getId());
                evt.setErrorCode(errf.instantiateErrorCode(VmErrors.REBOOT_ERROR, errorCode));
                bus.publish(evt);
                taskChain.next();
            }
        });
    }

    protected void handle(final APIRebootVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("reboot-vm-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                rebootVm(msg, chain);
            }
        });
    }

    protected void stopVm(final APIStopVmInstanceMsg msg, final SyncTaskChain taskChain) {
        stopVm(msg, new Completion(taskChain) {
            @Override
            public void success() {
                APIStopVmInstanceEvent evt = new APIStopVmInstanceEvent(msg.getId());
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                evt.setInventory(inv);
                bus.publish(evt);
                taskChain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                APIStopVmInstanceEvent evt = new APIStopVmInstanceEvent(msg.getId());
                evt.setErrorCode(errf.instantiateErrorCode(VmErrors.STOP_ERROR, errorCode));
                bus.publish(evt);
                taskChain.next();
            }
        });
    }

    private void stopVm(final Message msg, final Completion completion) {
        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), null);
        if (allowed != null) {
            completion.fail(allowed);
            return;
        }

        if (self.getState() == VmInstanceState.Stopped) {
            completion.success();
            return;
        }

        VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
        ErrorCode preStop = extEmitter.preStopVm(inv);
        if (preStop != null) {
            completion.fail(preStop);
            return;
        }

        final VmInstanceState originState = self.getState();
        changeVmStateInDb(VmInstanceStateEvent.stopping);

        extEmitter.beforeStopVm(VmInstanceInventory.valueOf(self));
        final VmInstanceSpec spec = buildSpecFromInventory(inv);
        spec.setMessage(msg);
        spec.setCurrentVmOperation(VmOperation.Stop);
        FlowChain chain = getStopVmWorkFlowChain(inv);
        chain.setName(String.format("stop-vm-%s", self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                self.setLastHostUuid(self.getHostUuid());
                self.setHostUuid(null);
                self = changeVmStateInDb(VmInstanceStateEvent.stopped);
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                extEmitter.afterStopVm(inv);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                if (HostErrors.FAILED_TO_STOP_VM_ON_HYPERVISOR.isEqual(errCode.getCode())) {
                    self = changeVmStateInDb(VmInstanceStateEvent.unknown);
                } else {
                    self.setState(originState);
                    self = dbf.updateAndRefresh(self);
                }
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                extEmitter.failedToStopVm(inv, errCode);
                completion.fail(errCode);
            }
        }).start();
    }

    protected void handle(final APIStopVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("stop-vm-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                stopVm(msg, chain);
            }
        });
    }
}
