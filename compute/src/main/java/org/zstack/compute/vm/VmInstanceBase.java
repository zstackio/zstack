package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.allocator.HostAllocatorManager;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.scheduler.SchedulerFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.allocator.AllocateHostDryRunReply;
import org.zstack.header.allocator.DesignatedAllocateHostMsg;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostAllocatorError;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.configuration.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.core.scheduler.SchedulerVO;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageEO;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.*;
import org.zstack.header.network.l3.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.*;
import org.zstack.header.vm.ChangeVmMetaDataMsg.AtomicHostUuid;
import org.zstack.header.vm.ChangeVmMetaDataMsg.AtomicVmState;
import org.zstack.header.vm.VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation;
import org.zstack.header.vm.VmInstanceConstant.Capability;
import org.zstack.header.vm.VmInstanceConstant.Params;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceSpec.HostName;
import org.zstack.header.vm.VmInstanceSpec.IsoSpec;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.*;


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
    protected VmInstanceNotifyPointEmitter notifyEmitter;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected AccountManager acntMgr;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected VmInstanceDeletionPolicyManager deletionPolicyMgr;
    @Autowired
    private SchedulerFacade schedulerFacade;
    @Autowired
    private HostAllocatorManager hostAllocatorMgr;

    protected VmInstanceVO self;
    protected VmInstanceVO originalCopy;
    protected String syncThreadName;

    private void checkState(final String hostUuid, final NoErrorCompletion completion) {
        CheckVmStateOnHypervisorMsg msg = new CheckVmStateOnHypervisorMsg();
        msg.setVmInstanceUuids(list(self.getUuid()));
        msg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    //TODO
                    logger.warn(String.format("unable to check state of the vm[uuid:%s] on the host[uuid:%s], %s." +
                                    " Put the vm to Unknown state",
                            self.getUuid(), hostUuid, reply.getError()));
                    self = dbf.reload(self);
                    changeVmStateInDb(VmInstanceStateEvent.unknown);
                    completion.done();
                    return;
                }

                CheckVmStateOnHypervisorReply r = reply.castReply();
                String state = r.getStates().get(self.getUuid());
                self = dbf.reload(self);
                if (VmInstanceState.Running.toString().equals(state)) {
                    self.setHostUuid(hostUuid);
                    changeVmStateInDb(VmInstanceStateEvent.running);
                } else if (VmInstanceState.Stopped.toString().equals(state)) {
                    changeVmStateInDb(VmInstanceStateEvent.stopped);
                } else {
                    throw new CloudRuntimeException(String.format(
                            "CheckVmStateOnHypervisorMsg should only report states[Running or Stopped]," +
                                    "but it reports %s for the vm[uuid:%s] on the host[uuid:%s]", state, self.getUuid(), hostUuid));
                }

                completion.done();
            }
        });
    }

    protected void destroy(final VmInstanceDeletionPolicy deletionPolicy, final Completion completion) {
        if (VmInstanceState.Created == self.getState()) {
            // the vm is only created in DB, no need to go through normal destroying process
            completion.success();
            return;
        }

        final VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
        VmInstanceSpec spec = buildSpecFromInventory(inv, VmOperation.Destroy);

        self = changeVmStateInDb(VmInstanceStateEvent.destroying);

        FlowChain chain = getDestroyVmWorkFlowChain(inv);
        setFlowMarshaller(chain);

        chain.setName(String.format("destroy-vm-%s", self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.getData().put(Params.DeletionPolicy, deletionPolicy);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                if (originalCopy.getState() == VmInstanceState.Running) {
                    checkState(originalCopy.getHostUuid(), new NoErrorCompletion(completion) {
                        @Override
                        public void done() {
                            completion.fail(errCode);
                        }
                    });
                } else {
                    self = dbf.reload(self);
                    changeVmStateInDb(VmInstanceStateEvent.unknown);
                    completion.fail(errCode);
                }
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
        this.originalCopy = ObjectUtils.newAndCopy(vo, vo.getClass());
    }

    protected VmInstanceVO refreshVO() {
        return refreshVO(false);
    }

    protected VmInstanceVO refreshVO(boolean noException) {
        VmInstanceVO vo = self;
        self = dbf.findByUuid(self.getUuid(), VmInstanceVO.class);
        if (self == null && noException) {
            return null;
        }

        if (self == null) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("vm[uuid:%s, name:%s] has been deleted", vo.getUuid(), vo.getName())));
        }

        originalCopy = ObjectUtils.newAndCopy(vo, vo.getClass());
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

    protected FlowChain getExpungeVmWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getExpungeVmWorkFlowChain(inv);
    }

    protected FlowChain getMigrateVmWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getMigrateVmWorkFlowChain(inv);
    }

    protected FlowChain getAttachUninstantiatedVolumeWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getAttachUninstantiatedVolumeWorkFlowChain(inv);
    }

    protected FlowChain getAttachIsoWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getAttachIsoWorkFlowChain(inv);
    }

    protected FlowChain getDetachIsoWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getDetachIsoWorkFlowChain(inv);
    }

    protected FlowChain getSuspendVmWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getSuspendWorkFlowChain(inv);
    }

    protected FlowChain getResumeVmWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getResumeVmWorkFlowChain(inv);
    }

    protected VmInstanceVO changeVmStateInDb(VmInstanceStateEvent stateEvent) {
        VmInstanceState bs = self.getState();
        final VmInstanceState state = self.getState().nextState(stateEvent);

        if (state == VmInstanceState.Stopped) {
            // cleanup the hostUuid if the VM is stopped
            if (self.getHostUuid() != null) {
                self.setLastHostUuid(self.getHostUuid());
            }
            self.setHostUuid(null);
        }

        self.setState(state);
        self = dbf.updateAndRefresh(self);

        if (bs != state) {
            logger.debug(String.format("vm[uuid:%s] changed state from %s to %s", self.getUuid(), bs, self.getState()));

            VmCanonicalEvents.VmStateChangedData data = new VmCanonicalEvents.VmStateChangedData();
            data.setVmUuid(self.getUuid());
            data.setOldState(bs.toString());
            data.setNewState(state.toString());
            data.setInventory(getSelfInventory());
            evtf.fire(VmCanonicalEvents.VM_FULL_STATE_CHANGED_PATH, data);

            VmInstanceInventory inv = getSelfInventory();
            CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmStateChangedExtensionPoint.class),
                    new ForEachFunction<VmStateChangedExtensionPoint>() {
                        @Override
                        public void run(VmStateChangedExtensionPoint ext) {
                            ext.vmStateChanged(inv, bs, self.getState());
                        }
                    });

            //TODO: remove this
            notifyEmitter.notifyVmStateChange(VmInstanceInventory.valueOf(self), bs, state);
        }

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
            handle((DestroyVmInstanceMsg) msg);
        } else if (msg instanceof AttachNicToVmMsg) {
            handle((AttachNicToVmMsg) msg);
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
        } else if (msg instanceof DetachNicFromVmMsg) {
            handle((DetachNicFromVmMsg) msg);
        } else if (msg instanceof VmStateChangedOnHostMsg) {
            handle((VmStateChangedOnHostMsg) msg);
        } else if (msg instanceof VmCheckOwnStateMsg) {
            handle((VmCheckOwnStateMsg) msg);
        } else if (msg instanceof ExpungeVmMsg) {
            handle((ExpungeVmMsg) msg);
        } else if (msg instanceof HaStartVmInstanceMsg) {
            handle((HaStartVmInstanceMsg) msg);
        } else {
            VmInstanceBaseExtensionFactory ext = vmMgr.getVmInstanceBaseExtensionFactory(msg);
            if (ext != null) {
                VmInstance v = ext.getVmInstance(self);
                v.handleMessage(msg);
            } else {
                bus.dealWithUnknownMessage(msg);
            }
        }
    }

    private void handle(final APIGetVmStartingCandidateClustersHostsMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                getStartingCandidateHosts(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "get-starting-candidate-hosts";
            }
        });
    }

    private void getStartingCandidateHosts(final APIGetVmStartingCandidateClustersHostsMsg msg, final NoErrorCompletion completion) {
        refreshVO();
        ErrorCode err = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
        if (err != null) {
            throw new OperationFailureException(err);
        }

        final DesignatedAllocateHostMsg amsg = new DesignatedAllocateHostMsg();
        amsg.setCpuCapacity(self.getCpuNum());
        amsg.setMemoryCapacity(self.getMemorySize());
        amsg.setVmInstance(VmInstanceInventory.valueOf(self));
        amsg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        amsg.setAllocatorStrategy(self.getAllocatorStrategy());
        amsg.setVmOperation(VmOperation.Start.toString());
        amsg.setL3NetworkUuids(CollectionUtils.transformToList(self.getVmNics(), new Function<String, VmNicVO>() {
            @Override
            public String call(VmNicVO arg) {
                return arg.getL3NetworkUuid();
            }
        }));
        amsg.setDryRun(true);
        amsg.setListAllHosts(true);

        final APIGetVmStartingCandidateClustersHostsReply reply = new APIGetVmStartingCandidateClustersHostsReply();
        bus.send(amsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply re) {
                if (!re.isSuccess()) {
                    if (HostAllocatorError.NO_AVAILABLE_HOST.toString().equals(re.getError().getCode())) {
                        reply.setHostInventories(new ArrayList<>());
                        reply.setClusterInventories(new ArrayList<>());
                    } else {
                        reply.setError(re.getError());
                    }
                } else {
                    List<HostInventory> hosts = ((AllocateHostDryRunReply) re).getHosts();
                    if (!hosts.isEmpty()) {
                        List<String> cuuids = CollectionUtils.transformToList(hosts, new Function<String, HostInventory>() {
                            @Override
                            public String call(HostInventory arg) {
                                return arg.getClusterUuid();
                            }
                        });

                        SimpleQuery<ClusterVO> cq = dbf.createQuery(ClusterVO.class);
                        cq.add(ClusterVO_.uuid, Op.IN, cuuids);
                        List<ClusterVO> cvos = cq.list();

                        reply.setClusterInventories(ClusterInventory.valueOf(cvos));
                        reply.setHostInventories(hosts);
                    } else {
                        reply.setHostInventories(hosts);
                        reply.setClusterInventories(new ArrayList<>());
                    }
                }

                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    private void handle(final HaStartVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                refreshVO();

                HaStartVmJudger judger;
                try {
                    Class clz = Class.forName(msg.getJudgerClassName());
                    judger = (HaStartVmJudger) clz.newInstance();
                } catch (Exception e) {
                    throw new CloudRuntimeException(e);
                }

                final HaStartVmInstanceReply reply = new HaStartVmInstanceReply();
                if (!judger.whetherStartVm(getSelfInventory())) {
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                logger.debug(String.format("HaStartVmJudger[%s] says the VM[uuid:%s, name:%s] is qualified for HA start, now we are starting it",
                        judger.getClass(), self.getUuid(), self.getName()));
                self.setState(VmInstanceState.Stopped);
                dbf.update(self);
                startVm(msg, new Completion(msg, chain) {
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
                return "ha-start-vm";
            }
        });
    }

    private void changeVmIp(final String l3Uuid, final String ip, final Completion completion) {
        final VmNicVO targetNic = CollectionUtils.find(self.getVmNics(), new Function<VmNicVO, VmNicVO>() {
            @Override
            public VmNicVO call(VmNicVO arg) {
                return l3Uuid.equals(arg.getL3NetworkUuid()) ? arg : null;
            }
        });

        if (targetNic == null) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("the vm[uuid:%s] has no nic on the L3 network[uuid:%s]", self.getUuid(), l3Uuid)
            ));
        }

        if (ip.equals(targetNic.getIp())) {
            completion.success();
            return;
        }

        final UsedIpInventory oldIp = new UsedIpInventory();
        oldIp.setIp(targetNic.getIp());
        oldIp.setGateway(targetNic.getGateway());
        oldIp.setNetmask(targetNic.getNetmask());
        oldIp.setL3NetworkUuid(targetNic.getL3NetworkUuid());
        oldIp.setUuid(targetNic.getUsedIpUuid());

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("change-vm-ip-to-%s-l3-%s-vm-%s", ip, l3Uuid, self.getUuid()));
        chain.then(new ShareFlow() {
            UsedIpInventory newIp;
            String oldIpUuid = targetNic.getUsedIpUuid();

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "acquire-new-ip";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        AllocateIpMsg amsg = new AllocateIpMsg();
                        amsg.setL3NetworkUuid(l3Uuid);
                        amsg.setRequiredIp(ip);
                        bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, l3Uuid);
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    AllocateIpReply r = reply.castReply();
                                    newIp = r.getIpInventory();
                                    trigger.next();
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (newIp != null) {
                            ReturnIpMsg rmsg = new ReturnIpMsg();
                            rmsg.setL3NetworkUuid(newIp.getL3NetworkUuid());
                            rmsg.setUsedIpUuid(newIp.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, newIp.getL3NetworkUuid());
                            bus.send(rmsg);
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "change-ip-in-database";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        targetNic.setUsedIpUuid(newIp.getUuid());
                        targetNic.setGateway(newIp.getGateway());
                        targetNic.setNetmask(newIp.getNetmask());
                        targetNic.setIp(newIp.getIp());
                        dbf.update(targetNic);
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "return-old-ip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ReturnIpMsg rmsg = new ReturnIpMsg();
                        rmsg.setUsedIpUuid(oldIpUuid);
                        rmsg.setL3NetworkUuid(targetNic.getL3NetworkUuid());
                        bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, targetNic.getL3NetworkUuid());
                        bus.send(rmsg);
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        final VmInstanceInventory vm = getSelfInventory();
                        final VmNicInventory nic = VmNicInventory.valueOf(targetNic);
                        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmIpChangedExtensionPoint.class),
                                new ForEachFunction<VmIpChangedExtensionPoint>() {
                                    @Override
                                    public void run(VmIpChangedExtensionPoint ext) {
                                        ext.vmIpChanged(vm, nic, oldIp, newIp);
                                    }
                                });

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

    private void handle(final ExpungeVmMsg msg) {
        final ExpungeVmReply reply = new ExpungeVmReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                expunge(msg, new Completion(msg, chain) {
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
                return "expunge-vm";
            }
        });
    }

    private void expunge(Message msg, final Completion completion) {
        refreshVO();
        final VmInstanceInventory inv = getSelfInventory();
        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmBeforeExpungeExtensionPoint.class),
                new ForEachFunction<VmBeforeExpungeExtensionPoint>() {
                    @Override
                    public void run(VmBeforeExpungeExtensionPoint arg) {
                        arg.vmBeforeExpunge(inv);
                    }
                });

        ErrorCode error = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
        if (error != null) {
            throw new OperationFailureException(error);
        }

        if (inv.getAllVolumes().size() > 1) {
            throw new CloudRuntimeException(String.format("why the deleted vm[uuid:%s] has data volumes??? %s",
                    self.getUuid(), JSONObjectUtil.toJsonString(inv.getAllVolumes())));
        }

        VmInstanceSpec spec = buildSpecFromInventory(inv, VmOperation.Expunge);
        FlowChain chain = getExpungeVmWorkFlowChain(inv);
        setFlowMarshaller(chain);
        chain.setName(String.format("destroy-vm-%s", self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.getData().put(Params.DeletionPolicy, VmInstanceDeletionPolicy.Direct);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                dbf.removeCollection(self.getVmNics(), VmNicVO.class);
                dbf.remove(self);
                logger.debug(String.format("successfully expunged the vm[uuid:%s]", self.getUuid()));
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(final VmCheckOwnStateMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                refreshVO();
                final VmCheckOwnStateReply reply = new VmCheckOwnStateReply();
                if (self.getHostUuid() == null) {
                    // no way to check
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                final CheckVmStateOnHypervisorMsg cmsg = new CheckVmStateOnHypervisorMsg();
                cmsg.setVmInstanceUuids(list(self.getUuid()));
                cmsg.setHostUuid(self.getHostUuid());
                bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, self.getHostUuid());
                bus.send(cmsg, new CloudBusCallBack(msg, chain) {
                    @Override
                    public void run(MessageReply r) {
                        if (!r.isSuccess()) {
                            reply.setError(r.getError());
                            bus.reply(msg, r);
                            chain.next();
                            return;
                        }

                        CheckVmStateOnHypervisorReply cr = r.castReply();
                        String s = cr.getStates().get(self.getUuid());
                        VmInstanceState state = VmInstanceState.valueOf(s);
                        if (state != self.getState()) {
                            VmStateChangedOnHostMsg vcmsg = new VmStateChangedOnHostMsg();
                            vcmsg.setHostUuid(self.getHostUuid());
                            vcmsg.setVmInstanceUuid(self.getUuid());
                            vcmsg.setStateOnHost(state);
                            bus.makeTargetServiceIdByResourceUuid(vcmsg, VmInstanceConstant.SERVICE_ID, self.getUuid());
                            bus.send(vcmsg);
                        }

                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "check-state";
            }
        });
    }

    private void handle(final VmStateChangedOnHostMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                vmStateChangeOnHost(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("vm-%s-state-change-on-the-host-%s", self.getUuid(), msg.getHostUuid());
            }
        });
    }

    private VmAbnormalLifeCycleOperation getVmAbnormalLifeCycleOperation(String originalHostUuid,
                                                                         String currentHostUuid,
                                                                         VmInstanceState originalState,
                                                                         VmInstanceState currentState) {
        if (originalState == VmInstanceState.Stopped && currentState == VmInstanceState.Running) {
            return VmAbnormalLifeCycleOperation.VmRunningOnTheHost;
        }

        if (originalState == VmInstanceState.Running && currentState == VmInstanceState.Stopped &&
                currentHostUuid.equals(originalHostUuid)) {
            return VmAbnormalLifeCycleOperation.VmStoppedOnTheSameHost;
        }

        if (VmInstanceState.intermediateStates.contains(originalState) && currentState == VmInstanceState.Running) {
            return VmAbnormalLifeCycleOperation.VmRunningFromIntermediateState;
        }

        if (VmInstanceState.intermediateStates.contains(originalState) && currentState == VmInstanceState.Stopped) {
            return VmAbnormalLifeCycleOperation.VmStoppedFromIntermediateState;
        }

        if (originalState == VmInstanceState.Unknown && currentState == VmInstanceState.Running &&
                currentHostUuid.equals(originalHostUuid)) {
            return VmAbnormalLifeCycleOperation.VmRunningFromUnknownStateHostNotChanged;
        }

        if (originalState == VmInstanceState.Unknown && currentState == VmInstanceState.Running &&
                !currentHostUuid.equals(originalHostUuid)) {
            return VmAbnormalLifeCycleOperation.VmRunningFromUnknownStateHostChanged;
        }

        if (originalState == VmInstanceState.Unknown && currentState == VmInstanceState.Stopped &&
                currentHostUuid.equals(originalHostUuid)) {
            return VmAbnormalLifeCycleOperation.VmStoppedOnTheSameHost;
        }

        if (originalState == VmInstanceState.Unknown && currentState == VmInstanceState.Stopped
                && originalHostUuid == null && currentHostUuid.equals(self.getLastHostUuid())) {
            return VmAbnormalLifeCycleOperation.VmStoppedFromUnknownStateHostNotChanged;
        }

        if (originalState == VmInstanceState.Running &&
                originalState == currentState &&
                !currentHostUuid.equals(originalHostUuid)) {
            return VmAbnormalLifeCycleOperation.VmMigrateToAnotherHost;
        }

        throw new CloudRuntimeException(String.format("unknown VM[uuid:%s] abnormal state combination[original state: %s," +
                        " current state: %s, original host:%s, current host:%s]",
                self.getUuid(), originalState, currentState, originalHostUuid, currentHostUuid));
    }

    private void vmStateChangeOnHost(final VmStateChangedOnHostMsg msg, final NoErrorCompletion completion) {
        final VmStateChangedOnHostReply reply = new VmStateChangedOnHostReply();
        if (refreshVO(true) == null) {
            // the vm has been deleted
            reply.setError(errf.stringToOperationError("the vm has been deleted"));
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        if (msg.getVmStateAtTracingMoment() != null) {
            // the vm tracer periodically reports vms's state. It catches an old state
            // before an vm operation(start, stop, reboot, migrate) completes. Ignore this
            VmInstanceState expected = VmInstanceState.valueOf(msg.getVmStateAtTracingMoment());
            if (expected != self.getState()) {
                bus.reply(msg, reply);
                completion.done();
                return;
            }
        }

        final String originalHostUuid = self.getHostUuid();
        final String currentHostUuid = msg.getHostUuid();
        final VmInstanceState originalState = self.getState();
        final VmInstanceState currentState = VmInstanceState.valueOf(msg.getStateOnHost());

        if (originalState == currentState && originalHostUuid != null && currentHostUuid.equals(originalHostUuid)) {
            logger.debug(String.format("vm[uuid:%s]'s state[%s] is inline with its state on the host[uuid:%s], ignore VmStateChangeOnHostMsg",
                    self.getUuid(), originalState, originalHostUuid));
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        if (originalState == VmInstanceState.Stopped && currentState == VmInstanceState.Unknown) {
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        final Runnable fireEvent = new Runnable() {
            @Override
            public void run() {
                VmTracerCanonicalEvents.VmStateChangedOnHostData data = new VmTracerCanonicalEvents.VmStateChangedOnHostData();
                data.setVmUuid(self.getUuid());
                data.setFrom(originalState);
                data.setTo(self.getState());
                data.setOriginalHostUuid(originalHostUuid);
                data.setCurrentHostUuid(self.getHostUuid());
                evtf.fire(VmTracerCanonicalEvents.VM_STATE_CHANGED_PATH, data);
            }
        };

        if (currentState == VmInstanceState.Unknown) {
            changeVmStateInDb(VmInstanceStateEvent.unknown);
            fireEvent.run();
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        VmAbnormalLifeCycleOperation operation = getVmAbnormalLifeCycleOperation(originalHostUuid,
                currentHostUuid, originalState, currentState);
        if (operation == VmAbnormalLifeCycleOperation.VmRunningFromUnknownStateHostNotChanged) {
            // the vm is detected on the host again. It's largely because the host disconnected before
            // and now reconnected
            self.setHostUuid(msg.getHostUuid());
            changeVmStateInDb(VmInstanceStateEvent.running);
            fireEvent.run();
            bus.reply(msg, reply);
            completion.done();
            return;
        } else if (operation == VmAbnormalLifeCycleOperation.VmStoppedFromUnknownStateHostNotChanged) {
            // the vm comes out of the unknown state to the stopped state
            // it happens when an operation failure led the vm from the stopped state to the unknown state,
            // and later on the vm was detected as stopped on the host again
            self.setHostUuid(null);
            changeVmStateInDb(VmInstanceStateEvent.stopped);
            fireEvent.run();
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        List<VmAbnormalLifeCycleExtensionPoint> exts = pluginRgty.getExtensionList(VmAbnormalLifeCycleExtensionPoint.class);

        VmAbnormalLifeCycleStruct struct = new VmAbnormalLifeCycleStruct();
        struct.setCurrentHostUuid(currentHostUuid);
        struct.setCurrentState(currentState);
        struct.setOriginalHostUuid(originalHostUuid);
        struct.setOriginalState(originalState);
        struct.setVmInstance(getSelfInventory());
        struct.setOperation(operation);

        logger.debug(String.format("the vm[uuid:%s]'s state changed abnormally on the host[uuid:%s]," +
                        " ZStack is going to take the operation[%s]," +
                        "[original state: %s, current state: %s, original host: %s, current host:%s]",
                self.getUuid(), currentHostUuid, operation, originalState, currentState, originalHostUuid, currentHostUuid));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("handle-abnormal-lifecycle-of-vm-%s", self.getUuid()));
        chain.getData().put(Params.AbnormalLifeCycleStruct, struct);
        chain.allowEmptyFlow();
        for (VmAbnormalLifeCycleExtensionPoint ext : exts) {
            Flow flow = ext.createVmAbnormalLifeCycleHandlingFlow(struct);
            chain.then(flow);
        }

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                if (currentState == VmInstanceState.Running) {
                    self.setHostUuid(currentHostUuid);
                    changeVmStateInDb(VmInstanceStateEvent.running);
                } else if (currentState == VmInstanceState.Stopped) {
                    self.setHostUuid(null);
                    changeVmStateInDb(VmInstanceStateEvent.stopped);
                }

                fireEvent.run();
                bus.reply(msg, reply);
                completion.done();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                //TODO
                logger.warn(String.format("failed to handle abnormal lifecycle of the vm[uuid:%s, original state: %s, current state:%s," +
                                "original host: %s, current host: %s], %s", self.getUuid(), originalState, currentState,
                        originalHostUuid, currentHostUuid, errCode));
                reply.setError(errCode);
                bus.reply(msg, reply);
                completion.done();
            }
        }).start();
    }

    private String buildUserdata() {
        return new UserdataBuilder().buildByVmUuid(self.getUuid());
    }

    private void handle(final DetachNicFromVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final DetachNicFromVmReply reply = new DetachNicFromVmReply();

                refreshVO();

                if (self.getState() == VmInstanceState.Destroyed) {
                    // the cascade framework may send this message when
                    // the vm has been destroyed
                    VmNicVO nic = CollectionUtils.find(self.getVmNics(), new Function<VmNicVO, VmNicVO>() {
                        @Override
                        public VmNicVO call(VmNicVO arg) {
                            return msg.getVmNicUuid().equals(arg.getUuid()) ? arg : null;
                        }
                    });

                    if (nic != null) {
                        dbf.remove(nic);
                    }

                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                final ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    reply.setError(allowed);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                detachNic(msg.getVmNicUuid(), new Completion(msg, chain) {
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
                return "detach-nic";
            }
        });
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
                            logger.debug(String.format("unlocked vm[uuid:%s] that was locked by %s",
                                    self.getUuid(), msg.getReason()));
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
        amsg.setCpuCapacity(self.getCpuNum());
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
        amsg.setAllowNoL3Networks(true);

        bus.send(amsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply re) {
                if (!re.isSuccess()) {
                    if (HostAllocatorError.NO_AVAILABLE_HOST.toString().equals(re.getError().getCode())) {
                        completion.success(new ArrayList<HostInventory>());
                    } else {
                        completion.fail(re.getError());
                    }
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
                attachDataVolume(msg, new NoErrorCompletion(chain) {
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
            @Deferred
            public void run(final SyncTaskChain chain) {
                refreshVO();
                ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    completion.fail(allowed);
                    return;
                }

                class SetDefaultL3Network {
                    private boolean isSet = false;

                    void set() {
                        if (self.getDefaultL3NetworkUuid() == null) {
                            self.setDefaultL3NetworkUuid(l3Uuid);
                            self = dbf.updateAndRefresh(self);
                            isSet = true;
                        }
                    }

                    void rollback() {
                        if (isSet) {
                            self.setDefaultL3NetworkUuid(null);
                            dbf.update(self);
                        }
                    }
                }

                class SetStaticIp {
                    private boolean isSet = false;

                    void set() {
                        if (!(msg instanceof APIAttachL3NetworkToVmMsg)) {
                            return;
                        }

                        APIAttachL3NetworkToVmMsg amsg = (APIAttachL3NetworkToVmMsg) msg;
                        if (amsg.getStaticIp() == null) {
                            return;
                        }

                        new StaticIpOperator().setStaticIp(self.getUuid(), amsg.getL3NetworkUuid(), amsg.getStaticIp());

                        isSet = true;
                    }

                    void rollback() {
                        if (isSet) {
                            APIAttachL3NetworkToVmMsg amsg = (APIAttachL3NetworkToVmMsg) msg;
                            new StaticIpOperator().deleteStaticIpByVmUuidAndL3Uuid(self.getUuid(), amsg.getL3NetworkUuid());
                        }
                    }
                }

                final SetDefaultL3Network setDefaultL3Network = new SetDefaultL3Network();
                setDefaultL3Network.set();
                Defer.guard(new Runnable() {
                    @Override
                    public void run() {
                        setDefaultL3Network.rollback();
                    }
                });

                final SetStaticIp setStaticIp = new SetStaticIp();
                setStaticIp.set();
                Defer.guard(new Runnable() {
                    @Override
                    public void run() {
                        setStaticIp.rollback();
                    }
                });

                final VmInstanceSpec spec = buildSpecFromInventory(getSelfInventory(), VmOperation.AttachNic);
                spec.setVmInventory(VmInstanceInventory.valueOf(self));
                L3NetworkVO l3vo = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
                final L3NetworkInventory l3 = L3NetworkInventory.valueOf(l3vo);
                final VmInstanceInventory vm = getSelfInventory();
                for (VmPreAttachL3NetworkExtensionPoint ext : pluginRgty.getExtensionList(VmPreAttachL3NetworkExtensionPoint.class)) {
                    ext.vmPreAttachL3Network(vm, l3);
                }

                spec.setL3Networks(list(l3));
                spec.setDestNics(new ArrayList<VmNicInventory>());

                CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmBeforeAttachL3NetworkExtensionPoint.class),
                        new ForEachFunction<VmBeforeAttachL3NetworkExtensionPoint>() {
                            @Override
                            public void run(VmBeforeAttachL3NetworkExtensionPoint arg) {
                                arg.vmBeforeAttachL3Network(vm, l3);
                            }
                        });

                FlowChain flowChain = FlowChainBuilder.newSimpleFlowChain();
                setFlowMarshaller(flowChain);
                flowChain.setName(String.format("attachNic-vm-%s-l3-%s", self.getUuid(), l3Uuid));
                flowChain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
                flowChain.then(new VmAllocateNicFlow());
                flowChain.then(new VmSetDefaultL3NetworkOnAttachingFlow());
                if (self.getState() == VmInstanceState.Running) {
                    flowChain.then(new VmInstantiateResourceOnAttachingNicFlow());
                    flowChain.then(new VmAttachNicOnHypervisorFlow());
                }

                flowChain.done(new FlowDoneHandler(chain) {
                    @Override
                    public void handle(Map data) {
                        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmAfterAttachL3NetworkExtensionPoint.class),
                                new ForEachFunction<VmAfterAttachL3NetworkExtensionPoint>() {
                                    @Override
                                    public void run(VmAfterAttachL3NetworkExtensionPoint arg) {
                                        arg.vmAfterAttachL3Network(vm, l3);
                                    }
                                });
                        VmNicInventory nic = spec.getDestNics().get(0);
                        completion.success(nic);
                        chain.next();
                    }
                }).error(new FlowErrorHandler(chain) {
                    @Override
                    public void handle(final ErrorCode errCode, Map data) {
                        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmFailToAttachL3NetworkExtensionPoint.class),
                                new ForEachFunction<VmFailToAttachL3NetworkExtensionPoint>() {
                                    @Override
                                    public void run(VmFailToAttachL3NetworkExtensionPoint arg) {
                                        arg.vmFailToAttachL3Network(vm, l3, errCode);
                                    }
                                });
                        setDefaultL3Network.rollback();
                        setStaticIp.rollback();
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

    protected void doDestroy(final VmInstanceDeletionPolicy deletionPolicy, final Completion completion) {
        final VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
        extEmitter.beforeDestroyVm(inv);

        destroy(deletionPolicy, new Completion(completion) {
            @Override
            public void success() {
                extEmitter.afterDestroyVm(inv);
                logger.debug(String.format("successfully deleted vm instance[name:%s, uuid:%s]", self.getName(), self.getUuid()));
                if (deletionPolicy == VmInstanceDeletionPolicy.Direct) {
                    dbf.remove(getSelf());
                } else if (deletionPolicy == VmInstanceDeletionPolicy.Delay) {
                    self = dbf.reload(self);
                    self.setHostUuid(null);
                    changeVmStateInDb(VmInstanceStateEvent.destroyed);
                } else if (deletionPolicy == VmInstanceDeletionPolicy.Never) {
                    logger.warn(String.format("the vm[uuid:%s] is deleted, but by it's deletion policy[Never]," +
                            " the root volume is not deleted on the primary storage", self.getUuid()));
                    self = dbf.reload(self);
                    self.setHostUuid(null);
                    changeVmStateInDb(VmInstanceStateEvent.destroyed);
                }

                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                extEmitter.failedToDestroyVm(inv, errorCode);
                logger.debug(String.format("failed to delete vm instance[name:%s, uuid:%s], because %s",
                        self.getName(), self.getUuid(), errorCode));
                completion.fail(errorCode);
            }
        });
    }

    private void handle(final VmInstanceDeletionMsg msg) {
        final VmInstanceDeletionReply r = new VmInstanceDeletionReply();
        self = dbf.findByUuid(self.getUuid(), VmInstanceVO.class);
        if (self == null || self.getState() == VmInstanceState.Destroyed) {
            // the vm has been destroyed, most likely by rollback
            bus.reply(msg, r);
            return;
        }

        final VmInstanceDeletionPolicy deletionPolicy = msg.getDeletionPolicy() == null ?
                deletionPolicyMgr.getDeletionPolicy(self.getUuid()) : VmInstanceDeletionPolicy.valueOf(msg.getDeletionPolicy());

        destroyHook(deletionPolicy, new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, r);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    protected void destroyHook(VmInstanceDeletionPolicy deletionPolicy, Completion completion) {
        doDestroy(deletionPolicy, completion);
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
            bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID,
                    msg.getRootVolumeInventory().getPrimaryStorageUuid());
            bus.send(cmsg, new CloudBusCallBack(chain) {
                private void fail(ErrorCode errorCode) {
                    String err = String.format("failed to create template from root volume[uuid:%s] on primary storage[uuid:%s]",
                            msg.getRootVolumeInventory().getUuid(), msg.getRootVolumeInventory().getPrimaryStorageUuid());
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
                if (self.getDefaultL3NetworkUuid() == null) {
                    self.setDefaultL3NetworkUuid(msg.getNics().get(0).getL3NetworkUuid());
                    self = dbf.updateAndRefresh(self);
                    logger.debug(String.format("set the VM[uuid: %s]'s default L3 network[uuid:%s], as it doen't have one before",
                            self.getUuid(), self.getDefaultL3NetworkUuid()));
                }

                AttachNicToVmReply r = new AttachNicToVmReply();
                if (!reply.isSuccess()) {
                    r.setError(errf.instantiateErrorCode(VmErrors.ATTACH_NETWORK_ERROR, r.getError()));
                }
                bus.reply(msg, r);
            }
        });
    }

    private void handle(final DestroyVmInstanceMsg msg) {
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
            public void run(final SyncTaskChain taskChain) {
                final DestroyVmInstanceReply reply = new DestroyVmInstanceReply();
                final String issuer = VmInstanceVO.class.getSimpleName();

                VmDeletionStruct s = new VmDeletionStruct();
                s.setDeletionPolicy(deletionPolicyMgr.getDeletionPolicy(self.getUuid()));
                s.setInventory(getSelfInventory());
                final List<VmDeletionStruct> ctx = list(s);

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
            @Deferred
            public void run(SyncTaskChain chain) {
                refreshVO();

                Defer.defer(new Runnable() {
                    @Override
                    public void run() {
                        ChangeVmStateReply reply = new ChangeVmStateReply();
                        bus.reply(msg, reply);
                    }
                });

                if (self == null) {
                    // vm has been deleted by previous request
                    // this happens when delete vm request queued before
                    // change state request from vm tracer.
                    // in this case, ignore change state request
                    logger.debug(String.format("vm[uuid:%s] has been deleted, ignore change vm state request from vm tracer",
                            msg.getVmInstanceUuid()));
                    chain.next();
                    return;
                }

                changeVmStateInDb(VmInstanceStateEvent.valueOf(msg.getStateEvent()));
                chain.next();
            }
        });
    }

    protected void setFlowMarshaller(FlowChain chain) {
        chain.setFlowMarshaller(new FlowMarshaller() {
            @Override
            public Flow marshalTheNextFlow(String previousFlowClassName, String nextFlowClassName, FlowChain chain, Map data) {
                Flow nflow = null;
                for (MarshalVmOperationFlowExtensionPoint mext : pluginRgty.getExtensionList(MarshalVmOperationFlowExtensionPoint.class)) {
                    VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                    nflow = mext.marshalVmOperationFlow(previousFlowClassName, nextFlowClassName, chain, spec);
                    if (nflow != null) {
                        logger.debug(String.format("a VM[uuid: %s, operation: %s] operation flow[%s] is changed to the flow[%s] by %s",
                                self.getUuid(), spec.getCurrentVmOperation(), nextFlowClassName, nflow.getClass(), mext.getClass()));
                        break;
                    }
                }

                return nflow;
            }
        });
    }

    protected void selectBootOrder(VmInstanceSpec spec) {
        if (spec.getCurrentVmOperation() == null) {
            throw new CloudRuntimeException("selectBootOrder must be called after VmOperation is set");
        }

        if (spec.getCurrentVmOperation() == VmOperation.NewCreate && spec.getDestIso() != null) {
            spec.setBootOrders(list(VmBootDevice.CdRom.toString()));
        } else {
            String order = VmSystemTags.BOOT_ORDER.getTokenByResourceUuid(self.getUuid(), VmSystemTags.BOOT_ORDER_TOKEN);
            if (order == null) {
                spec.setBootOrders(list(VmBootDevice.HardDisk.toString()));
            } else {
                spec.setBootOrders(list(order.split(",")));
            }
        }
    }

    protected void startVmFromNewCreate(final StartNewCreatedVmInstanceMsg msg, final SyncTaskChain taskChain) {
        refreshVO();
        ErrorCode error = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
        if (error != null) {
            throw new OperationFailureException(error);
        }

        error = extEmitter.preStartNewCreatedVm(msg.getVmInstanceInventory());
        if (error != null) {
            throw new OperationFailureException(error);
        }

        StartNewCreatedVmInstanceReply reply = new StartNewCreatedVmInstanceReply();
        startVmFromNewCreate(StartVmFromNewCreatedStruct.fromMessage(msg), new Completion(msg, taskChain) {
            @Override
            public void success() {
                self = dbf.reload(self);
                reply.setVmInventory(getSelfInventory());
                bus.reply(msg, reply);
                taskChain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                taskChain.next();
            }
        });

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
        } else if (msg instanceof APICreateStopVmInstanceSchedulerMsg) {
            handle((APICreateStopVmInstanceSchedulerMsg) msg);
        } else if (msg instanceof APIRebootVmInstanceMsg) {
            handle((APIRebootVmInstanceMsg) msg);
        } else if (msg instanceof APICreateRebootVmInstanceSchedulerMsg) {
            handle((APICreateRebootVmInstanceSchedulerMsg) msg);
        } else if (msg instanceof APIDestroyVmInstanceMsg) {
            handle((APIDestroyVmInstanceMsg) msg);
        } else if (msg instanceof APIStartVmInstanceMsg) {
            handle((APIStartVmInstanceMsg) msg);
        } else if (msg instanceof APICreateStartVmInstanceSchedulerMsg) {
            handle((APICreateStartVmInstanceSchedulerMsg) msg);
        } else if (msg instanceof APIMigrateVmMsg) {
            handle((APIMigrateVmMsg) msg);
        } else if (msg instanceof APIAttachL3NetworkToVmMsg) {
            handle((APIAttachL3NetworkToVmMsg) msg);
        } else if (msg instanceof APIGetVmMigrationCandidateHostsMsg) {
            handle((APIGetVmMigrationCandidateHostsMsg) msg);
        } else if (msg instanceof APIGetVmAttachableDataVolumeMsg) {
            handle((APIGetVmAttachableDataVolumeMsg) msg);
        } else if (msg instanceof APIUpdateVmInstanceMsg) {
            handle((APIUpdateVmInstanceMsg) msg);
        } else if (msg instanceof APIChangeInstanceOfferingMsg) {
            handle((APIChangeInstanceOfferingMsg) msg);
        } else if (msg instanceof APIDetachL3NetworkFromVmMsg) {
            handle((APIDetachL3NetworkFromVmMsg) msg);
        } else if (msg instanceof APIGetVmAttachableL3NetworkMsg) {
            handle((APIGetVmAttachableL3NetworkMsg) msg);
        } else if (msg instanceof APIAttachIsoToVmInstanceMsg) {
            handle((APIAttachIsoToVmInstanceMsg) msg);
        } else if (msg instanceof APIDetachIsoFromVmInstanceMsg) {
            handle((APIDetachIsoFromVmInstanceMsg) msg);
        } else if (msg instanceof APIExpungeVmInstanceMsg) {
            handle((APIExpungeVmInstanceMsg) msg);
        } else if (msg instanceof APIRecoverVmInstanceMsg) {
            handle((APIRecoverVmInstanceMsg) msg);
        } else if (msg instanceof APISetVmBootOrderMsg) {
            handle((APISetVmBootOrderMsg) msg);
        } else if (msg instanceof APISetVmConsolePasswordMsg) {
            handle((APISetVmConsolePasswordMsg) msg);
        } else if (msg instanceof APIGetVmBootOrderMsg) {
            handle((APIGetVmBootOrderMsg) msg);
        } else if (msg instanceof APIDeleteVmConsolePasswordMsg) {
            handle((APIDeleteVmConsolePasswordMsg) msg);
        } else if (msg instanceof APIGetVmConsolePasswordMsg) {
            handle((APIGetVmConsolePasswordMsg) msg);
        } else if (msg instanceof APIGetVmConsoleAddressMsg) {
            handle((APIGetVmConsoleAddressMsg) msg);
        } else if (msg instanceof APISetVmHostnameMsg) {
            handle((APISetVmHostnameMsg) msg);
        } else if (msg instanceof APIDeleteVmHostnameMsg) {
            handle((APIDeleteVmHostnameMsg) msg);
        } else if (msg instanceof APISetVmStaticIpMsg) {
            handle((APISetVmStaticIpMsg) msg);
        } else if (msg instanceof APIDeleteVmStaticIpMsg) {
            handle((APIDeleteVmStaticIpMsg) msg);
        } else if (msg instanceof APIGetVmHostnameMsg) {
            handle((APIGetVmHostnameMsg) msg);
        } else if (msg instanceof APIGetVmStartingCandidateClustersHostsMsg) {
            handle((APIGetVmStartingCandidateClustersHostsMsg) msg);
        } else if (msg instanceof APIGetVmCapabilitiesMsg) {
            handle((APIGetVmCapabilitiesMsg) msg);
        } else if (msg instanceof APISetVmSshKeyMsg) {
            handle((APISetVmSshKeyMsg) msg);
        } else if (msg instanceof APIGetVmSshKeyMsg) {
            handle((APIGetVmSshKeyMsg) msg);
        } else if (msg instanceof APIDeleteVmSshKeyMsg) {
            handle((APIDeleteVmSshKeyMsg) msg);
        } else if (msg instanceof APIGetCandidateIsoForAttachingVmMsg) {
            handle((APIGetCandidateIsoForAttachingVmMsg) msg);
        } else if (msg instanceof APISuspendVmInstanceMsg) {
            handle((APISuspendVmInstanceMsg) msg);
        } else if (msg instanceof APIResumeVmInstanceMsg) {
            handle((APIResumeVmInstanceMsg) msg);
        } else {
            VmInstanceBaseExtensionFactory ext = vmMgr.getVmInstanceBaseExtensionFactory(msg);
            if (ext != null) {
                VmInstance v = ext.getVmInstance(self);
                v.handleMessage(msg);
            } else {
                bus.dealWithUnknownMessage(msg);
            }
        }
    }

    @Transactional(readOnly = true)
    private void handle(APIGetCandidateIsoForAttachingVmMsg msg) {
        APIGetCandidateIsoForAttachingVmReply reply = new APIGetCandidateIsoForAttachingVmReply();
        if (self.getState() != VmInstanceState.Running && self.getState() != VmInstanceState.Stopped) {
            reply.setInventories(new ArrayList<>());
            bus.reply(msg, reply);
            return;
        }

        String psUuid = getSelfInventory().getRootVolume().getPrimaryStorageUuid();
        PrimaryStorageVO ps = dbf.getEntityManager().find(PrimaryStorageVO.class, psUuid);
        PrimaryStorageType psType = PrimaryStorageType.valueOf(ps.getType());
        List<String> bsUuids = psType.findBackupStorage(psUuid);

        if (bsUuids == null) {
            String sql = "select img" +
                    " from ImageVO img, ImageBackupStorageRefVO ref, BackupStorageVO bs" +
                    " where ref.imageUuid = img.uuid" +
                    " and img.mediaType = :imgType" +
                    " and img.status = :status" +
                    " and bs.uuid = ref.backupStorageUuid" +
                    " and bs.type in (:bsTypes)";
            TypedQuery<ImageVO> q = dbf.getEntityManager().createQuery(sql, ImageVO.class);
            q.setParameter("imgType", ImageMediaType.ISO);
            q.setParameter("status", ImageStatus.Ready);
            q.setParameter("bsTypes", hostAllocatorMgr.getBackupStorageTypesByPrimaryStorageTypeFromMetrics(ps.getType()));
            reply.setInventories(ImageInventory.valueOf(q.getResultList()));
        } else if (!bsUuids.isEmpty()) {
            String sql = "select img" +
                    " from ImageVO img, ImageBackupStorageRefVO ref, BackupStorageVO bs" +
                    " where ref.imageUuid = img.uuid" +
                    " and img.mediaType = :imgType" +
                    " and img.status = :status" +
                    " and bs.uuid = ref.backupStorageUuid" +
                    " and bs.uuid in (:bsUuids)";
            TypedQuery<ImageVO> q = dbf.getEntityManager().createQuery(sql, ImageVO.class);
            q.setParameter("imgType", ImageMediaType.ISO);
            q.setParameter("status", ImageStatus.Ready);
            q.setParameter("bsUuids", bsUuids);
            reply.setInventories(ImageInventory.valueOf(q.getResultList()));
        } else {
            reply.setInventories(new ArrayList<>());
        }

        bus.reply(msg, reply);
    }

    private void handle(APIGetVmCapabilitiesMsg msg) {
        APIGetVmCapabilitiesReply reply = new APIGetVmCapabilitiesReply();
        Map<String, Object> ret = new HashMap<>();
        checkPrimaryStorageCapabilities(ret);
        reply.setCapabilities(ret);
        bus.reply(msg, reply);
    }

    private void checkPrimaryStorageCapabilities(Map<String, Object> ret) {
        VolumeInventory rootVolume = getSelfInventory().getRootVolume();

        if (rootVolume == null) {
            ret.put(Capability.LiveMigration.toString(), false);
            ret.put(Capability.VolumeMigration.toString(), false);
        } else {
            SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
            q.select(PrimaryStorageVO_.type);
            q.add(PrimaryStorageVO_.uuid, Op.EQ, rootVolume.getPrimaryStorageUuid());
            String type = q.findValue();

            PrimaryStorageType psType = PrimaryStorageType.valueOf(type);
            ret.put(Capability.LiveMigration.toString(), psType.isSupportVmLiveMigration());
            ret.put(Capability.VolumeMigration.toString(), psType.isSupportVolumeMigration());
        }
    }

    private void handle(APIGetVmHostnameMsg msg) {
        String hostname = VmSystemTags.HOSTNAME.getTokenByResourceUuid(self.getUuid(), VmSystemTags.HOSTNAME_TOKEN);
        APIGetVmHostnameReply reply = new APIGetVmHostnameReply();
        reply.setHostname(hostname);
        bus.reply(msg, reply);
    }

    private void handle(final APIDeleteVmStaticIpMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIDeleteVmStaticIpEvent evt = new APIDeleteVmStaticIpEvent(msg.getId());
                new StaticIpOperator().deleteStaticIpByVmUuidAndL3Uuid(self.getUuid(), msg.getL3NetworkUuid());
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "delete-static-ip";
            }
        });
    }

    private void handle(final APISetVmStaticIpMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                setStaticIp(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "set-static-ip";
            }
        });
    }

    private void setStaticIp(final APISetVmStaticIpMsg msg, final NoErrorCompletion completion) {
        refreshVO();
        ErrorCode error = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
        if (error != null) {
            throw new OperationFailureException(error);
        }

        final APISetVmStaticIpEvent evt = new APISetVmStaticIpEvent(msg.getId());
        changeVmIp(msg.getL3NetworkUuid(), msg.getIp(), new Completion(msg, completion) {
            @Override
            public void success() {
                new StaticIpOperator().setStaticIp(self.getUuid(), msg.getL3NetworkUuid(), msg.getIp());
                bus.publish(evt);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setErrorCode(errorCode);
                bus.publish(evt);
                completion.done();
            }
        });
    }

    private void handle(APIDeleteVmHostnameMsg msg) {
        APIDeleteVmHostnameEvent evt = new APIDeleteVmHostnameEvent(msg.getId());
        VmSystemTags.HOSTNAME.delete(self.getUuid());
        bus.publish(evt);
    }

    private void handle(APISetVmHostnameMsg msg) {
        if (!VmSystemTags.HOSTNAME.hasTag(self.getUuid())) {
            VmSystemTags.HOSTNAME.createTag(self.getUuid(), map(
                    e(VmSystemTags.HOSTNAME_TOKEN, msg.getHostname())
            ));
        } else {
            VmSystemTags.HOSTNAME.update(self.getUuid(), VmSystemTags.HOSTNAME.instantiateTag(
                    map(e(VmSystemTags.HOSTNAME_TOKEN, msg.getHostname()))
            ));
        }

        APISetVmHostnameEvent evt = new APISetVmHostnameEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(final APIGetVmConsoleAddressMsg msg) {
        ErrorCode error = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
        if (error != null) {
            throw new OperationFailureException(error);
        }

        final APIGetVmConsoleAddressReply creply = new APIGetVmConsoleAddressReply();
        GetVmConsoleAddressFromHostMsg hmsg = new GetVmConsoleAddressFromHostMsg();
        hmsg.setHostUuid(self.getHostUuid());
        hmsg.setVmInstanceUuid(self.getUuid());
        bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, self.getHostUuid());
        bus.send(hmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    creply.setError(reply.getError());
                } else {
                    GetVmConsoleAddressFromHostReply hr = reply.castReply();
                    creply.setHostIp(hr.getHostIp());
                    creply.setPort(hr.getPort());
                    creply.setProtocol(hr.getProtocol());
                }

                bus.reply(msg, creply);
            }
        });
    }

    private void handle(APIGetVmBootOrderMsg msg) {
        APIGetVmBootOrderReply reply = new APIGetVmBootOrderReply();
        String order = VmSystemTags.BOOT_ORDER.getTokenByResourceUuid(self.getUuid(), VmSystemTags.BOOT_ORDER_TOKEN);
        if (order == null) {
            reply.setOrder(list(VmBootDevice.HardDisk.toString()));
        } else {
            reply.setOrder(list(order.split(",")));
        }
        bus.reply(msg, reply);
    }

    private void handle(APISetVmBootOrderMsg msg) {
        APISetVmBootOrderEvent evt = new APISetVmBootOrderEvent(msg.getId());
        if (msg.getBootOrder() != null) {
            VmSystemTags.BOOT_ORDER.recreateInherentTag(self.getUuid(),
                    map(e(VmSystemTags.BOOT_ORDER_TOKEN, StringUtils.join(msg.getBootOrder(), ","))));
        } else {
            VmSystemTags.BOOT_ORDER.deleteInherentTag(self.getUuid());
        }
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(APISetVmConsolePasswordMsg msg) {
        APISetVmConsolePasswordEvent evt = new APISetVmConsolePasswordEvent(msg.getId());
        VmSystemTags.CONSOLE_PASSWORD.recreateTag(self.getUuid(),
                map(e(VmSystemTags.CONSOLE_PASSWORD_TOKEN, msg.getConsolePassword())));
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(APIGetVmConsolePasswordMsg msg) {
        APIGetVmConsolePasswordReply reply = new APIGetVmConsolePasswordReply();
        String consolePassword = VmSystemTags.CONSOLE_PASSWORD.getTokenByResourceUuid(self.getUuid(),
                VmSystemTags.CONSOLE_PASSWORD_TOKEN);
        reply.setConsolePassword(consolePassword);
        bus.reply(msg, reply);
    }

    private void handle(APIDeleteVmConsolePasswordMsg msg) {
        APIDeleteVmConsolePasswordEvent evt = new APIDeleteVmConsolePasswordEvent(msg.getId());
        VmSystemTags.CONSOLE_PASSWORD.delete(self.getUuid());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(APISetVmSshKeyMsg msg) {
        APISetVmSshKeyEvent evt = new APISetVmSshKeyEvent(msg.getId());
        VmSystemTags.SSHKEY.recreateTag(self.getUuid(), map(e(VmSystemTags.SSHKEY_TOKEN, msg.getSshKey())));
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(APIGetVmSshKeyMsg msg) {
        APIGetVmSshKeyReply reply = new APIGetVmSshKeyReply();
        String sshKey = VmSystemTags.SSHKEY.getTokenByResourceUuid(self.getUuid(), VmSystemTags.SSHKEY_TOKEN);
        reply.setSshKey(sshKey);
        bus.reply(msg, reply);
    }

    private void handle(APIDeleteVmSshKeyMsg msg) {
        APIDeleteVmSshKeyEvent evt = new APIDeleteVmSshKeyEvent(msg.getId());
        VmSystemTags.SSHKEY.delete(self.getUuid());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void recoverVm(final Completion completion) {
        final VmInstanceInventory vm = getSelfInventory();
        final List<RecoverVmExtensionPoint> exts = pluginRgty.getExtensionList(RecoverVmExtensionPoint.class);
        for (RecoverVmExtensionPoint ext : exts) {
            ext.preRecoverVm(vm);
        }

        CollectionUtils.forEach(exts, new ForEachFunction<RecoverVmExtensionPoint>() {
            @Override
            public void run(RecoverVmExtensionPoint ext) {
                ext.beforeRecoverVm(vm);
            }
        });

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("recover-vm-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "recover-root-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        RecoverVolumeMsg msg = new RecoverVolumeMsg();
                        msg.setVolumeUuid(self.getRootVolumeUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, self.getRootVolumeUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "recover-vm";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        self = changeVmStateInDb(VmInstanceStateEvent.stopped);
                        CollectionUtils.forEach(exts, new ForEachFunction<RecoverVmExtensionPoint>() {
                            @Override
                            public void run(RecoverVmExtensionPoint ext) {
                                ext.afterRecoverVm(vm);
                            }
                        });

                        trigger.next();
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

    private void handle(final APIRecoverVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final APIRecoverVmInstanceEvent evt = new APIRecoverVmInstanceEvent(msg.getId());
                refreshVO();

                ErrorCode error = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (error != null) {
                    evt.setErrorCode(error);
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                recoverVm(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        evt.setInventory(getSelfInventory());
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

            @Override
            public String getName() {
                return "recover-vm";
            }
        });
    }

    private void handle(final APIExpungeVmInstanceMsg msg) {
        final APIExpungeVmInstanceEvent evt = new APIExpungeVmInstanceEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                expunge(msg, new Completion(msg, chain) {
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

            @Override
            public String getName() {
                return "expunge-vm-by-api";
            }
        });
    }

    private void handle(final APIDetachIsoFromVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final APIDetachIsoFromVmInstanceEvent evt = new APIDetachIsoFromVmInstanceEvent(msg.getId());
                refreshVO();
                ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    evt.setErrorCode(allowed);
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                detachIso(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        self = dbf.reload(self);
                        evt.setInventory(getSelfInventory());
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

            @Override
            public String getName() {
                return String.format("detach-iso-from-vm-%s", self.getUuid());
            }
        });
    }

    private void detachIso(final Completion completion) {
        if (self.getState() == VmInstanceState.Stopped) {
            new IsoOperator().detachIsoFromVm(self.getUuid());
            completion.success();
            return;
        }

        if (!new IsoOperator().isIsoAttachedToVm(self.getUuid())) {
            completion.success();
            return;
        }

        VmInstanceSpec spec = buildSpecFromInventory(getSelfInventory(), VmOperation.DetachIso);
        if (spec.getDestIso() == null) {
            // the image ISO has been deleted from backup storage
            // try to detach it from the VM anyway
            String isoUuid = new IsoOperator().getIsoUuidByVmUuid(self.getUuid());
            IsoSpec isoSpec = new IsoSpec();
            isoSpec.setImageUuid(isoUuid);
            spec.setDestIso(isoSpec);
            logger.debug(String.format("the iso[uuid:%s] has been deleted, try to detach it from the VM[uuid:%s] anyway",
                    isoUuid, self.getUuid()));
        }

        FlowChain chain = getDetachIsoWorkFlowChain(spec.getVmInventory());
        chain.setName(String.format("detach-iso-%s-from-vm-%s", spec.getDestIso().getImageUuid(), self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);

        setFlowMarshaller(chain);

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                new IsoOperator().detachIsoFromVm(self.getUuid());
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Transactional(readOnly = true)
    private List<L3NetworkInventory> getAttachableL3Network(String accountUuid) {
        List<String> l3Uuids = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, L3NetworkVO.class);
        if (l3Uuids != null && l3Uuids.isEmpty()) {
            return new ArrayList<L3NetworkInventory>();
        }

        String sql;
        TypedQuery<L3NetworkVO> q;
        if (self.getVmNics().isEmpty()) {
            if (l3Uuids == null) {
                // accessed by a system admin
                sql = "select l3" +
                        " from L3NetworkVO l3, VmInstanceVO vm, L2NetworkVO l2, L2NetworkClusterRefVO l2ref" +
                        " where vm.uuid = :uuid" +
                        " and vm.clusterUuid = l2ref.clusterUuid" +
                        " and l2ref.l2NetworkUuid = l2.uuid" +
                        " and l2.uuid = l3.l2NetworkUuid" +
                        " and l3.state = :l3State" +
                        " group by l3.uuid";
                q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
            } else {
                // accessed by a normal account
                sql = "select l3" +
                        " from L3NetworkVO l3, VmInstanceVO vm, L2NetworkVO l2, L2NetworkClusterRefVO l2ref" +
                        " where vm.uuid = :uuid" +
                        " and vm.clusterUuid = l2ref.clusterUuid" +
                        " and l2ref.l2NetworkUuid = l2.uuid" +
                        " and l2.uuid = l3.l2NetworkUuid" +
                        " and l3.state = :l3State" +
                        " and l3.uuid in (:l3uuids)" +
                        " group by l3.uuid";
                q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
                q.setParameter("l3uuids", l3Uuids);
            }
        } else {
            if (l3Uuids == null) {
                // accessed by a system admin
                sql = "select l3" +
                        " from L3NetworkVO l3, VmInstanceVO vm, L2NetworkVO l2, L2NetworkClusterRefVO l2ref" +
                        " where l3.uuid not in" +
                        " (select nic.l3NetworkUuid from VmNicVO nic where nic.vmInstanceUuid = :uuid)" +
                        " and vm.uuid = :uuid" +
                        " and vm.clusterUuid = l2ref.clusterUuid" +
                        " and l2ref.l2NetworkUuid = l2.uuid" +
                        " and l2.uuid = l3.l2NetworkUuid" +
                        " and l3.state = :l3State" +
                        " group by l3.uuid";
                q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
            } else {
                // accessed by a normal account
                sql = "select l3" +
                        " from L3NetworkVO l3, VmInstanceVO vm, L2NetworkVO l2, L2NetworkClusterRefVO l2ref" +
                        " where l3.uuid not in" +
                        " (select nic.l3NetworkUuid from VmNicVO nic where nic.vmInstanceUuid = :uuid)" +
                        " and vm.uuid = :uuid" +
                        " and vm.clusterUuid = l2ref.clusterUuid" +
                        " and l2ref.l2NetworkUuid = l2.uuid" +
                        " and l2.uuid = l3.l2NetworkUuid" +
                        " and l3.state = :l3State" +
                        " and l3.uuid in (:l3uuids)" +
                        " group by l3.uuid";
                q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
                q.setParameter("l3uuids", l3Uuids);
            }
        }

        q.setParameter("l3State", L3NetworkState.Enabled);
        q.setParameter("uuid", self.getUuid());
        List<L3NetworkVO> l3s = q.getResultList();
        return L3NetworkInventory.valueOf(l3s);
    }

    private void handle(APIGetVmAttachableL3NetworkMsg msg) {
        APIGetVmAttachableL3NetworkReply reply = new APIGetVmAttachableL3NetworkReply();
        reply.setInventories(getAttachableL3Network(msg.getSession().getAccountUuid()));
        bus.reply(msg, reply);
    }

    private void handle(final APIAttachIsoToVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final APIAttachIsoToVmInstanceEvent evt = new APIAttachIsoToVmInstanceEvent(msg.getId());

                refreshVO();
                ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    evt.setErrorCode(allowed);
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                attachIso(msg.getIsoUuid(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        self = dbf.reload(self);
                        evt.setInventory(getSelfInventory());
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

            @Override
            public String getName() {
                return String.format("attach-iso-%s-to-vm-%s", msg.getIsoUuid(), self.getUuid());
            }
        });
    }

    private void attachIso(final String isoUuid, final Completion completion) {
        checkIfIsoAttachable(isoUuid);

        if (self.getState() == VmInstanceState.Stopped) {
            new IsoOperator().attachIsoToVm(self.getUuid(), isoUuid);
            completion.success();
            return;
        }

        VmInstanceSpec spec = buildSpecFromInventory(getSelfInventory(), VmOperation.AttachIso);
        IsoSpec isoSpec = new IsoSpec();
        isoSpec.setImageUuid(isoUuid);
        spec.setDestIso(isoSpec);

        FlowChain chain = getAttachIsoWorkFlowChain(spec.getVmInventory());
        chain.setName(String.format("attach-iso-%s-to-vm-%s", isoUuid, self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);

        setFlowMarshaller(chain);

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                new IsoOperator().attachIsoToVm(self.getUuid(), isoUuid);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Transactional(readOnly = true)
    private void checkIfIsoAttachable(String isoUuid) {
        String psUuid = getSelfInventory().getRootVolume().getPrimaryStorageUuid();
        String sql = "select count(i)" +
                " from ImageCacheVO i" +
                " where i.primaryStorageUuid = :psUuid" +
                " and i.imageUuid = :isoUuid";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("psUuid", psUuid);
        q.setParameter("isoUuid", isoUuid);
        Long count = q.getSingleResult();
        if (count > 0) {
            // on the same primary storage
            return;
        }

        PrimaryStorageVO psvo = dbf.getEntityManager().find(PrimaryStorageVO.class, psUuid);
        PrimaryStorageType type = PrimaryStorageType.valueOf(psvo.getType());
        List<String> bsUuids = type.findBackupStorage(psUuid);
        if (bsUuids == null) {
            List<String> possibleBsTypes = hostAllocatorMgr.getBackupStorageTypesByPrimaryStorageTypeFromMetrics(psvo.getType());
            sql = "select count(bs)" +
                    " from BackupStorageVO bs, ImageBackupStorageRefVO ref" +
                    " where bs.uuid = ref.backupStorageUuid" +
                    " and ref.imageUuid = :imgUuid" +
                    " and bs.type in (:bsTypes)";
            q = dbf.getEntityManager().createQuery(sql, Long.class);
            q.setParameter("imgUuid", isoUuid);
            q.setParameter("bsTypes", possibleBsTypes);
            count = q.getSingleResult();
            if (count > 0) {
                return;
            }
        } else if (!bsUuids.isEmpty()) {
            sql = "select count(bs)" +
                    " from BackupStorageVO bs, ImageBackupStorageRefVO ref" +
                    " where bs.uuid = ref.backupStorageUuid" +
                    " and ref.imageUuid = :imgUuid" +
                    " and bs.uuid in (:bsUuids)";
            q = dbf.getEntityManager().createQuery(sql, Long.class);
            q.setParameter("imgUuid", isoUuid);
            q.setParameter("bsUuids", bsUuids);
            count = q.getSingleResult();
            if (count > 0) {
                return;
            }
        }

        throw new OperationFailureException(errf.stringToOperationError(
                String.format("the ISO[uuid:%s] is on backup storage that is not compatible of the primary storage[uuid:%s]" +
                        " where the VM[name:%s, uuid:%s] is on", isoUuid, psUuid, self.getName(), self.getUuid())
        ));
    }

    private void handle(final APIDetachL3NetworkFromVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final APIDetachL3NetworkFromVmEvent evt = new APIDetachL3NetworkFromVmEvent(msg.getId());

                refreshVO();
                ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    evt.setErrorCode(allowed);
                    bus.publish(evt);
                    chain.next();
                    return;
                }


                detachNic(msg.getVmNicUuid(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        self = dbf.reload(self);
                        evt.setInventory(getSelfInventory());
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

            @Override
            public String getName() {
                return "detach-nic";
            }
        });
    }

    private void detachNic(final String nicUuid, final Completion completion) {
        final VmNicInventory nic = VmNicInventory.valueOf(
                CollectionUtils.find(self.getVmNics(), new Function<VmNicVO, VmNicVO>() {
                    @Override
                    public VmNicVO call(VmNicVO arg) {
                        return arg.getUuid().equals(nicUuid) ? arg : null;
                    }
                })
        );

        for (VmDetachNicExtensionPoint ext : pluginRgty.getExtensionList(VmDetachNicExtensionPoint.class)) {
            ext.preDetachNic(nic);
        }

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmDetachNicExtensionPoint.class),
                new ForEachFunction<VmDetachNicExtensionPoint>() {
                    @Override
                    public void run(VmDetachNicExtensionPoint arg) {
                        arg.beforeDetachNic(nic);
                    }
                });

        final VmInstanceSpec spec = buildSpecFromInventory(getSelfInventory(), VmOperation.DetachNic);
        spec.setVmInventory(VmInstanceInventory.valueOf(self));
        spec.setDestNics(list(nic));
        spec.setL3Networks(list(L3NetworkInventory.valueOf(dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class))));

        FlowChain flowChain = FlowChainBuilder.newSimpleFlowChain();
        flowChain.setName(String.format("detachNic-vm-%s-nic-%s", self.getUuid(), nicUuid));
        setFlowMarshaller(flowChain);
        flowChain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        if (self.getState() == VmInstanceState.Running) {
            flowChain.then(new VmDetachNicOnHypervisorFlow());
        }
        flowChain.then(new VmReleaseResourceOnDetachingNicFlow());
        flowChain.then(new VmDetachNicFlow());
        flowChain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                selectDefaultL3();
                removeStaticIp();
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmDetachNicExtensionPoint.class),
                        new ForEachFunction<VmDetachNicExtensionPoint>() {
                            @Override
                            public void run(VmDetachNicExtensionPoint arg) {
                                arg.afterDetachNic(nic);
                            }
                        });
                completion.success();
            }

            private void removeStaticIp() {
                new StaticIpOperator().deleteStaticIpByVmUuidAndL3Uuid(self.getUuid(), nic.getL3NetworkUuid());
            }

            private void selectDefaultL3() {
                if (!self.getDefaultL3NetworkUuid().equals(nic.getL3NetworkUuid())) {
                    return;
                }

                final VmInstanceInventory vm = getSelfInventory();
                final String previousDefaultL3 = vm.getDefaultL3NetworkUuid();

                // the nic has been removed, reload
                self = dbf.reload(self);

                final VmNicVO candidate = CollectionUtils.find(self.getVmNics(), new Function<VmNicVO, VmNicVO>() {
                    @Override
                    public VmNicVO call(VmNicVO arg) {
                        return arg.getL3NetworkUuid().equals(nic.getUuid()) ? null : arg;
                    }
                });

                if (candidate != null) {
                    CollectionUtils.safeForEach(
                            pluginRgty.getExtensionList(VmDefaultL3NetworkChangedExtensionPoint.class),
                            new ForEachFunction<VmDefaultL3NetworkChangedExtensionPoint>() {
                                @Override
                                public void run(VmDefaultL3NetworkChangedExtensionPoint ext) {
                                    ext.vmDefaultL3NetworkChanged(vm, previousDefaultL3, candidate.getL3NetworkUuid());
                                }
                            });

                    self.setDefaultL3NetworkUuid(candidate.getL3NetworkUuid());
                    logger.debug(String.format(
                            "after detaching the nic[uuid:%s, L3 uuid:%s], change the default L3 of the VM[uuid:%s]" +
                                    " to the L3 network[uuid: %s]", nic.getUuid(), nic.getL3NetworkUuid(), self.getUuid(),
                            candidate.getL3NetworkUuid()));
                } else {
                    self.setDefaultL3NetworkUuid(null);
                    logger.debug(String.format(
                            "after detaching the nic[uuid:%s, L3 uuid:%s], change the default L3 of the VM[uuid:%s]" +
                                    " to null, as the VM has no other nics", nic.getUuid(), nic.getL3NetworkUuid(), self.getUuid()));
                }

                self = dbf.updateAndRefresh(self);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmDetachNicExtensionPoint.class),
                        new ForEachFunction<VmDetachNicExtensionPoint>() {
                            @Override
                            public void run(VmDetachNicExtensionPoint arg) {
                                arg.failedToDetachNic(nic, errCode);
                            }
                        });
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(final APIChangeInstanceOfferingMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                changeOffering(msg);
                chain.next();
            }

            @Override
            public String getName() {
                return "change-instance-offering";
            }
        });
    }

    private void changeOffering(APIChangeInstanceOfferingMsg msg) {
        APIChangeInstanceOfferingEvent evt = new APIChangeInstanceOfferingEvent(msg.getId());

        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
        if (allowed != null) {
            bus.replyErrorByMessageType(msg, allowed);
            return;
        }

        final InstanceOfferingVO newOfferingVO = dbf.findByUuid(msg.getInstanceOfferingUuid(), InstanceOfferingVO.class);
        final InstanceOfferingInventory inv = InstanceOfferingInventory.valueOf(newOfferingVO);
        final VmInstanceInventory vm = getSelfInventory();

        List<ChangeInstanceOfferingExtensionPoint> exts = pluginRgty.getExtensionList(ChangeInstanceOfferingExtensionPoint.class);
        for (ChangeInstanceOfferingExtensionPoint ext : exts) {
            ext.preChangeInstanceOffering(vm, inv);
        }

        CollectionUtils.safeForEach(exts, new ForEachFunction<ChangeInstanceOfferingExtensionPoint>() {
            @Override
            public void run(ChangeInstanceOfferingExtensionPoint arg) {
                arg.beforeChangeInstanceOffering(vm, inv);
            }
        });


        if (self.getState() == VmInstanceState.Stopped) {
            changeInstanceOfferingForStoppedVm(exts, newOfferingVO, vm, inv, evt);
            return;
        } else {
            if (self.getState() != VmInstanceState.Running) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("The state of vm[uuid:%s] is %s. Only these state[%s] is allowed.",
                                self.getUuid(), self.getState(),
                                StringUtils.join(list(VmInstanceState.Running, VmInstanceState.Stopped), ","))));
            }
        }

        String instanceOfferingOnlineChange = VmSystemTags.INSTANCEOFFERING_ONLIECHANGE
                .getTokenByResourceUuid(self.getUuid(), VmSystemTags.INSTANCEOFFERING_ONLINECHANGE_TOKEN);

        if ((instanceOfferingOnlineChange != null
                && instanceOfferingOnlineChange.equals("true"))
                && self.getCpuNum() <= newOfferingVO.getCpuNum()
                && self.getMemorySize() <= newOfferingVO.getMemorySize()) {
            stretchInstanceOfferingForRunningVm(exts, newOfferingVO, vm, inv, evt);
        } else {
            pendingChangeInstanceOfferingForRunningVmNextStart(exts, newOfferingVO, vm, inv, evt);
        }
    }

    private void pendingChangeInstanceOfferingForRunningVmNextStart(List<ChangeInstanceOfferingExtensionPoint> exts,
                                                                    final InstanceOfferingVO newOfferingVO,
                                                                    final VmInstanceInventory vm,
                                                                    final InstanceOfferingInventory inv,
                                                                    APIChangeInstanceOfferingEvent evt) {
        Map m = new HashMap();
        m.put(VmSystemTags.PENDING_CAPACITY_CHNAGE_CPU_NUM_TOKEN, newOfferingVO.getCpuNum());
        m.put(VmSystemTags.PENDING_CAPACITY_CHNAGE_CPU_SPEED_TOKEN, newOfferingVO.getCpuSpeed());
        m.put(VmSystemTags.PENDING_CAPACITY_CHNAGE_MEMORY_TOKEN, newOfferingVO.getMemorySize());
        VmSystemTags.PENDING_CAPACITY_CHANGE.recreateInherentTag(self.getUuid(), m);

        self.setInstanceOfferingUuid(newOfferingVO.getUuid());
        self = dbf.updateAndRefresh(self);

        CollectionUtils.safeForEach(exts, new ForEachFunction<ChangeInstanceOfferingExtensionPoint>() {
            @Override
            public void run(ChangeInstanceOfferingExtensionPoint arg) {
                arg.afterChangeInstanceOffering(vm, inv);
            }
        });

        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void changeInstanceOfferingForStoppedVm(List<ChangeInstanceOfferingExtensionPoint> exts,
                                                    final InstanceOfferingVO newOfferingVO,
                                                    final VmInstanceInventory vm,
                                                    final InstanceOfferingInventory inv,
                                                    APIChangeInstanceOfferingEvent evt) {
        self.setInstanceOfferingUuid(newOfferingVO.getUuid());
        self.setCpuNum(newOfferingVO.getCpuNum());
        self.setCpuSpeed(newOfferingVO.getCpuSpeed());
        self.setMemorySize(newOfferingVO.getMemorySize());
        self = dbf.updateAndRefresh(self);

        CollectionUtils.safeForEach(exts, new ForEachFunction<ChangeInstanceOfferingExtensionPoint>() {
            @Override
            public void run(ChangeInstanceOfferingExtensionPoint arg) {
                arg.afterChangeInstanceOffering(vm, inv);
            }
        });

        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void stretchInstanceOfferingForRunningVm(List<ChangeInstanceOfferingExtensionPoint> exts,
                                                     final InstanceOfferingVO newOfferingVO,
                                                     final VmInstanceInventory vm,
                                                     final InstanceOfferingInventory inv,
                                                     APIChangeInstanceOfferingEvent evt) {
        OnlineChangeVmCpuMemoryMsg hmsg = new OnlineChangeVmCpuMemoryMsg();
        hmsg.setVmInstanceUuid(self.getUuid());
        hmsg.setHostUuid(self.getHostUuid());
        hmsg.setInstanceOfferingInventory(inv);
        bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, self.getHostUuid());
        bus.send(hmsg, new CloudBusCallBack() {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setErrorCode(reply.getError());
                } else {
                    OnlineChangeVmCpuMemoryReply hr = reply.castReply();
                    self.setInstanceOfferingUuid(newOfferingVO.getUuid());
                    self.setCpuNum(hr.getInstanceOfferingInventory().getCpuNum());
                    self.setMemorySize(hr.getInstanceOfferingInventory().getMemorySize());
                    self = dbf.updateAndRefresh(self);

                    CollectionUtils.safeForEach(exts, new ForEachFunction<ChangeInstanceOfferingExtensionPoint>() {
                        @Override
                        public void run(ChangeInstanceOfferingExtensionPoint arg) {
                            arg.afterChangeInstanceOffering(vm, inv);
                        }
                    });
                    evt.setInventory(getSelfInventory());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(final APIUpdateVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                refreshVO();

                List<Runnable> extensions = new ArrayList<Runnable>();
                final VmInstanceInventory vm = getSelfInventory();

                boolean update = false;
                if (msg.getName() != null) {
                    self.setName(msg.getName());
                    update = true;
                }
                if (msg.getDescription() != null) {
                    self.setDescription(msg.getDescription());
                    update = true;
                }
                if (msg.getState() != null) {
                    self.setState(VmInstanceState.valueOf(msg.getState()));
                    update = true;
                    if (!vm.getState().equals(msg.getState())) {
                        extensions.add(new Runnable() {
                            @Override
                            public void run() {
                                logger.debug(String.format("vm[uuid:%s] changed state from %s to %s", self.getUuid(),
                                        vm.getState(), msg.getState()));

                                VmCanonicalEvents.VmStateChangedData data = new VmCanonicalEvents.VmStateChangedData();
                                data.setVmUuid(self.getUuid());
                                data.setOldState(vm.getState());
                                data.setNewState(msg.getState());
                                data.setInventory(getSelfInventory());
                                evtf.fire(VmCanonicalEvents.VM_FULL_STATE_CHANGED_PATH, data);
                            }
                        });
                    }
                }
                if (msg.getDefaultL3NetworkUuid() != null) {
                    self.setDefaultL3NetworkUuid(msg.getDefaultL3NetworkUuid());
                    update = true;
                    if (!msg.getDefaultL3NetworkUuid().equals(vm.getDefaultL3NetworkUuid())) {
                        extensions.add(new Runnable() {
                            @Override
                            public void run() {
                                for (VmDefaultL3NetworkChangedExtensionPoint ext :
                                        pluginRgty.getExtensionList(VmDefaultL3NetworkChangedExtensionPoint.class)) {
                                    ext.vmDefaultL3NetworkChanged(vm, vm.getDefaultL3NetworkUuid(), msg.getDefaultL3NetworkUuid());
                                }
                            }
                        });
                    }
                }
                if (msg.getPlatform() != null) {
                    self.setPlatform(msg.getPlatform());
                    update = true;
                }
                if (update) {
                    self = dbf.updateAndRefresh(self);
                }

                CollectionUtils.safeForEach(extensions, new ForEachFunction<Runnable>() {
                    @Override
                    public void run(Runnable arg) {
                        arg.run();
                    }
                });

                APIUpdateVmInstanceEvent evt = new APIUpdateVmInstanceEvent(msg.getId());
                evt.setInventory(getSelfInventory());
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "update-vm-info";
            }
        });
    }

    @Transactional(readOnly = true)
    private List<VolumeVO> getAttachableVolume(String accountUuid) {
        List<String> volUuids = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, VolumeVO.class);
        if (volUuids != null && volUuids.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> formats = VolumeFormat.getVolumeFormatSupportedByHypervisorTypeInString(self.getHypervisorType());
        if (formats.isEmpty()) {
            throw new CloudRuntimeException(String.format("cannot find volume formats for the hypervisor type[%s]",
                    self.getHypervisorType()));
        }

        String sql;
        List<VolumeVO> vos;
        if (volUuids == null) {
            // accessed by a system admin
            sql = "select vol" +
                    " from VolumeVO vol, VmInstanceVO vm, PrimaryStorageClusterRefVO ref" +
                    " where vol.type = :type" +
                    " and vol.state = :volState" +
                    " and vol.status = :volStatus" +
                    " and vol.format in (:formats)" +
                    " and vol.vmInstanceUuid is null" +
                    " and vm.clusterUuid = ref.clusterUuid" +
                    " and ref.primaryStorageUuid = vol.primaryStorageUuid" +
                    " and vm.uuid = :vmUuid" +
                    " group by vol.uuid";
            TypedQuery<VolumeVO> q = dbf.getEntityManager().createQuery(sql, VolumeVO.class);
            q.setParameter("volState", VolumeState.Enabled);
            q.setParameter("volStatus", VolumeStatus.Ready);
            q.setParameter("formats", formats);
            q.setParameter("vmUuid", self.getUuid());
            q.setParameter("type", VolumeType.Data);
            vos = q.getResultList();

            sql = "select vol" +
                    " from VolumeVO vol" +
                    " where vol.type = :type" +
                    " and vol.status = :volStatus" +
                    " and vol.state = :volState" +
                    " group by vol.uuid";
            q = dbf.getEntityManager().createQuery(sql, VolumeVO.class);
            q.setParameter("type", VolumeType.Data);
            q.setParameter("volState", VolumeState.Enabled);
            q.setParameter("volStatus", VolumeStatus.NotInstantiated);
            vos.addAll(q.getResultList());
        } else {
            // accessed by a normal account
            sql = "select vol" +
                    " from VolumeVO vol, VmInstanceVO vm, PrimaryStorageClusterRefVO ref" +
                    " where vol.type = :type" +
                    " and vol.state = :volState" +
                    " and vol.status = :volStatus" +
                    " and vol.format in (:formats)" +
                    " and vol.vmInstanceUuid is null" +
                    " and vm.clusterUuid = ref.clusterUuid" +
                    " and ref.primaryStorageUuid = vol.primaryStorageUuid" +
                    " and vol.uuid in (:volUuids)" +
                    " and vm.uuid = :vmUuid" +
                    " group by vol.uuid";
            TypedQuery<VolumeVO> q = dbf.getEntityManager().createQuery(sql, VolumeVO.class);
            q.setParameter("volState", VolumeState.Enabled);
            q.setParameter("volStatus", VolumeStatus.Ready);
            q.setParameter("vmUuid", self.getUuid());
            q.setParameter("formats", formats);
            q.setParameter("type", VolumeType.Data);
            q.setParameter("volUuids", volUuids);
            vos = q.getResultList();

            sql = "select vol" +
                    " from VolumeVO vol" +
                    " where vol.type = :type" +
                    " and vol.status = :volStatus" +
                    " and vol.state = :volState" +
                    " and vol.uuid in (:volUuids)" +
                    " group by vol.uuid";
            q = dbf.getEntityManager().createQuery(sql, VolumeVO.class);
            q.setParameter("type", VolumeType.Data);
            q.setParameter("volState", VolumeState.Enabled);
            q.setParameter("volUuids", volUuids);
            q.setParameter("volStatus", VolumeStatus.NotInstantiated);
            vos.addAll(q.getResultList());
        }


        for (GetAttachableVolumeExtensionPoint ext : pluginRgty.getExtensionList(GetAttachableVolumeExtensionPoint.class)) {
            if (!vos.isEmpty()) {
                vos = ext.returnAttachableVolumes(getSelfInventory(), vos);
            }
        }

        return vos;
    }

    private void handle(APIGetVmAttachableDataVolumeMsg msg) {
        APIGetVmAttachableDataVolumeReply reply = new APIGetVmAttachableDataVolumeReply();
        reply.setInventories(VolumeInventory.valueOf(getAttachableVolume(msg.getSession().getAccountUuid())));
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

    private void handle(final APIAttachL3NetworkToVmMsg msg) {
        final APIAttachL3NetworkToVmEvent evt = new APIAttachL3NetworkToVmEvent(msg.getId());
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
        refreshVO(true);

        if (self == null || VmInstanceState.Destroyed == self.getState()) {
            // the vm is destroyed, the data volume must have been detached
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        ErrorCode allowed = validateOperationByState(msg, self.getState(), VmErrors.DETACH_VOLUME_ERROR);
        if (allowed != null) {
            throw new OperationFailureException(allowed);
        }

        final VolumeInventory volume = msg.getVolume();
        extEmitter.preDetachVolume(getSelfInventory(), volume);
        extEmitter.beforeDetachVolume(getSelfInventory(), volume);

        if (self.getState() == VmInstanceState.Stopped) {
            extEmitter.afterDetachVolume(getSelfInventory(), volume);
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
                    extEmitter.failedToDetachVolume(getSelfInventory(), volume, r.getError());
                } else {
                    extEmitter.afterDetachVolume(getSelfInventory(), volume);
                }

                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    protected void attachDataVolume(final AttachDataVolumeToVmMsg msg, final NoErrorCompletion completion) {
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
        spec.setCurrentVmOperation(VmOperation.AttachVolume);
        spec.setDestDataVolumes(list(volume));
        FlowChain chain;
        if (volume.getStatus().equals(VolumeStatus.Ready.toString())) {
            chain = FlowChainBuilder.newSimpleFlowChain();
            chain.then(new VmAssignDeviceIdToAttachingVolumeFlow());
            chain.then(new VmAttachVolumeOnHypervisorFlow());
        } else {
            chain = getAttachUninstantiatedVolumeWorkFlowChain(spec.getVmInventory());
        }

        setFlowMarshaller(chain);

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

        VmInstanceInventory pinv = getSelfInventory();
        for (VmPreMigrationExtensionPoint ext : pluginRgty.getExtensionList(VmPreMigrationExtensionPoint.class)) {
            ext.preVmMigration(pinv);
        }

        VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
        final VmInstanceSpec spec = buildSpecFromInventory(inv, VmOperation.Migrate);

        final VmInstanceState originState = self.getState();
        changeVmStateInDb(VmInstanceStateEvent.migrating);
        spec.setMessage(msg);
        FlowChain chain = getMigrateVmWorkFlowChain(inv);

        setFlowMarshaller(chain);

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
                extEmitter.afterMigrateVm(vm, vm.getLastHostUuid());
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                extEmitter.failedToMigrateVm(VmInstanceInventory.valueOf(self), spec.getDestHost().getUuid(), errCode);
                if (HostErrors.FAILED_TO_MIGRATE_VM_ON_HYPERVISOR.isEqual(errCode.getCode())) {
                    checkState(originalCopy.getHostUuid(), new NoErrorCompletion(completion) {
                        @Override
                        public void done() {
                            completion.fail(errCode);
                        }
                    });
                } else {
                    self.setState(originState);
                    self = dbf.updateAndRefresh(self);
                    completion.fail(errCode);
                }
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

    protected void applyPendingCapacityChangeIfNeed() {
        String pendingCapacityChange = VmSystemTags.PENDING_CAPACITY_CHANGE.getTag(self.getUuid());
        if (pendingCapacityChange != null) {
            // the instance offering had been changed, apply new capacity to myself
            Map<String, String> tokens = VmSystemTags.PENDING_CAPACITY_CHANGE.getTokensByTag(pendingCapacityChange);
            int cpuNum = Integer.valueOf(tokens.get(VmSystemTags.PENDING_CAPACITY_CHNAGE_CPU_NUM_TOKEN));
            int cpuSpeed = Integer.valueOf(tokens.get(VmSystemTags.PENDING_CAPACITY_CHNAGE_CPU_SPEED_TOKEN));
            long memory = Long.valueOf(tokens.get(VmSystemTags.PENDING_CAPACITY_CHNAGE_MEMORY_TOKEN));

            self.setCpuNum(cpuNum);
            self.setCpuSpeed(cpuSpeed);
            self.setMemorySize(memory);
            self = dbf.updateAndRefresh(self);

            VmSystemTags.PENDING_CAPACITY_CHANGE.deleteInherentTag(self.getUuid());
        }
    }

    protected void startVm(final Message msg, final Completion completion) {
        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), null);
        if (allowed != null) {
            completion.fail(allowed);
            return;
        }

        if (self.getState() == VmInstanceState.Created) {
            StartVmFromNewCreatedStruct struct = new JsonLabel().get(
                    StartVmFromNewCreatedStruct.makeLabelKey(self.getUuid()), StartVmFromNewCreatedStruct.class);

            startVmFromNewCreate(struct, completion);
            return;
        }

        applyPendingCapacityChangeIfNeed();

        VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
        ErrorCode preStart = extEmitter.preStartVm(inv);
        if (preStart != null) {
            completion.fail(preStart);
            return;
        }

        final VmInstanceSpec spec = buildSpecFromInventory(inv, VmOperation.Start);
        spec.setMessage(msg);

        if (msg instanceof APIStartVmInstanceMsg) {
            APIStartVmInstanceMsg amsg = (APIStartVmInstanceMsg) msg;
            spec.setRequiredClusterUuid(amsg.getClusterUuid());
            spec.setRequiredHostUuid(amsg.getHostUuid());
        }

        if (spec.getDestNics().isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("unable to start the vm[uuid:%s]." +
                                    " It doesn't have any nic, please attach a nic and try again",
                            self.getUuid())
            ));
        }

        final VmInstanceState originState = self.getState();
        changeVmStateInDb(VmInstanceStateEvent.starting);

        extEmitter.beforeStartVm(VmInstanceInventory.valueOf(self));

        FlowChain chain = getStartVmWorkFlowChain(inv);
        setFlowMarshaller(chain);

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
                extEmitter.failedToStartVm(VmInstanceInventory.valueOf(self), errCode);
                VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                if (HostErrors.FAILED_TO_START_VM_ON_HYPERVISOR.isEqual(errCode.getCode())) {
                    checkState(spec.getDestHost().getUuid(), new NoErrorCompletion(completion) {
                        @Override
                        public void done() {
                            completion.fail(errCode);
                        }
                    });
                } else {
                    self.setState(originState);
                    self = dbf.updateAndRefresh(self);
                    completion.fail(errCode);
                }
            }
        }).start();
    }

    private void startVmFromNewCreate(StartVmFromNewCreatedStruct struct, Completion completion) {
        VmInstanceInventory inv = getSelfInventory();

        final VmInstanceSpec spec = new VmInstanceSpec();
        spec.setRequiredPrimaryStorageUuidForRootVolume(struct.getPrimaryStorageUuidForRootVolume());
        spec.setVmInventory(inv);
        if (struct.getL3NetworkUuids() != null && !struct.getL3NetworkUuids().isEmpty()) {
            SimpleQuery<L3NetworkVO> nwquery = dbf.createQuery(L3NetworkVO.class);
            nwquery.add(L3NetworkVO_.uuid, Op.IN, struct.getL3NetworkUuids());
            List<L3NetworkVO> vos = nwquery.list();
            List<L3NetworkInventory> nws = L3NetworkInventory.valueOf(vos);

            // order L3 networks by the order they specified in the API
            List<L3NetworkInventory> l3s = new ArrayList<>(nws.size());
            for (final String l3Uuid : struct.getL3NetworkUuids()) {
                L3NetworkInventory l3 = CollectionUtils.find(nws, new Function<L3NetworkInventory, L3NetworkInventory>() {
                    @Override
                    public L3NetworkInventory call(L3NetworkInventory arg) {
                        return arg.getUuid().equals(l3Uuid) ? arg : null;
                    }
                });
                DebugUtils.Assert(l3 != null, "where is the L3???");
                l3s.add(l3);
            }

            spec.setL3Networks(l3s);
        } else {
            spec.setL3Networks(new ArrayList<>());
        }

        if (struct.getDataDiskOfferingUuids() != null && !struct.getDataDiskOfferingUuids().isEmpty()) {
            SimpleQuery<DiskOfferingVO> dquery = dbf.createQuery(DiskOfferingVO.class);
            dquery.add(DiskOfferingVO_.uuid, SimpleQuery.Op.IN, struct.getDataDiskOfferingUuids());
            List<DiskOfferingVO> vos = dquery.list();

            // allow create multiple data volume from the same disk offering
            List<DiskOfferingInventory> disks = new ArrayList<>();
            for (final String duuid : struct.getDataDiskOfferingUuids()) {
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
            spec.setDataDiskOfferings(new ArrayList<>());
        }
        if (struct.getRootDiskOfferingUuid() != null) {
            DiskOfferingVO rootDisk = dbf.findByUuid(struct.getRootDiskOfferingUuid(), DiskOfferingVO.class);
            spec.setRootDiskOffering(DiskOfferingInventory.valueOf(rootDisk));
        }

        ImageVO imvo = dbf.findByUuid(spec.getVmInventory().getImageUuid(), ImageVO.class);
        if (imvo.getMediaType() == ImageMediaType.ISO) {
            new IsoOperator().attachIsoToVm(self.getUuid(), imvo.getUuid());
            IsoSpec isoSpec = new IsoSpec();
            isoSpec.setImageUuid(imvo.getUuid());
            spec.setDestIso(isoSpec);
        }

        spec.getImageSpec().setInventory(ImageInventory.valueOf(imvo));
        spec.setCurrentVmOperation(VmOperation.NewCreate);
        if (self.getZoneUuid() != null || self.getClusterUuid() != null || self.getHostUuid() != null) {
            spec.setHostAllocatorStrategy(HostAllocatorConstant.DESIGNATED_HOST_ALLOCATOR_STRATEGY_TYPE);
        }
        buildHostname(spec);

        spec.setUserdata(buildUserdata());
        selectBootOrder(spec);
        String instanceOfferingOnlineChange = VmSystemTags.INSTANCEOFFERING_ONLIECHANGE.
                getTokenByResourceUuid(self.getUuid(), VmSystemTags.INSTANCEOFFERING_ONLINECHANGE_TOKEN);
        if (instanceOfferingOnlineChange != null && instanceOfferingOnlineChange.equals("true")) {
            spec.setInstanceOfferingOnlineChange(true);
        }
        spec.setConsolePassword(VmSystemTags.CONSOLE_PASSWORD.
                getTokenByResourceUuid(self.getUuid(), VmSystemTags.CONSOLE_PASSWORD_TOKEN));

        changeVmStateInDb(VmInstanceStateEvent.starting);

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(BeforeStartNewCreatedVmExtensionPoint.class),
                new ForEachFunction<BeforeStartNewCreatedVmExtensionPoint>() {
                    @Override
                    public void run(BeforeStartNewCreatedVmExtensionPoint ext) {
                        ext.beforeStartNewCreatedVm(spec);
                    }
                });

        extEmitter.beforeStartNewCreatedVm(VmInstanceInventory.valueOf(self));
        FlowChain chain = getCreateVmWorkFlowChain(inv);
        setFlowMarshaller(chain);
        // add user-defined root password
        if (struct.getRootPassword() != null) {
            spec.setAccountPerference(new VmAccountPerference(self.getUuid(), "root", struct.getRootPassword()));
        }

        chain.setName(String.format("create-vm-%s", self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler(completion) {
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
                logger.debug(String.format("vm[uuid:%s] is running ..", self.getUuid()));
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                extEmitter.afterStartNewCreatedVm(inv);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                extEmitter.failedToStartNewCreatedVm(VmInstanceInventory.valueOf(self), errCode);
                dbf.remove(self);
                // clean up EO, otherwise API-retry may cause conflict if
                // the resource uuid is set
                dbf.eoCleanup(VmInstanceVO.class);
                completion.fail(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, errCode));
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
                taskChain.next();
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
        final List<VmDeletionStruct> ctx = new ArrayList<VmDeletionStruct>();
        VmDeletionStruct s = new VmDeletionStruct();
        s.setInventory(getSelfInventory());
        s.setDeletionPolicy(deletionPolicyMgr.getDeletionPolicy(self.getUuid()));
        ctx.add(s);

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

    protected VmInstanceSpec buildSpecFromInventory(VmInstanceInventory inv, VmOperation operation) {
        VmInstanceSpec spec = new VmInstanceSpec();

        spec.setUserdata(buildUserdata());

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

        // When starting an imported VM, we might not have an image UUID.
        if (inv.getImageUuid() != null) {
            ImageVO imgvo = dbf.findByUuid(inv.getImageUuid(), ImageVO.class);
            ImageInventory imginv = null;
            if (imgvo == null) {
                // the image has been deleted, use EO instead
                ImageEO imgeo = dbf.findByUuid(inv.getImageUuid(), ImageEO.class);
                imginv = ImageInventory.valueOf(imgeo);
            } else {
                imginv = ImageInventory.valueOf(imgvo);
            }
            spec.getImageSpec().setInventory(imginv);
        }

        spec.setVmInventory(inv);
        buildHostname(spec);

        String isoUuid = new IsoOperator().getIsoUuidByVmUuid(inv.getUuid());
        if (isoUuid != null) {
            if (dbf.isExist(isoUuid, ImageVO.class)) {
                IsoSpec isoSpec = new IsoSpec();
                isoSpec.setImageUuid(isoUuid);
                spec.setDestIso(isoSpec);
            } else {
                //TODO
                logger.warn(String.format("iso[uuid:%s] is deleted, however, the VM[uuid:%s] still has it attached",
                        isoUuid, self.getUuid()));
            }
        }

        spec.setCurrentVmOperation(operation);
        selectBootOrder(spec);
        String instanceOfferingOnlineChange = VmSystemTags.INSTANCEOFFERING_ONLIECHANGE.
                getTokenByResourceUuid(self.getUuid(), VmSystemTags.INSTANCEOFFERING_ONLINECHANGE_TOKEN);
        if (instanceOfferingOnlineChange != null && instanceOfferingOnlineChange.equals("true")) {
            spec.setInstanceOfferingOnlineChange(true);
        } else {
            spec.setInstanceOfferingOnlineChange(false);
        }
        spec.setConsolePassword(VmSystemTags.CONSOLE_PASSWORD.
                getTokenByResourceUuid(self.getUuid(), VmSystemTags.CONSOLE_PASSWORD_TOKEN));
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

        final VmInstanceSpec spec = buildSpecFromInventory(inv, VmOperation.Reboot);
        spec.setDestHost(HostInventory.valueOf(dbf.findByUuid(self.getHostUuid(), HostVO.class)));

        final VmInstanceState originState = self.getState();
        changeVmStateInDb(VmInstanceStateEvent.rebooting);

        extEmitter.beforeRebootVm(VmInstanceInventory.valueOf(self));
        spec.setMessage(msg);
        FlowChain chain = getRebootVmWorkFlowChain(inv);
        setFlowMarshaller(chain);

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
                extEmitter.failedToRebootVm(VmInstanceInventory.valueOf(self), errCode);
                if (HostErrors.FAILED_TO_STOP_VM_ON_HYPERVISOR.isEqual(errCode.getCode())
                        || HostErrors.FAILED_TO_START_VM_ON_HYPERVISOR.isEqual(errCode.getCode())) {
                    checkState(originalCopy.getHostUuid(), new NoErrorCompletion(completion) {
                        @Override
                        public void done() {
                            completion.fail(errCode);
                        }
                    });
                } else {
                    self.setState(originState);
                    self = dbf.updateAndRefresh(self);
                    completion.fail(errCode);
                }
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

        final VmInstanceSpec spec = buildSpecFromInventory(inv, VmOperation.Stop);
        spec.setMessage(msg);
        if (msg instanceof StopVmInstanceMsg) {
            spec.setGcOnStopFailure(((StopVmInstanceMsg) msg).isGcOnFailure());
        }

        final VmInstanceState originState = self.getState();
        changeVmStateInDb(VmInstanceStateEvent.stopping);

        extEmitter.beforeStopVm(VmInstanceInventory.valueOf(self));

        FlowChain chain = getStopVmWorkFlowChain(inv);
        setFlowMarshaller(chain);

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
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                extEmitter.failedToStopVm(inv, errCode);
                if (HostErrors.FAILED_TO_STOP_VM_ON_HYPERVISOR.isEqual(errCode.getCode())) {
                    checkState(originalCopy.getHostUuid(), new NoErrorCompletion(completion) {
                        @Override
                        public void done() {
                            completion.fail(errCode);
                        }
                    });
                } else {
                    self.setState(originState);
                    self = dbf.updateAndRefresh(self);
                    completion.fail(errCode);
                }
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

    protected void handle(final APICreateStopVmInstanceSchedulerMsg msg) {
        APICreateStopVmInstanceSchedulerEvent evt = new APICreateStopVmInstanceSchedulerEvent(msg.getId());
        StopVmInstanceJob job = new StopVmInstanceJob(msg);
        job.setVmUuid(msg.getVmInstanceUuid());
        job.setTargetResourceUuid(msg.getVmInstanceUuid());
        SchedulerVO schedulerVO = schedulerFacade.runScheduler(job);
        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), schedulerVO.getUuid(), SchedulerVO.class);
        if (schedulerVO != null) {
            schedulerVO = dbf.reload(schedulerVO);
            SchedulerInventory sinv = SchedulerInventory.valueOf(schedulerVO);
            evt.setInventory(sinv);
        }
        bus.publish(evt);
    }

    protected void handle(final APICreateStartVmInstanceSchedulerMsg msg) {
        APICreateStartVmInstanceSchedulerEvent evt = new APICreateStartVmInstanceSchedulerEvent(msg.getId());
        StartVmInstanceJob job = new StartVmInstanceJob(msg);
        job.setVmUuid(msg.getVmInstanceUuid());
        job.setTargetResourceUuid(msg.getVmInstanceUuid());
        SchedulerVO schedulerVO = schedulerFacade.runScheduler(job);
        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), schedulerVO.getUuid(), SchedulerVO.class);
        if (schedulerVO != null) {
            schedulerVO = dbf.reload(schedulerVO);
            SchedulerInventory sinv = SchedulerInventory.valueOf(schedulerVO);
            evt.setInventory(sinv);
        }
        bus.publish(evt);
    }

    protected void handle(final APICreateRebootVmInstanceSchedulerMsg msg) {
        APICreateRebootVmInstanceSchedulerEvent evt = new APICreateRebootVmInstanceSchedulerEvent(msg.getId());
        RebootVmInstanceJob job = new RebootVmInstanceJob(msg);
        job.setVmUuid(msg.getVmInstanceUuid());
        job.setTargetResourceUuid(msg.getVmInstanceUuid());
        SchedulerVO schedulerVO = schedulerFacade.runScheduler(job);
        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), schedulerVO.getUuid(), SchedulerVO.class);
        if (schedulerVO != null) {
            schedulerVO = dbf.reload(schedulerVO);
            SchedulerInventory sinv = SchedulerInventory.valueOf(schedulerVO);
            evt.setInventory(sinv);
        }
        bus.publish(evt);
    }

    protected void suspendVm(final APISuspendVmInstanceMsg msg, final SyncTaskChain taskChain) {
        suspendVm(msg, new Completion(taskChain) {
            @Override
            public void success() {
                APISuspendVmInstanceEvent evt = new APISuspendVmInstanceEvent(msg.getId());
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                evt.setInventory(inv);
                bus.publish(evt);
                taskChain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                APISuspendVmInstanceEvent evt = new APISuspendVmInstanceEvent(msg.getId());
                evt.setErrorCode(errf.instantiateErrorCode(VmErrors.SUSPEND_ERROR, errorCode));
                bus.publish(evt);
                taskChain.next();
            }
        });
    }

    protected void suspendVm(final Message msg, Completion completion) {
        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), null);
        if (allowed != null) {
            completion.fail(allowed);
            return;
        }
        if (self.getState() == VmInstanceState.Suspended) {
            completion.success();
            return;
        }
        VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
        final VmInstanceSpec spec = buildSpecFromInventory(inv, VmOperation.Suspend);
        spec.setMessage(msg);
        final VmInstanceState originState = self.getState();
        changeVmStateInDb(VmInstanceStateEvent.suspending);

        FlowChain chain = getSuspendVmWorkFlowChain(inv);
        setFlowMarshaller(chain);

        chain.setName(String.format("suspend-vm-%s", self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map Data) {
                self = changeVmStateInDb(VmInstanceStateEvent.suspended);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                self.setState(originState);
                self = dbf.updateAndRefresh(self);
                completion.fail(errCode);
            }
        }).start();
    }

    protected void handle(final APISuspendVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                suspendVm(msg, chain);
            }

            @Override
            public String getName() {
                return String.format("suspend-vm-%s", msg.getVmInstanceUuid());
            }
        });
    }

    protected void resumeVm(final APIResumeVmInstanceMsg msg, final SyncTaskChain taskChain) {
        resumeVm(msg, new Completion(taskChain) {
            @Override
            public void success() {
                APIResumeVmInstanceEvent evt = new APIResumeVmInstanceEvent(msg.getId());
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                evt.setInventory(inv);
                bus.publish(evt);
                taskChain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                APIResumeVmInstanceEvent evt = new APIResumeVmInstanceEvent(msg.getId());
                evt.setErrorCode(errf.instantiateErrorCode(VmErrors.RESUME_ERROR, errorCode));
                bus.publish(evt);
                taskChain.next();
            }
        });
    }

    protected void resumeVm(final Message msg, Completion completion) {
        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), null);
        if (allowed != null) {
            completion.fail(allowed);
            return;
        }
        VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
        final VmInstanceSpec spec = buildSpecFromInventory(inv, VmOperation.Suspend);
        spec.setMessage(msg);
        final VmInstanceState originState = self.getState();
        changeVmStateInDb(VmInstanceStateEvent.resuming);
        FlowChain chain = getResumeVmWorkFlowChain(inv);
        setFlowMarshaller(chain);

        chain.setName(String.format("resume-vm-%s", self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map Data) {
                self = changeVmStateInDb(VmInstanceStateEvent.running);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                self.setState(originState);
                self = dbf.updateAndRefresh(self);
                completion.fail(errCode);
            }
        }).start();
    }

    protected void handle(final APIResumeVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                resumeVm(msg, chain);
            }

            @Override
            public String getName() {
                return String.format("resume-vm-%s", msg.getVmInstanceUuid());
            }
        });
    }
}

