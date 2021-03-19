package org.zstack.compute.vm;


import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.allocator.HostAllocatorManager;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.allocator.*;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.configuration.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.message.*;
import org.zstack.header.network.l3.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.*;
import org.zstack.header.vm.ChangeVmMetaDataMsg.AtomicHostUuid;
import org.zstack.header.vm.ChangeVmMetaDataMsg.AtomicVmState;
import org.zstack.header.vm.VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation;
import org.zstack.header.vm.VmInstanceConstant.Params;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceSpec.CdRomSpec;
import org.zstack.header.vm.VmInstanceSpec.HostName;
import org.zstack.header.vm.VmInstanceSpec.IsoSpec;
import org.zstack.header.vm.cdrom.*;
import org.zstack.header.volume.*;
import org.zstack.identity.Account;
import org.zstack.identity.AccountManager;
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.SystemTagUtils;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.core.progress.ProgressReportService.reportProgress;
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
    protected HostAllocatorManager hostAllocatorMgr;
    @Autowired
    private VmPriorityOperator priorityOperator;
    @Autowired
    private VmNicManager nicManager;

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
                    logger.warn(String.format("unable to check state of the vm[uuid:%s] on the host[uuid:%s], %s;" +
                            "put the VM into the Unknown state", self.getUuid(), hostUuid, reply.getError()));
                    changeVmStateInDb(VmInstanceStateEvent.unknown);
                    completion.done();
                    return;
                }

                CheckVmStateOnHypervisorReply r = reply.castReply();
                String state = r.getStates().get(self.getUuid());
                if (state == null) {
                    changeVmStateInDb(VmInstanceStateEvent.unknown);
                    completion.done();
                    return;
                }

                if (VmInstanceState.Running.toString().equals(state)) {
                    changeVmStateInDb(VmInstanceStateEvent.running, ()-> self.setHostUuid(hostUuid));
                } else if (VmInstanceState.Stopped.toString().equals(state) && self.getState().equals(VmInstanceState.Destroying)) {
                    changeVmStateInDb(VmInstanceStateEvent.destroyed);
                } else if (VmInstanceState.Stopped.toString().equals(state)) {
                    changeVmStateInDb(VmInstanceStateEvent.stopped);
                } else if (VmInstanceState.Paused.toString().equals(state)) {
                    changeVmStateInDb(VmInstanceStateEvent.paused);
                } else {
                    throw new CloudRuntimeException(String.format(
                            "CheckVmStateOnHypervisorMsg should only report states[Running, Paused or Stopped]," +
                                    "but it reports %s for the vm[uuid:%s] on the host[uuid:%s]", state, self.getUuid(), hostUuid));
                }

                completion.done();
            }
        });
    }

    protected void destroy(final VmInstanceDeletionPolicy deletionPolicy, Message msg, final Completion completion) {
        if (deletionPolicy == VmInstanceDeletionPolicy.DBOnly) {
            completion.success();
            return;
        }

        if (deletionPolicy == VmInstanceDeletionPolicy.KeepVolume && self.getState().equals(VmInstanceState.Destroyed)) {
            completion.success();
            return;
        }

        final VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
        VmInstanceSpec spec = buildSpecFromInventory(inv, VmOperation.Destroy);
        if (msg instanceof ReleaseResourceMessage) {
            spec.setIgnoreResourceReleaseFailure(((ReleaseResourceMessage) msg).isIgnoreResourceReleaseFailure());
        }

        self = changeVmStateInDb(VmInstanceStateEvent.destroying);

        FlowChain chain = new SimpleFlowChain();
        setFlowBeforeFormalWorkFlow(chain, spec);
        chain.getFlows().addAll(getDestroyVmWorkFlowChain(inv).getFlows());
        setFlowMarshaller(chain);
        setAdditionalFlow(chain, spec);

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
            throw new OperationFailureException(operr("vm[uuid:%s, name:%s] has been deleted", vo.getUuid(), vo.getName()));
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

    protected FlowChain getPauseVmWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getPauseWorkFlowChain(inv);
    }

    protected FlowChain getResumeVmWorkFlowChain(VmInstanceInventory inv) {
        return vmMgr.getResumeVmWorkFlowChain(inv);
    }

    protected VmInstanceVO changeVmStateInDb(VmInstanceStateEvent stateEvent) {
        return changeVmStateInDb(stateEvent, null);
    }

    protected VmInstanceVO changeVmStateInDb(VmInstanceStateEvent stateEvent, Runnable runnable) {
        VmInstanceState bs = self.getState();
        final VmInstanceState state = self.getState().nextState(stateEvent);

        SQLBatch sql = new SQLBatch(){
            @Override
            protected void scripts() {
                self = findByUuid(self.getUuid(), self.getClass());

                if (runnable != null) {
                    runnable.run();
                }

                if (state == VmInstanceState.Stopped) {
                    // cleanup the hostUuid if the VM is stopped
                    if (self.getHostUuid() != null) {
                        self.setLastHostUuid(self.getHostUuid());
                    }
                    self.setHostUuid(null);
                } else if (state == VmInstanceState.Destroyed) {
                    // when destroying vm,vmState does not be changed to Stopped in db
                    // change LastHostUuid as hostUuid if the VM is Destroyed
                    // cleanup the HostUuid if the VM is Destroyed
                    if (self.getHostUuid() != null) {
                        self.setLastHostUuid(self.getHostUuid());
                    }
                    self.setHostUuid(null);
                }

                self.setState(state);
                self = merge(self);
            }
        };
        try {
            sql.execute();
        } catch (DataIntegrityViolationException e){
            sql.execute();
        }

        if (bs != state) {
            logger.debug(String.format("vm[uuid:%s] changed state from %s to %s in db", self.getUuid(), bs, state));

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
        if (msg instanceof InstantiateNewCreatedVmInstanceMsg) {
            handle((InstantiateNewCreatedVmInstanceMsg) msg);
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
        } else if (msg instanceof RecoverVmInstanceMsg) {
            handle((RecoverVmInstanceMsg) msg);
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
        } else if (msg instanceof OverlayMessage) {
            handle((OverlayMessage) msg);
        } else if (msg instanceof ReimageVmInstanceMsg) {
            handle((ReimageVmInstanceMsg) msg);
        } else if (msg instanceof GetVmStartingCandidateClustersHostsMsg) {
            handle((GetVmStartingCandidateClustersHostsMsg) msg);
        } else if (msg instanceof MigrateVmInnerMsg) {
            handle((MigrateVmInnerMsg) msg);
        } else if (msg instanceof AddL3NetworkToVmNicMsg) {
            handle((AddL3NetworkToVmNicMsg) msg);
        } else if (msg instanceof DeleteL3NetworkFromVmNicMsg) {
            handle((DeleteL3NetworkFromVmNicMsg) msg);
        } else if (msg instanceof DetachIsoFromVmInstanceMsg) {
            handle((DetachIsoFromVmInstanceMsg) msg);
        } else if (msg instanceof DeleteVmCdRomMsg) {
            handle((DeleteVmCdRomMsg) msg);
        } else if (msg instanceof CreateVmCdRomMsg) {
            handle((CreateVmCdRomMsg) msg);
        } else if (msg instanceof RestoreVmInstanceMsg) {
            handle((RestoreVmInstanceMsg) msg);
        } else if (msg instanceof CancelMigrateVmMsg) {
            handle((CancelMigrateVmMsg) msg);
        } else if (msg instanceof AttachIsoToVmInstanceMsg) {
            handle((AttachIsoToVmInstanceMsg) msg);
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

    private void handle(RestoreVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                StartVmInstanceReply reply = new StartVmInstanceReply();
                refreshVO();
                startVm(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        reply.setInventory(getSelfInventory());
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
                return "restore-vm";
            }
        });
    }

    private void handle(CreateVmCdRomMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                CreateVmCdRomReply reply = new CreateVmCdRomReply();

                doCreateVmCdRom(msg, new ReturnValueCompletion<VmCdRomInventory>(msg) {
                    @Override
                    public void success(VmCdRomInventory inv) {
                        reply.setInventory(inv);
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
                return String.format("create-vm-%s-cd-rom", msg.getVmInstanceUuid());
            }
        });
    }

    private void handle(MigrateVmInnerMsg msg) {
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
                        MigrateVmInnerReply evt = new MigrateVmInnerReply();
                        evt.setInventory(VmInstanceInventory.valueOf(self));
                        bus.reply(msg, evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        MigrateVmInnerReply evt = new MigrateVmInnerReply();
                        evt.setError(errorCode);
                        bus.reply(msg, evt);
                        chain.next();
                    }
                });
            }
        });
    }

    private void handle(final APIGetVmStartingCandidateClustersHostsMsg msg) {
        APIGetVmStartingCandidateClustersHostsReply reply = new APIGetVmStartingCandidateClustersHostsReply();
        final GetVmStartingCandidateClustersHostsMsg gmsg = new GetVmStartingCandidateClustersHostsMsg();
        gmsg.setUuid(msg.getUuid());
        bus.makeLocalServiceId(gmsg, VmInstanceConstant.SERVICE_ID);
        bus.send(gmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply re) {
                if (!re.isSuccess()) {
                    reply.setSuccess(false);
                    reply.setError(re.getError());
                } else {
                    GetVmStartingCandidateClustersHostsReply greply = (GetVmStartingCandidateClustersHostsReply) re;
                    reply.setHostInventories(greply.getHostInventories());
                    reply.setClusterInventories(greply.getClusterInventories());
                }
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final GetVmStartingCandidateClustersHostsMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                GetVmStartingCandidateClustersHostsReply reply = new GetVmStartingCandidateClustersHostsReply();
                getStartingCandidateHosts(msg, new ReturnValueCompletion<AllocateHostDryRunReply>(chain) {
                    @Override
                    public void success(AllocateHostDryRunReply returnValue) {
                        List<HostInventory> hosts = returnValue.getHosts();
                        if (!hosts.isEmpty()) {
                            List<String> cuuids = CollectionUtils.transformToList(hosts, HostInventory::getClusterUuid);

                            SimpleQuery<ClusterVO> cq = dbf.createQuery(ClusterVO.class);
                            cq.add(ClusterVO_.uuid, Op.IN, cuuids);
                            List<ClusterVO> cvos = cq.list();

                            reply.setClusterInventories(ClusterInventory.valueOf(cvos));
                            reply.setHostInventories(hosts);
                        } else {
                            reply.setHostInventories(hosts);
                            reply.setClusterInventories(new ArrayList<>());
                        }
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        if (HostAllocatorError.NO_AVAILABLE_HOST.toString().equals(errorCode.getCode())) {
                            reply.setHostInventories(new ArrayList<>());
                            reply.setClusterInventories(new ArrayList<>());
                        } else {
                            reply.setError(errorCode);
                        }
                        reply.setSuccess(false);
                        bus.reply(msg, reply);
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

    private void getStartingCandidateHosts(final NeedReplyMessage msg, final ReturnValueCompletion completion) {
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
        if (self.getImageUuid() != null && dbf.isExist(self.getImageUuid(), ImageVO.class)) {
            amsg.setImage(ImageInventory.valueOf(dbf.findByUuid(self.getImageUuid(), ImageVO.class)));
        }
        amsg.setL3NetworkUuids(VmNicHelper.getL3Uuids(VmNicInventory.valueOf(self.getVmNics())));
        amsg.setDryRun(true);
        amsg.setListAllHosts(true);

        bus.send(amsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply re) {
                if (!re.isSuccess()) {
                    completion.fail(re.getError());
                } else {
                    completion.success(re);
                }
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

                ErrorCodeList errList = new ErrorCodeList();
                new While<>(pluginRgty.getExtensionList(BeforeHaStartVmInstanceExtensionPoint.class)).each((ext, whileCompletion) -> {
                    ext.beforeHaStartVmInstance(msg.getVmInstanceUuid(), msg.getJudgerClassName(), msg.getSoftAvoidHostUuids(), new Completion(msg) {
                        @Override
                        public void success() {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            errList.getCauses().add(errorCode);
                            whileCompletion.done();
                        }
                    });
                }).run(new NoErrorCompletion() {
                    @Override
                    public void done() {
                        if (!errList.getCauses().isEmpty()) {
                            reply.setError(errList.getCauses().get(0));
                            bus.reply(msg, reply);
                            chain.next();
                            return;
                        }

                        logger.debug(String.format("HaStartVmJudger[%s] says the VM[uuid:%s, name:%s] is qualified for HA start, now we are starting it",
                                judger.getClass(), self.getUuid(), self.getName()));
                        UpdateQuery sql = SQL.New(VmInstanceVO.class)
                                .eq(VmInstanceVO_.uuid, self.getUuid())
                                .set(VmInstanceVO_.state, VmInstanceState.Stopped)
                                .set(VmInstanceVO_.hostUuid, null);

                        if (self.getHostUuid() != null) {
                            sql.set(VmInstanceVO_.lastHostUuid, self.getHostUuid());
                        }

                        sql.update();

                        startVm(msg, new Completion(msg, chain) {
                            @Override
                            public void success() {
                                reply.setInventory(getSelfInventory());
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
                });
            }

            @Override
            public String getName() {
                return "ha-start-vm";
            }
        });
    }

    private void changeVmIp(final String l3Uuid, final Map<Integer, String> staticIpMap, final Completion completion) {
        final VmNicVO targetNic = CollectionUtils.find(self.getVmNics(), new Function<VmNicVO, VmNicVO>() {
            @Override
            public VmNicVO call(VmNicVO arg) {
                for (UsedIpVO ip : arg.getUsedIps()) {
                    if (ip.getL3NetworkUuid().equals(l3Uuid)) {
                        return arg;
                    }
                }

                return null;
            }
        });

        if (targetNic == null) {
            throw new OperationFailureException(operr("the vm[uuid:%s] has no nic on the L3 network[uuid:%s]", self.getUuid(), l3Uuid));
        }

        /* if static ip is same to nic, do nothing */
        if (targetNic.getUsedIps().stream().map(UsedIpVO::getIp).collect(Collectors.toList()).containsAll(staticIpMap.values())) {
            completion.success();
            return;
        }

        final Map<Integer, UsedIpInventory> oldIpMap = new HashMap<>();
        final Map<Integer, UsedIpInventory> newIpMap = new HashMap<>();
        for (UsedIpVO ipvo : targetNic.getUsedIps()) {
            if (staticIpMap.get(ipvo.getIpVersion()) != null) {
                UsedIpInventory oldIp = new UsedIpInventory();
                oldIp.setIp(ipvo.getIp());
                oldIp.setGateway(ipvo.getGateway());
                oldIp.setNetmask(ipvo.getNetmask());
                oldIp.setL3NetworkUuid(ipvo.getL3NetworkUuid());
                oldIp.setUuid(ipvo.getUuid());
                oldIpMap.put(ipvo.getIpVersion(), oldIp);
            }
        }

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("change-vm-ip-l3-%s-vm-%s", l3Uuid, self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "acquire-new-ip";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<ErrorCode> errs = new ArrayList<>();
                        new While<>(staticIpMap.entrySet()).each((entry, wcomp) -> {
                            AllocateIpMsg amsg = new AllocateIpMsg();
                            amsg.setL3NetworkUuid(l3Uuid);
                            amsg.setRequiredIp(entry.getValue());
                            amsg.setIpVersion(entry.getKey());
                            bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, l3Uuid);
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        errs.add(reply.getError());
                                        wcomp.allDone();
                                    } else {
                                        AllocateIpReply r = reply.castReply();
                                        newIpMap.put(entry.getKey(), r.getIpInventory());
                                        wcomp.done();
                                    }
                                }
                            });
                        }).run(new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                if (errs.size() > 0) {
                                    trigger.fail(errs.get(0));
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (!newIpMap.isEmpty()) {
                            new While<>(newIpMap.entrySet()).each((entry, wcomp) -> {
                                UsedIpInventory ip = entry.getValue();
                                ReturnIpMsg rmsg = new ReturnIpMsg();
                                rmsg.setL3NetworkUuid(ip.getL3NetworkUuid());
                                rmsg.setUsedIpUuid(ip.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, ip.getL3NetworkUuid());
                                bus.send(rmsg, new CloudBusCallBack(wcomp) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        wcomp.done();
                                    }
                                });
                            }).run(new NoErrorCompletion() {
                                @Override
                                public void done() {
                                    trigger.rollback();
                                }
                            });

                        } else {
                            trigger.rollback();
                        }
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "change-ip-in-database";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        /* for multiple IP address, change nic.ip ONLY when set static ip of of default IP */
                        for (VmNicExtensionPoint ext : pluginRgty.getExtensionList(VmNicExtensionPoint.class)) {
                            for (UsedIpInventory ip : newIpMap.values()) {
                                ext.afterAddIpAddress(targetNic.getUuid(), ip.getUuid());
                            }
                        }
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "return-old-ip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(oldIpMap.values()).each((ip, wcomp) -> {
                            ReturnIpMsg rmsg = new ReturnIpMsg();
                            rmsg.setUsedIpUuid(ip.getUuid());
                            rmsg.setL3NetworkUuid(ip.getL3NetworkUuid());
                            bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, ip.getL3NetworkUuid());
                            bus.send(rmsg, new CloudBusCallBack(wcomp) {
                                @Override
                                public void run(MessageReply reply) {
                                    for (VmNicExtensionPoint ext : pluginRgty.getExtensionList(VmNicExtensionPoint.class)) {
                                        ext.afterDelIpAddress(targetNic.getUuid(), ip.getUuid());
                                    }
                                    wcomp.done();
                                }
                            });
                        }).run(new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                trigger.next();
                            }
                        });

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
                                        ext.vmIpChanged(vm, nic, oldIpMap, newIpMap);
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
                arg -> arg.vmBeforeExpunge(inv));

        ErrorCode error = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
        if (error != null) {
            throw new OperationFailureException(error);
        }

        if (inv.getAllVolumes().stream().anyMatch(v -> v.getType().equals(VolumeType.Data.toString()))) {
            throw new CloudRuntimeException(String.format("why the deleted vm[uuid:%s] has data volumes??? %s",
                    self.getUuid(), JSONObjectUtil.toJsonString(inv.getAllVolumes())));
        }

        VmInstanceSpec spec = buildSpecFromInventory(inv, VmOperation.Expunge);
        FlowChain chain = getExpungeVmWorkFlowChain(inv);
        setFlowMarshaller(chain);
        setAdditionalFlow(chain, spec);
        chain.setName(String.format("expunge-vm-%s", self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.getData().put(Params.DeletionPolicy, VmInstanceDeletionPolicy.Direct);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmAfterExpungeExtensionPoint.class),
                        arg -> arg.vmAfterExpunge(inv));

                callVmJustBeforeDeleteFromDbExtensionPoint();

                dbf.reload(self);
                dbf.removeCollection(self.getVmNics(), VmNicVO.class);
                dbf.removeCollection(self.getVmCdRoms(), VmCdRomVO.class);
                dbf.remove(self);
                logger.debug(String.format("successfully expunged the vm[uuid:%s]", self.getUuid()));
                dbf.eoCleanup(VmInstanceVO.class, self.getUuid());
                if (inv.getRootVolumeUuid() != null) {
                    dbf.eoCleanup(VolumeVO.class, inv.getRootVolumeUuid());
                }
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
        logger.debug(String.format("get VmStateChangedOnHostMsg for vm[uuid:%s], on host[uuid:%s], which tracing state is [%s]" +
                " and current state on host is [%s]", msg.getVmInstanceUuid(), msg.getHostUuid(), msg.getVmStateAtTracingMoment(), msg.getStateOnHost()));
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                if (msg.isFromSync()) {
                    return syncThreadName;
                } else {
                    return String.format("change-vm-state-%s", syncThreadName);
                }
            }

            @Override
            public void run(final SyncTaskChain chain) {
                logger.debug(String.format("running sync task %s with sync signature %s", getName(), getSyncSignature()));
                vmStateChangeOnHost(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("vm-%s-state-change-on-the-host-%s", msg.getVmInstanceUuid(), msg.getHostUuid());
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

        // c.f. ZSTAC-25974
        if ((originalState == VmInstanceState.Running || originalState == VmInstanceState.Starting)
                && currentState == VmInstanceState.Stopped
                && currentHostUuid.equals(originalHostUuid)) {
            return VmAbnormalLifeCycleOperation.VmStoppedOnTheSameHost;
        }

        if (VmInstanceState.intermediateStates.contains(originalState) && currentState == VmInstanceState.Running) {
            return VmAbnormalLifeCycleOperation.VmRunningFromIntermediateState;
        }

        if (VmInstanceState.intermediateStates.contains(originalState) && currentState == VmInstanceState.Stopped) {
            return VmAbnormalLifeCycleOperation.VmStoppedFromIntermediateState;
        }

        if (originalState == VmInstanceState.Running && currentState == VmInstanceState.Paused &&
                currentHostUuid.equals(originalHostUuid)) {
            return VmAbnormalLifeCycleOperation.VmPausedFromRunningStateHostNotChanged;
        }

        if (originalState == VmInstanceState.Unknown && currentState == VmInstanceState.Paused &&
                currentHostUuid.equals(originalHostUuid)) {
            return VmAbnormalLifeCycleOperation.VmPausedFromUnknownStateHostNotChanged;
        }

        if (originalState == VmInstanceState.Stopped && currentState == VmInstanceState.Paused &&
                currentHostUuid.equals(originalHostUuid)) {
            return VmAbnormalLifeCycleOperation.VmPausedFromStoppedStateHostNotChanged;
        }

        if (originalState == VmInstanceState.Migrating && currentState == VmInstanceState.Paused &&
                currentHostUuid.equals(originalHostUuid)) {
            return VmAbnormalLifeCycleOperation.VmPausedFromMigratingStateHostNotChanged;
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

        if (originalState == VmInstanceState.Paused && currentState == VmInstanceState.Running &&
                currentHostUuid.equals(originalHostUuid)) {
            return VmAbnormalLifeCycleOperation.VmRunningFromPausedStateHostNotChanged;
        }

        if (originalState == VmInstanceState.Paused && currentState == VmInstanceState.Stopped &&
                currentHostUuid.equals(originalHostUuid)) {
            return VmAbnormalLifeCycleOperation.VmStoppedFromPausedStateHostNotChanged;
        }

        if (originalState == VmInstanceState.Destroyed &&
                (currentState == VmInstanceState.Running || currentState == VmInstanceState.Paused)) {
            return VmAbnormalLifeCycleOperation.VmRunningFromDestroyed;
        }

        throw new CloudRuntimeException(String.format("unknown VM[uuid:%s] abnormal state combination[original state: %s," +
                        " current state: %s, original host:%s, current host:%s]",
                self.getUuid(), originalState, currentState, originalHostUuid, currentHostUuid));
    }

    private void vmStateChangeOnHost(final VmStateChangedOnHostMsg msg, final NoErrorCompletion completion) {
        final VmStateChangedOnHostReply reply = new VmStateChangedOnHostReply();
        if (refreshVO(true) == null) {
            // the vm has been deleted
            reply.setError(operr("the vm has been deleted"));
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

        if (originalState == currentState && currentHostUuid.equals(originalHostUuid)) {
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

        final Runnable fireEvent = () -> {
            VmTracerCanonicalEvents.VmStateChangedOnHostData data = new VmTracerCanonicalEvents.VmStateChangedOnHostData();
            data.setVmUuid(self.getUuid());
            data.setFrom(originalState);
            data.setTo(self.getState());
            data.setOriginalHostUuid(originalHostUuid);
            data.setCurrentHostUuid(self.getHostUuid());
            evtf.fire(VmTracerCanonicalEvents.VM_STATE_CHANGED_PATH, data);
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
            changeVmStateInDb(VmInstanceStateEvent.running, ()-> self.setHostUuid(msg.getHostUuid()));
            fireEvent.run();
            bus.reply(msg, reply);
            completion.done();
            return;
        } else if (operation == VmAbnormalLifeCycleOperation.VmStoppedFromUnknownStateHostNotChanged) {
            // the vm comes out of the unknown state to the stopped state
            // it happens when an operation failure led the vm from the stopped state to the unknown state,
            // and later on the vm was detected as stopped on the host again
            changeVmStateInDb(VmInstanceStateEvent.stopped, ()-> self.setHostUuid(null));
            fireEvent.run();
            bus.reply(msg, reply);
            completion.done();
            return;
        } else if (operation == VmAbnormalLifeCycleOperation.VmStoppedFromPausedStateHostNotChanged) {
            changeVmStateInDb(VmInstanceStateEvent.stopped, ()-> self.setHostUuid(msg.getHostUuid()));
            fireEvent.run();
            bus.reply(msg, reply);
            completion.done();
            return;
        } else if (operation == VmAbnormalLifeCycleOperation.VmPausedFromUnknownStateHostNotChanged
                || operation == VmAbnormalLifeCycleOperation.VmPausedFromStoppedStateHostNotChanged ) {
            //some reason led vm to unknown state and the paused vm are detected on the host again
            changeVmStateInDb(VmInstanceStateEvent.paused, ()-> self.setHostUuid(msg.getHostUuid()));
            fireEvent.run();
            bus.reply(msg, reply);
            completion.done();
            return;
        } else if (operation == VmAbnormalLifeCycleOperation.VmPausedFromRunningStateHostNotChanged
                || operation == VmAbnormalLifeCycleOperation.VmPausedFromMigratingStateHostNotChanged) {
            // just synchronize database
            changeVmStateInDb(VmInstanceStateEvent.paused, ()->self.setHostUuid(msg.getHostUuid()));
            fireEvent.run();
            bus.reply(msg, reply);
            completion.done();
            return;
        } else if (operation == VmAbnormalLifeCycleOperation.VmRunningFromPausedStateHostNotChanged) {
            // just synchronize database
            changeVmStateInDb(VmInstanceStateEvent.running, ()->self.setHostUuid(msg.getHostUuid()));
            fireEvent.run();
            bus.reply(msg, reply);
            completion.done();
            return;
        } else if (operation == VmAbnormalLifeCycleOperation.VmRunningFromDestroyed) {
            DestroyVmOnHypervisorMsg dmsg = new DestroyVmOnHypervisorMsg();
            dmsg.setVmInventory(getSelfInventory());
            if (!msg.getHostUuid().equals(dmsg.getHostUuid())) {
                dmsg.getVmInventory().setHostUuid(msg.getHostUuid());
            }
            bus.makeTargetServiceIdByResourceUuid(dmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
            bus.send(dmsg);

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
                    changeVmStateInDb(VmInstanceStateEvent.running, ()-> self.setHostUuid(currentHostUuid));
                } else if (currentState == VmInstanceState.Stopped) {
                    changeVmStateInDb(VmInstanceStateEvent.stopped);
                }

                fireEvent.run();
                bus.reply(msg, reply);
                completion.done();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                logger.warn(String.format("failed to handle abnormal lifecycle of the vm[uuid:%s, original state: %s, current state:%s," +
                                "original host: %s, current host: %s], %s", self.getUuid(), originalState, currentState,
                        originalHostUuid, currentHostUuid, errCode));

                reply.setError(errCode);
                bus.reply(msg, reply);
                completion.done();
            }
        }).start();
    }

    private List<String> buildUserdata() {
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

                VmNicVO nicVO = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).find();

                if (nicVO == null) {
                    logger.debug(String.format("vm nic[uuid:%s] not exists. It may have been deleted", msg.getVmNicUuid()));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                doDetachNic(VmNicInventory.valueOf(nicVO), true, false, new Completion(chain) {
                    @Override
                    public void success() {
                        self = dbf.reload(self);
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
                return "nic-detach";
            }
        });
    }

    private void handle(final AddL3NetworkToVmNicMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final AddL3NetworkToVmNicReply reply = new AddL3NetworkToVmNicReply();

                refreshVO();

                final ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    reply.setError(allowed);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                VmNicVO vmNicVO = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
                final VmInstanceSpec spec = buildSpecFromInventory(getSelfInventory(), VmOperation.AttachNic);
                spec.setDestNics(list(VmNicInventory.valueOf(vmNicVO)));
                L3NetworkVO l3Vo = dbf.findByUuid(msg.getNewL3Uuid(), L3NetworkVO.class);
                spec.setL3Networks(list(new VmNicSpec(L3NetworkInventory.valueOf(l3Vo))));

                FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
                fchain.setName(String.format("update-vmNic-%s-to-backend", msg.getVmInstanceUuid()));
                fchain.getData().put(Params.VmInstanceSpec.toString(), spec);
                fchain.then(new VmInstantiateResourceOnAttachingNicFlow());
                fchain.then(new VmUpdateNicOnHypervisorFlow());
                fchain.done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        self = dbf.reload(self);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                }).error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                }).start();
            }

            @Override
            public String getName() {
                return "update-vmNic-to-backend";
            }
        });
    }

    private void handle(final DeleteL3NetworkFromVmNicMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final DeleteL3NetworkFromVmNicReply reply = new DeleteL3NetworkFromVmNicReply();

                refreshVO();

                final ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    reply.setError(allowed);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                VmNicVO vmNicVO = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
                final VmInstanceSpec spec = buildSpecFromInventory(getSelfInventory(), VmOperation.AttachNic);
                spec.setDestNics(list(VmNicInventory.valueOf(vmNicVO)));
                L3NetworkVO l3Vo = dbf.findByUuid(msg.getNewL3Uuid(), L3NetworkVO.class);
                spec.setL3Networks(list(new VmNicSpec(L3NetworkInventory.valueOf(l3Vo))));

                FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
                fchain.setName(String.format("update-vmNic-%s-to-backend", msg.getVmInstanceUuid()));
                fchain.getData().put(Params.VmInstanceSpec.toString(), spec);
                fchain.then(new VmReleaseResourceOnDetachingNicFlow());
                fchain.done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        self = dbf.reload(self);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                }).error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                }).start();
            }

            @Override
            public String getName() {
                return "update-vmNic-to-backend";
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

                    changeVmStateInDb(s.getValue().getDrivenEvent(), ()-> {
                        self.setHostUuid(h.getValue());
                    });

                    reply.setChangeStateDone(true);
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
        } else {
            if (msg instanceof APIMessage){
                if (((APIMessage) msg).getSystemTags() != null){
                    amsg.setSystemTags(new ArrayList<String>(((APIMessage) msg).getSystemTags()));
                }
            }
        }
        amsg.setVmInstance(VmInstanceInventory.valueOf(self));
        amsg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        amsg.setAllocatorStrategy(HostAllocatorConstant.MIGRATE_VM_ALLOCATOR_TYPE);
        amsg.setVmOperation(VmOperation.Migrate.toString());
        amsg.setL3NetworkUuids(VmNicHelper.getL3Uuids(VmNicInventory.valueOf(self.getVmNics())));
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
        thdf.chainSubmit(new ChainTask(msg) {
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

    @Deferred
    private void attachNic(final Message msg, final List<String> l3Uuids, final ReturnValueCompletion<VmNicInventory> completion) {
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
                    self.setDefaultL3NetworkUuid(l3Uuids.get(0));
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
            private boolean allowDupicatedAddress = false;
            Map<String, List<String>> staticIpMap = null;

            void set() {
                if (msg instanceof APIAttachL3NetworkToVmMsg) {
                    APIAttachL3NetworkToVmMsg amsg = (APIAttachL3NetworkToVmMsg) msg;
                    staticIpMap = amsg.getStaticIpMap();
                } else if (msg instanceof VmAttachNicMsg) {
                    VmAttachNicMsg nicMsg = (VmAttachNicMsg)msg;
                    staticIpMap = nicMsg.getStaticIpMap();
                    allowDupicatedAddress = nicMsg.isAllowDuplicatedAddress();
                }

                if (staticIpMap == null || staticIpMap.isEmpty()) {
                    return;
                }

                for (Map.Entry<String, List<String>> e : staticIpMap.entrySet()) {
                    List<String> ips = e.getValue();
                    String l3Uuid = e.getKey();
                    for (String ip : ips) {
                        new StaticIpOperator().setStaticIp(self.getUuid(), l3Uuid, ip);
                    }
                }

                isSet = true;
            }

            void rollback() {
                if (isSet) {
                    for (Map.Entry<String, List<String>> e : staticIpMap.entrySet()) {
                        new StaticIpOperator().deleteStaticIpByVmUuidAndL3Uuid(self.getUuid(), e.getKey());
                    }
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
        final VmInstanceInventory vm = spec.getVmInventory();
        List<L3NetworkInventory> l3s = new ArrayList<>();
        for (String l3Uuid : l3Uuids) {
            L3NetworkVO l3vo = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
            final L3NetworkInventory l3 = L3NetworkInventory.valueOf(l3vo);
            l3s.add(l3);
            for (VmPreAttachL3NetworkExtensionPoint ext : pluginRgty.getExtensionList(VmPreAttachL3NetworkExtensionPoint.class)) {
                ext.vmPreAttachL3Network(vm, l3);
            }
        }

        spec.setL3Networks(list(new VmNicSpec(l3s)));
        spec.setDestNics(new ArrayList<VmNicInventory>());

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmBeforeAttachL3NetworkExtensionPoint.class),
                new ForEachFunction<VmBeforeAttachL3NetworkExtensionPoint>() {
                    @Override
                    public void run(VmBeforeAttachL3NetworkExtensionPoint arg) {
                        for (L3NetworkInventory l3 : l3s) {
                            arg.vmBeforeAttachL3Network(vm, l3);
                        }
                    }
                });

        FlowChain flowChain = FlowChainBuilder.newSimpleFlowChain();
        setFlowMarshaller(flowChain);
        flowChain.setName(String.format("attachNic-vm-%s-l3-%s", self.getUuid(), l3Uuids.get(0)));
        flowChain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        flowChain.getData().put(VmInstanceConstant.Params.VmAllocateNicFlow_allowDuplicatedAddress.toString(), setStaticIp.allowDupicatedAddress);
        flowChain.then(new VmAllocateNicFlow());
        flowChain.then(new VmSetDefaultL3NetworkOnAttachingFlow());
        if (self.getState() == VmInstanceState.Running) {
            flowChain.then(new VmInstantiateResourceOnAttachingNicFlow());
            flowChain.then(new VmAttachNicOnHypervisorFlow());
        }

        flowChain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmAfterAttachL3NetworkExtensionPoint.class),
                        new ForEachFunction<VmAfterAttachL3NetworkExtensionPoint>() {
                            @Override
                            public void run(VmAfterAttachL3NetworkExtensionPoint arg) {
                                for (L3NetworkInventory l3 : l3s) {
                                    arg.vmAfterAttachL3Network(vm, l3);
                                }
                            }
                        });
                VmNicInventory nic = spec.getDestNics().get(0);
                completion.success(nic);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmFailToAttachL3NetworkExtensionPoint.class),
                        new ForEachFunction<VmFailToAttachL3NetworkExtensionPoint>() {
                            @Override
                            public void run(VmFailToAttachL3NetworkExtensionPoint arg) {
                                for (L3NetworkInventory l3 : l3s) {
                                    arg.vmFailToAttachL3Network(vm, l3, errCode);
                                }
                            }
                        });
                setDefaultL3Network.rollback();
                setStaticIp.rollback();
                completion.fail(errCode);
            }
        }).start();
    }

    private void attachNic(final APIAttachVmNicToVmMsg msg, final ReturnValueCompletion<VmNicInventory> completion) {
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

                String vmNicUuid = msg.getVmNicUuid();
                VmNicVO vmNicVO = dbf.findByUuid(vmNicUuid, VmNicVO.class);
                String l3Uuid = VmNicHelper.getPrimaryL3Uuid(VmNicInventory.valueOf(vmNicVO));

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

                final SetDefaultL3Network setDefaultL3Network = new SetDefaultL3Network();
                setDefaultL3Network.set();
                Defer.guard(setDefaultL3Network::rollback);

                final VmInstanceSpec spec = buildSpecFromInventory(getSelfInventory(), VmOperation.AttachNic);
                spec.setVmInventory(VmInstanceInventory.valueOf(self));
                L3NetworkVO l3vo = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
                final L3NetworkInventory l3 = L3NetworkInventory.valueOf(l3vo);
                final VmInstanceInventory vm = getSelfInventory();
                for (VmPreAttachL3NetworkExtensionPoint ext : pluginRgty.getExtensionList(VmPreAttachL3NetworkExtensionPoint.class)) {
                    ext.vmPreAttachL3Network(vm, l3);
                }

                spec.setL3Networks(list(new VmNicSpec(l3)));

                CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmBeforeAttachL3NetworkExtensionPoint.class),
                        new ForEachFunction<VmBeforeAttachL3NetworkExtensionPoint>() {
                            @Override
                            public void run(VmBeforeAttachL3NetworkExtensionPoint arg) {
                                arg.vmBeforeAttachL3Network(vm, l3);
                            }
                        });

                FlowChain flowChain = FlowChainBuilder.newSimpleFlowChain();
                setFlowMarshaller(flowChain);
                flowChain.setName(String.format("attachNic-vm-%s-nic-%s", self.getUuid(), vmNicVO.getUuid()));
                flowChain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);

                flowChain.then(new Flow() {
                    String __name__ = "update-nic";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        final BitSet deviceIdBitmap = new BitSet(512);
                        for (VmNicInventory nic : spec.getVmInventory().getVmNics()) {
                            deviceIdBitmap.set(nic.getDeviceId());
                        }
                        int deviceId = deviceIdBitmap.nextClearBit(0);
                        deviceIdBitmap.set(deviceId);
                        String internalName = VmNicVO.generateNicInternalName(spec.getVmInventory().getInternalId(), deviceId);

                        UpdateQuery.New(VmNicVO.class)
                                .eq(VmNicVO_.uuid, vmNicUuid)
                                .set(VmNicVO_.vmInstanceUuid, self.getUuid())
                                .set(VmNicVO_.deviceId, deviceId)
                                .set(VmNicVO_.internalName, internalName)
                                .set(VmNicVO_.hypervisorType, spec.getVmInventory().getHypervisorType())
                                .update();

                        vmNicVO.setVmInstanceUuid(self.getUuid());
                        vmNicVO.setDeviceId(deviceId);
                        vmNicVO.setInternalName(internalName);
                        vmNicVO.setHypervisorType(spec.getVmInventory().getHypervisorType());
                        vmNicVO.setDriverType(ImagePlatform.valueOf(vm.getPlatform()).isParaVirtualization() ?
                                nicManager.getDefaultPVNicDriver() : nicManager.getDefaultNicDriver());
                        spec.getDestNics().add(0, VmNicInventory.valueOf(vmNicVO));

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        UpdateQuery.New(VmNicVO.class)
                                .eq(VmNicVO_.uuid, vmNicUuid)
                                .set(VmNicVO_.vmInstanceUuid, null)
                                .update();

                        trigger.rollback();
                    }
                });

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
                        completion.fail(errCode);
                        chain.next();
                    }
                }).start();
            }

            @Override
            public String getName() {
                return String.format("attachNic-vm-%s-nic-%s", self.getUuid(), msg.getVmNicUuid());
            }
        });
    }

    private void attachNicInQueue(final VmInstanceMessage msg, final String l3Uuid, boolean applyToBackend, final ReturnValueCompletion<VmNicInventory> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            @Deferred
            public void run(final SyncTaskChain chain) {
                String vmNicInvKey = "VmAttachNicMsg";
                FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
                fchain.setName(String.format("attach-l3-network-to-vm-%s", msg.getVmInstanceUuid()));
                fchain.then(new Flow() {
                    String __name__ = "attach-nic";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> l3Uuids = new ArrayList<>();
                        l3Uuids.add(l3Uuid);
                        attachNic((Message) msg, l3Uuids, new ReturnValueCompletion<VmNicInventory>(trigger) {
                            @Override
                            public void success(VmNicInventory returnValue) {
                                data.put(vmNicInvKey, returnValue);
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        refreshVO();
                        VmNicInventory nic = (VmNicInventory)data.get(vmNicInvKey);
                        doDetachNic(nic, true, true, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = "after-attach-nic";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        afterAttachNic((VmNicInventory) data.get(vmNicInvKey), applyToBackend, new Completion(trigger) {
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
                }).done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        VmNicInventory nic = (VmNicInventory) data.get(vmNicInvKey);

                        for (VmInstanceAttachNicExtensionPoint ext : pluginRgty.getExtensionList(VmInstanceAttachNicExtensionPoint.class)) {
                            ext.afterAttachNicToVm(nic);
                        }

                        VmNicCanonicalEvents.VmNicEventData vmNicEventData = new VmNicCanonicalEvents.VmNicEventData();
                        vmNicEventData.setCurrentStatus(self.getState().toString());
                        String vmNicAccountUuid = acntMgr.getOwnerAccountUuidOfResource(nic.getUuid());
                        vmNicEventData.setAccountUuid(vmNicAccountUuid);
                        vmNicEventData.setInventory(nic);
                        evtf.fire(VmNicCanonicalEvents.VM_NIC_CREATED_PATH, vmNicEventData);

                        completion.success(nic);
                        chain.next();
                    }
                }).error(new FlowErrorHandler(completion) {
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
        attachNicInQueue(msg, msg.getL3NetworkUuid(), msg.isApplyToBackend(), new ReturnValueCompletion<VmNicInventory>(msg) {
            @Override
            public void success(VmNicInventory returnValue) {
                reply.setInventroy(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void callVmJustBeforeDeleteFromDbExtensionPoint() {
        VmInstanceInventory inv = getSelfInventory();
        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmJustBeforeDeleteFromDbExtensionPoint.class), p -> p.vmJustBeforeDeleteFromDb(inv));
    }

    private void callVmJustAfterDeleteFromDbExtensionPoint(VmInstanceInventory inv, String accountUuid) {
        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmJustAfterDeleteFromDbExtensionPoint.class), p -> p.vmJustAfterDeleteFromDbExtensionPoint(inv, accountUuid));
    }

    protected void doDestroy(final VmInstanceDeletionPolicy deletionPolicy, Message msg, final Completion completion) {
        final VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
        extEmitter.beforeDestroyVm(inv);

        destroy(deletionPolicy, msg, new Completion(completion) {
            @Override
            public void success() {
                extEmitter.afterDestroyVm(inv);
                logger.debug(String.format("successfully deleted vm instance[name:%s, uuid:%s]", self.getName(), self.getUuid()));
                if (deletionPolicy == VmInstanceDeletionPolicy.Direct) {
                    changeVmStateInDb(VmInstanceStateEvent.destroyed);
                    callVmJustBeforeDeleteFromDbExtensionPoint();
                    dbf.removeCollection(self.getVmCdRoms(), VmCdRomVO.class);
                    dbf.remove(getSelf());
                } else if (deletionPolicy == VmInstanceDeletionPolicy.DBOnly || deletionPolicy == VmInstanceDeletionPolicy.KeepVolume) {
                    String accountUuid = acntMgr.getOwnerAccountUuidOfResource(inv.getUuid());
                    new SQLBatch() {
                        @Override
                        protected void scripts() {
                            callVmJustBeforeDeleteFromDbExtensionPoint();

                            sql(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, self.getUuid()).hardDelete();
                            sql(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, self.getUuid())
                                    .eq(VolumeVO_.type, VolumeType.Root)
                                    .hardDelete();
                            sql(VmCdRomVO.class).eq(VmCdRomVO_.vmInstanceUuid, self.getUuid()).hardDelete();
                            sql(VmInstanceVO.class).eq(VmInstanceVO_.uuid, self.getUuid()).hardDelete();
                        }
                    }.execute();
                    callVmJustAfterDeleteFromDbExtensionPoint(inv, accountUuid);
                } else if (deletionPolicy == VmInstanceDeletionPolicy.Delay) {
                    changeVmStateInDb(VmInstanceStateEvent.destroyed);
                } else if (deletionPolicy == VmInstanceDeletionPolicy.Never) {
                    logger.warn(String.format("the vm[uuid:%s] is deleted, but by it's deletion policy[Never]," +
                            " the root volume is not deleted on the primary storage", self.getUuid()));
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

    private VmInstanceDeletionPolicy getVmDeletionPolicy(final VmInstanceDeletionMsg msg) {
        if (self.getState() == VmInstanceState.Created) {
            return VmInstanceDeletionPolicy.DBOnly;
        }

        return msg.getDeletionPolicy() == null ?
                deletionPolicyMgr.getDeletionPolicy(self.getUuid()) :
                VmInstanceDeletionPolicy.valueOf(msg.getDeletionPolicy());
    }

    private void handle(final VmInstanceDeletionMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                final VmInstanceDeletionReply r = new VmInstanceDeletionReply();
                final VmInstanceDeletionPolicy deletionPolicy = getVmDeletionPolicy(msg);

                self = dbf.findByUuid(self.getUuid(), VmInstanceVO.class);
                if (self == null || self.getState() == VmInstanceState.Destroyed) {
                    // the vm has been destroyed, most likely by rollback
                    if (deletionPolicy != VmInstanceDeletionPolicy.DBOnly && deletionPolicy != VmInstanceDeletionPolicy.KeepVolume) {
                        bus.reply(msg, r);
                        chain.next();
                        return;
                    }
                }

                destroyHook(deletionPolicy, msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, r);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        r.setError(errorCode);
                        bus.reply(msg, r);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-vm";
            }
        });
    }

    protected void destroyHook(VmInstanceDeletionPolicy deletionPolicy, Message msg, Completion completion) {
        doDestroy(deletionPolicy, msg, completion);
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
                reply.setError(err(VmErrors.REBOOT_ERROR, errorCode, errorCode.getDetails()));
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
                reply.setError(err(VmErrors.STOP_ERROR, errorCode, errorCode.getDetails()));
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
                    reply.setError(operr(errorCode, "failed to create template from root volume[uuid:%s] on primary storage[uuid:%s]",
                            msg.getRootVolumeInventory().getUuid(), msg.getRootVolumeInventory().getPrimaryStorageUuid()));
                    logger.warn(reply.getError().getDetails());
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
                    r.setError(err(VmErrors.ATTACH_NETWORK_ERROR, r.getError(), r.getError().getDetails()));
                }
                bus.reply(msg, r);
            }
        });
    }

    private void handle(final RecoverVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final RecoverVmInstanceReply reply = new RecoverVmInstanceReply();
                refreshVO();

                ErrorCode error = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (error != null) {
                    reply.setError(error);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                recoverVm(new Completion(msg, chain) {
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
                return "recover-vm";
            }
        });
    }

    private void handle(final DestroyVmInstanceMsg msg) {
        final DestroyVmInstanceReply reply = new DestroyVmInstanceReply();
        final String issuer = VmInstanceVO.class.getSimpleName();

        VmDeletionStruct s = new VmDeletionStruct();
        if (msg.getDeletionPolicy() == null) {
            s.setDeletionPolicy(deletionPolicyMgr.getDeletionPolicy(self.getUuid()));
        } else {
            s.setDeletionPolicy(msg.getDeletionPolicy());
        }
        s.setInventory(getSelfInventory());
        final List<VmDeletionStruct> ctx = list(s);

        final FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("destroy-vm-%s", self.getUuid()));
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
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
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

                Defer.defer(() -> {
                    ChangeVmStateReply reply = new ChangeVmStateReply();
                    bus.reply(msg, reply);
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

    protected void setFlowBeforeFormalWorkFlow(FlowChain chain, VmInstanceSpec spec) {
        List<Flow> flows = new ArrayList<>();
        for (VmOperationAdditionalFlowExtensionPoint ext : pluginRgty.getExtensionList(VmOperationAdditionalFlowExtensionPoint.class)) {
            flows.addAll(ext.getBeforeFormalWorkFlows(spec));
        }

        for (Flow flow : flows) {
            chain.then(flow);
        }
    }

    protected void setAdditionalFlow(FlowChain chain, VmInstanceSpec spec) {
        List<Flow> flows = new ArrayList<>();
        for (VmOperationAdditionalFlowExtensionPoint ext : pluginRgty.getExtensionList(VmOperationAdditionalFlowExtensionPoint.class)) {
            flows.addAll(ext.getAdditionalVmOperationFlows(spec));
        }

        for (Flow flow : flows) {
            chain.then(flow);
        }
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

        List<CdRomSpec> cdRomSpecs = spec.getCdRomSpecs().stream()
                .filter(cdRom -> cdRom.getImageUuid() != null)
                .collect(Collectors.toList());
        if (spec.getCurrentVmOperation() == VmOperation.NewCreate && !cdRomSpecs.isEmpty()) {
            ImageVO imageVO = dbf.findByUuid(spec.getVmInventory().getImageUuid(), ImageVO.class);
            assert imageVO != null;

            if(imageVO.getMediaType() == ImageMediaType.ISO) {
                spec.setBootOrders(list(VmBootDevice.CdRom.toString()));
            } else {
                spec.setBootOrders(list(VmBootDevice.HardDisk.toString()));
            }
        } else {
            String order = VmSystemTags.BOOT_ORDER.getTokenByResourceUuid(self.getUuid(), VmSystemTags.BOOT_ORDER_TOKEN);
            if (order == null) {
                spec.setBootOrders(list(VmBootDevice.HardDisk.toString()));
            } else {
                spec.setBootOrders(list(order.split(",")));
            }
        }
    }

    protected void instantiateVmFromNewCreate(final InstantiateNewCreatedVmInstanceMsg msg, final SyncTaskChain taskChain) {
        refreshVO();
        ErrorCode error = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
        if (error != null) {
            throw new OperationFailureException(error);
        }

        error = extEmitter.preStartNewCreatedVm(msg.getVmInstanceInventory());
        if (error != null) {
            throw new OperationFailureException(error);
        }

        InstantiateNewCreatedVmInstanceReply reply = new InstantiateNewCreatedVmInstanceReply();
        instantiateVmFromNewCreate(InstantiateVmFromNewCreatedStruct.fromMessage(msg), new Completion(msg, taskChain) {
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

    protected void handle(final InstantiateNewCreatedVmInstanceMsg msg) {
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
                instantiateVmFromNewCreate(msg, chain);
            }
        });
    }

    @Transactional(readOnly = true)
    protected List<ImageInventory> getImageCandidatesForVm(ImageMediaType type) {
        String psUuid = getSelfInventory().getRootVolume().getPrimaryStorageUuid();
        PrimaryStorageVO ps = dbf.getEntityManager().find(PrimaryStorageVO.class, psUuid);
        PrimaryStorageType psType = PrimaryStorageType.valueOf(ps.getType());
        List<String> bsUuids = psType.findBackupStorage(psUuid);

        if (!bsUuids.isEmpty()) {
            String sql = "select distinct img" +
                    " from ImageVO img, ImageBackupStorageRefVO ref, BackupStorageVO bs, BackupStorageZoneRefVO bsRef" +
                    " where ref.imageUuid = img.uuid" +
                    " and img.mediaType = :imgType" +
                    " and img.state = :state" +
                    " and img.status = :status" +
                    " and img.system = :system" +
                    " and bs.uuid = ref.backupStorageUuid" +
                    " and bs.uuid in (:bsUuids)" +
                    " and bs.uuid = bsRef.backupStorageUuid" +
                    " and bsRef.zoneUuid = :zoneUuid";
            TypedQuery<ImageVO> q = dbf.getEntityManager().createQuery(sql, ImageVO.class);
            q.setParameter("zoneUuid", getSelfInventory().getZoneUuid());
            if (type != null) {
                q.setParameter("imgType", type);
            }
            q.setParameter("state", ImageState.Enabled);
            q.setParameter("status", ImageStatus.Ready);
            q.setParameter("system", false);
            q.setParameter("bsUuids", bsUuids);
            return ImageInventory.valueOf(q.getResultList());
        } else {
            return new ArrayList<>();
        }
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
        } else if (msg instanceof APIAttachL3NetworkToVmMsg) {
            handle((APIAttachL3NetworkToVmMsg) msg);
        } else if(msg instanceof APIAttachVmNicToVmMsg) {
            handle((APIAttachVmNicToVmMsg) msg);
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
        } else if (msg instanceof APISetVmSoundTypeMsg) {
            handle((APISetVmSoundTypeMsg) msg);
        } else if (msg instanceof APISetVmQxlMemoryMsg) {
            handle((APISetVmQxlMemoryMsg) msg);
        } else if (msg instanceof APIGetVmBootOrderMsg) {
            handle((APIGetVmBootOrderMsg) msg);
        } else if (msg instanceof APIGetVmDeviceAddressMsg) {
            handle((APIGetVmDeviceAddressMsg) msg);
        } else if (msg instanceof APIDeleteVmConsolePasswordMsg) {
            handle((APIDeleteVmConsolePasswordMsg) msg);
        } else if (msg instanceof APIGetVmConsolePasswordMsg) {
            handle((APIGetVmConsolePasswordMsg) msg);
        } else if (msg instanceof APIGetVmConsoleAddressMsg) {
            handle((APIGetVmConsoleAddressMsg) msg);
        } else if (msg instanceof APISetVmHostnameMsg) {
            handle((APISetVmHostnameMsg) msg);
        } else if (msg instanceof APISetVmBootModeMsg) {
            handle((APISetVmBootModeMsg) msg);
        } else if (msg instanceof APIDeleteVmBootModeMsg) {
            handle((APIDeleteVmBootModeMsg) msg);
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
        } else if (msg instanceof APIPauseVmInstanceMsg) {
            handle((APIPauseVmInstanceMsg) msg);
        } else if (msg instanceof APIResumeVmInstanceMsg) {
            handle((APIResumeVmInstanceMsg) msg);
        } else if (msg instanceof APIReimageVmInstanceMsg) {
            handle((APIReimageVmInstanceMsg) msg);
        } else if (msg instanceof APIDeleteVmCdRomMsg) {
            handle((APIDeleteVmCdRomMsg) msg);
        } else if (msg instanceof APICreateVmCdRomMsg) {
            handle((APICreateVmCdRomMsg) msg);
        } else if (msg instanceof APIUpdateVmCdRomMsg) {
            handle((APIUpdateVmCdRomMsg) msg);
        } else if (msg instanceof APIUpdateVmPriorityMsg) {
            handle((APIUpdateVmPriorityMsg) msg);
        } else if (msg instanceof APISetVmInstanceDefaultCdRomMsg) {
            handle((APISetVmInstanceDefaultCdRomMsg) msg);
        } else if (msg instanceof APIUpdateVmNicDriverMsg) {
            handle((APIUpdateVmNicDriverMsg) msg);
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

    private void handle(APIGetCandidateIsoForAttachingVmMsg msg) {
        APIGetCandidateIsoForAttachingVmReply reply = new APIGetCandidateIsoForAttachingVmReply();
        if (self.getState() != VmInstanceState.Running && self.getState() != VmInstanceState.Stopped) {
            reply.setInventories(new ArrayList<>());
            bus.reply(msg, reply);
            return;
        }

        List<ImageInventory> result = getImageCandidatesForVm(ImageMediaType.ISO);
        List<String> vmIsoList = IsoOperator.getIsoUuidByVmUuid(msg.getVmInstanceUuid());
        result = result.stream()
                .filter(iso -> !vmIsoList.contains(iso.getUuid()))
                .collect(Collectors.toList());

        for (VmAttachIsoExtensionPoint ext : pluginRgty.getExtensionList(VmAttachIsoExtensionPoint.class)) {
            ext.filtCandidateIsos(msg.getVmInstanceUuid(), result);
        }

        reply.setInventories(result);
        bus.reply(msg, reply);
    }

    private void handle(APIGetVmCapabilitiesMsg msg) {
        APIGetVmCapabilitiesReply reply = new APIGetVmCapabilitiesReply();

        VmCapabilities capabilities = new VmCapabilities();
        checkPrimaryStorageCapabilities(capabilities);
        checkImageMediaTypeCapabilities(capabilities);

        extEmitter.getVmCapabilities(getSelfInventory(), capabilities);

        reply.setCapabilities(capabilities.toMap());
        bus.reply(msg, reply);
    }

    private void checkPrimaryStorageCapabilities(VmCapabilities capabilities) {
        VolumeInventory rootVolume = getSelfInventory().getRootVolume();

        if (rootVolume == null) {
            capabilities.setSupportLiveMigration(false);
            capabilities.setSupportVolumeMigration(false);
        } else {
            SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
            q.select(PrimaryStorageVO_.type);
            q.add(PrimaryStorageVO_.uuid, Op.EQ, rootVolume.getPrimaryStorageUuid());
            String type = q.findValue();

            PrimaryStorageType psType = PrimaryStorageType.valueOf(type);

            if (self.getState() != VmInstanceState.Running) {
                capabilities.setSupportLiveMigration(false);
            } else {
                capabilities.setSupportLiveMigration(psType.isSupportVmLiveMigration());
            }

            if (self.getState() != VmInstanceState.Stopped) {
                capabilities.setSupportVolumeMigration(false);
            } else {
                capabilities.setSupportVolumeMigration(psType.isSupportVolumeMigration());
            }
        }
    }

    private void checkImageMediaTypeCapabilities(VmCapabilities capabilities) {
        ImageVO vo = null;
        ImageMediaType imageMediaType;

        if (self.getImageUuid() != null) {
            vo = dbf.findByUuid(self.getImageUuid(), ImageVO.class);
        }

        if (vo == null) {
            imageMediaType = null;
        } else {
            imageMediaType = vo.getMediaType();
        }

        if (imageMediaType == ImageMediaType.ISO || imageMediaType == null) {
            capabilities.setSupportReimage(false);
        } else {
            capabilities.setSupportReimage(true);
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
                if (msg.getStaticIp() == null) {
                    new StaticIpOperator().deleteStaticIpByVmUuidAndL3Uuid(self.getUuid(), msg.getL3NetworkUuid());
                } else {
                    new StaticIpOperator().deleteStaticIpByVmUuidAndL3Uuid(self.getUuid(), msg.getL3NetworkUuid(), IPv6NetworkUtils.ipv6AddessToTagValue(msg.getStaticIp()));
                }
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
        Map<Integer, String> staticIpMap = new HashMap<>();
        if (msg.getIp() != null) {
            if (NetworkUtils.isIpv4Address(msg.getIp())) {
                staticIpMap.put(IPv6Constants.IPv4, msg.getIp());
            } else {
                staticIpMap.put(IPv6Constants.IPv6, msg.getIp());
            }
        }
        if (msg.getIp6() != null) {
            staticIpMap.put(IPv6Constants.IPv6, msg.getIp6());
        }

        changeVmIp(msg.getL3NetworkUuid(), staticIpMap, new Completion(msg, completion) {
            @Override
            public void success() {
                if (msg.getIp() != null) {
                    new StaticIpOperator().setStaticIp(self.getUuid(), msg.getL3NetworkUuid(), msg.getIp());
                }
                if (msg.getIp6() != null) {
                    new StaticIpOperator().setStaticIp(self.getUuid(), msg.getL3NetworkUuid(), msg.getIp6());
                }
                bus.publish(evt);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
                completion.done();
            }
        });
    }

    private void handle(APISetVmBootModeMsg msg) {
        FlowChain chain = new SimpleFlowChain();
        chain.then(new Flow() {
            String __name__ = "set-vm-boot-mode";

            String originLevel;

            @Override
            public void run(FlowTrigger trigger, Map data) {
                SystemTagCreator creator = VmSystemTags.BOOT_MODE.newSystemTagCreator(self.getUuid());
                creator.setTagByTokens(map(
                        e(VmSystemTags.BOOT_MODE_TOKEN, msg.getBootMode())
                ));
                creator.recreate = true;
                creator.create();

                originLevel = msg.getBootMode();
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (originLevel == null) {
                    VmSystemTags.BOOT_MODE.delete(self.getUuid());
                } else {
                    SystemTagCreator creator = VmSystemTags.BOOT_MODE.newSystemTagCreator(self.getUuid());
                    creator.setTagByTokens(map(
                            e(VmSystemTags.BOOT_MODE_TOKEN, originLevel)
                    ));
                    creator.recreate = true;
                    creator.create();
                }

                trigger.rollback();
            }
        });
        VmInstanceSpec spec = new VmInstanceSpec();
        spec.setVmInventory(getSelfInventory());
        spec.setCurrentVmOperation(VmOperation.SetBootMode);
        setAdditionalFlow(chain, spec);

        chain.error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                APISetVmBootModeEvent evt = new APISetVmBootModeEvent(msg.getId());
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                APISetVmBootModeEvent evt = new APISetVmBootModeEvent(msg.getId());
                bus.publish(evt);
            }
        }).start();
    }

    private void handle(APIDeleteVmBootModeMsg msg) {
        APIDeleteVmBootModeEvent evt = new APIDeleteVmBootModeEvent(msg.getId());
        VmSystemTags.BOOT_MODE.delete(self.getUuid());
        bus.publish(evt);
    }

    private void handle(APIDeleteVmHostnameMsg msg) {
        APIDeleteVmHostnameEvent evt = new APIDeleteVmHostnameEvent(msg.getId());
        VmSystemTags.HOSTNAME.delete(self.getUuid());
        bus.publish(evt);
    }

    private void handle(APISetVmHostnameMsg msg) {
        if (!VmSystemTags.HOSTNAME.hasTag(self.getUuid())) {
            SystemTagCreator creator = VmSystemTags.HOSTNAME.newSystemTagCreator(self.getUuid());
            creator.setTagByTokens(map(
                    e(VmSystemTags.HOSTNAME_TOKEN, msg.getHostname())
            ));
            creator.create();
        } else {
            VmSystemTags.HOSTNAME.update(self.getUuid(), VmSystemTags.HOSTNAME.instantiateTag(
                    map(e(VmSystemTags.HOSTNAME_TOKEN, msg.getHostname()))
            ));
        }

        APISetVmHostnameEvent evt = new APISetVmHostnameEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(final APIGetVmConsoleAddressMsg msg) {
        refreshVO();
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
                    creply.setVdiPortInfo(hr.getVdiPortInfo());
                }

                bus.reply(msg, creply);
            }
        });
    }

    private void handle(APIGetVmBootOrderMsg msg) {
        APIGetVmBootOrderReply reply = new APIGetVmBootOrderReply();
        String order = VmSystemTags.BOOT_ORDER.getTokenByResourceUuid(self.getUuid(), VmSystemTags.BOOT_ORDER_TOKEN);
        if (order != null) {
            reply.setOrder(list(order.split(",")));
        } else if (!IsoOperator.isIsoAttachedToVm(msg.getUuid())) {
            reply.setOrder(list(VmBootDevice.HardDisk.toString(), VmBootDevice.Network.toString()));
        } else {
            reply.setOrder(list(VmBootDevice.HardDisk.toString(), VmBootDevice.CdRom.toString(), VmBootDevice.Network.toString()));
        }

        bus.reply(msg, reply);
    }

    private void handle(APIGetVmDeviceAddressMsg msg) {
        APIGetVmDeviceAddressReply reply = new APIGetVmDeviceAddressReply();
        GetVmDeviceAddressMsg gmsg = new GetVmDeviceAddressMsg();
        if (self.getHostUuid() == null || self.getState() != VmInstanceState.Running) {
            reply.setError(operr("VM[uuid:%s] state is not Running.", msg.getUuid()));
        }

        gmsg.setHostUuid(self.getHostUuid());
        for (String resourceType : msg.getResourceTypes()) {
            if (resourceType.equals(VolumeVO.class.getSimpleName())) {
                List<VolumeInventory> vols = new ArrayList<>(getAllDataVolumes(getSelfInventory()));
                vols.add(VolumeInventory.valueOf(self.getRootVolume()));
                gmsg.putInventories(resourceType, vols);
            }
        }
        gmsg.setVmInstanceUuid(self.getUuid());
        bus.makeLocalServiceId(gmsg, HostConstant.SERVICE_ID);
        bus.send(gmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                } else {
                    reply.setAddresses(((GetVmDeviceAddressReply) r).getAddresses());
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APISetVmBootOrderMsg msg) {
        APISetVmBootOrderEvent evt = new APISetVmBootOrderEvent(msg.getId());
        if (msg.getBootOrder() != null) {
            SystemTagCreator creator = VmSystemTags.BOOT_ORDER.newSystemTagCreator(self.getUuid());
            creator.inherent = true;
            creator.recreate = true;
            creator.setTagByTokens(map(e(VmSystemTags.BOOT_ORDER_TOKEN, StringUtils.join(msg.getBootOrder().stream().distinct().collect(Collectors.toList()), ","))));
            creator.create();
        } else {
            VmSystemTags.BOOT_ORDER.deleteInherentTag(self.getUuid());
            VmSystemTags.BOOT_ORDER.delete(self.getUuid());
        }

        boolean bootOrderOnce = false;
        if (msg.getSystemTags() != null && !msg.getSystemTags().isEmpty()) {
            Optional<String> opt = msg.getSystemTags().stream().filter(s -> VmSystemTags.BOOT_ORDER_ONCE.isMatch(s)).findAny();
            if (opt.isPresent()) {
                bootOrderOnce = Boolean.parseBoolean(
                        VmSystemTags.BOOT_ORDER_ONCE.getTokenByTag(opt.get(), VmSystemTags.BOOT_ORDER_ONCE_TOKEN)
                );
            }
        }
        if (bootOrderOnce) {
            SystemTagCreator creator = VmSystemTags.BOOT_ORDER_ONCE.newSystemTagCreator(self.getUuid());
            creator.inherent = true;
            creator.recreate = true;
            creator.setTagByTokens(map(e(VmSystemTags.BOOT_ORDER_ONCE_TOKEN, String.valueOf(true))));
            creator.create();
        } else {
            VmSystemTags.BOOT_ORDER_ONCE.deleteInherentTag(self.getUuid());
            VmSystemTags.BOOT_ORDER_ONCE.delete(self.getUuid());
        }
        //No need to use this tag: cdromBootOnce
        if (VmSystemTags.CDROM_BOOT_ONCE.hasTag(self.getUuid(), VmInstanceVO.class)) {
            VmSystemTags.CDROM_BOOT_ONCE.deleteInherentTag(self.getUuid());
            VmSystemTags.CDROM_BOOT_ONCE.delete(self.getUuid());
        }
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(APISetVmConsolePasswordMsg msg) {
        APISetVmConsolePasswordEvent evt = new APISetVmConsolePasswordEvent(msg.getId());

        FlowChain chain = new SimpleFlowChain();
        chain.then(new Flow() {
            String __name__ = "set-vm-console-password";

            String password;

            @Override
            public void run(FlowTrigger trigger, Map data) {
                SystemTagCreator creator = VmSystemTags.CONSOLE_PASSWORD.newSystemTagCreator(self.getUuid());
                creator.setTagByTokens(map(e(VmSystemTags.CONSOLE_PASSWORD_TOKEN, msg.getConsolePassword())));
                creator.recreate = true;
                creator.create();
                password = msg.getConsolePassword();
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (password == null) {
                    VmSystemTags.CONSOLE_PASSWORD.delete(self.getUuid());
                } else {
                    SystemTagCreator creator = VmSystemTags.CONSOLE_PASSWORD.newSystemTagCreator(self.getUuid());
                    creator.setTagByTokens(map(
                            e(VmSystemTags.CONSOLE_PASSWORD_TOKEN, password)
                    ));
                    creator.recreate = true;
                    creator.create();
                }
            }
        });

        VmInstanceSpec spec = new VmInstanceSpec();
        spec.setVmInventory(getSelfInventory());
        spec.setCurrentVmOperation(VmOperation.SetConsolePassword);
        setAdditionalFlow(chain, spec);

        chain.error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                evt.setInventory(getSelfInventory());
                bus.publish(evt);
            }
        }).start();
    }

    private void handle(APISetVmSoundTypeMsg msg) {
        APISetVmSoundTypeEvent evt = new APISetVmSoundTypeEvent(msg.getId());
        SystemTagCreator creator = VmSystemTags.SOUND_TYPE.newSystemTagCreator(self.getUuid());
        creator.setTagByTokens(map(e(VmSystemTags.SOUND_TYPE_TOKEN, msg.getSoundType())));
        creator.recreate = true;
        creator.create();
        bus.publish(evt);
    }

    private void handle(APISetVmQxlMemoryMsg msg) {
        APISetVmQxlMemoryEvent evt = new APISetVmQxlMemoryEvent(msg.getId());
        SystemTagCreator creator = VmSystemTags.QXL_MEMORY.newSystemTagCreator(self.getUuid());
        creator.setTagByTokens(map(
                e(VmSystemTags.QXL_RAM_TOKEN, msg.getRam()),
                e(VmSystemTags.QXL_VRAM_TOKEN, msg.getVram()),
                e(VmSystemTags.QXL_VGAMEM_TOKEN, msg.getVgamem())
        ));
        creator.recreate = true;
        creator.create();
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

        FlowChain chain = new SimpleFlowChain();
        chain.then(new NoRollbackFlow() {
            String __name__ = "delete-vm-console-password";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmSystemTags.CONSOLE_PASSWORD.delete(self.getUuid());
                trigger.next();
            }
        });

        VmInstanceSpec spec = new VmInstanceSpec();
        spec.setVmInventory(getSelfInventory());
        spec.setCurrentVmOperation(VmOperation.SetConsolePassword);
        setAdditionalFlow(chain, spec);

        chain.error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                evt.setInventory(getSelfInventory());
                bus.publish(evt);
            }
        }).start();
    }


    private void handle(APISetVmSshKeyMsg msg) {
        APISetVmSshKeyEvent evt = new APISetVmSshKeyEvent(msg.getId());
        SystemTagCreator creator = VmSystemTags.SSHKEY.newSystemTagCreator(self.getUuid());
        creator.setTagByTokens(map(e(VmSystemTags.SSHKEY_TOKEN, msg.getSshKey())));
        creator.recreate = true;
        creator.create();
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

    private boolean ipExists(final String l3uuid, final String ipAddress) {
        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.add(VmNicVO_.l3NetworkUuid, Op.EQ, l3uuid);
        q.add(VmNicVO_.ip, Op.EQ, ipAddress);
        return q.isExists();
    }

    // If the VM is assigned static IP and it is now occupied, we will
    // remove the static IP tag so that it can acquire IP dynamically.
    // c.f. issue #1639
    private void checkIpConflict(final String vmUuid) {
        StaticIpOperator ipo = new StaticIpOperator();

        for (Map.Entry<String, List<String>> entry : ipo.getStaticIpbyVmUuid(vmUuid).entrySet()) {
            for (String ip : entry.getValue()) {
                if (ipExists(entry.getKey(), ip)) {
                    ipo.deleteStaticIpByVmUuidAndL3Uuid(vmUuid, entry.getKey());
                }
            }
        }
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
                    String __name__ = "check-ip-conflict";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        checkIpConflict(vm.getUuid());
                        trigger.next();
                    }
                });

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
                    String __name__ = "recover-cache-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<String> cacheVolumeUuids = self.getAllVolumes().stream()
                                .filter(vol -> vol.getType().equals(VolumeType.Cache))
                                .map(VolumeVO::getUuid)
                                .collect(Collectors.toList());

                        List<RecoverVolumeMsg> rmsgs = new ArrayList<>();
                        for (String volumeUuid : cacheVolumeUuids) {
                            RecoverVolumeMsg rmsg = new RecoverVolumeMsg();
                            rmsg.setVolumeUuid(volumeUuid);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, VolumeConstant.SERVICE_ID, volumeUuid);
                            rmsgs.add(rmsg);
                        }

                        new While<>(rmsgs).each((rmsg, c) -> bus.send(rmsg, new CloudBusCallBack(c) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.debug("failed to recover cache volume");
                                }

                                c.done();
                            }
                        })).run(new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                trigger.next();
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
                    evt.setError(error);
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
                        evt.setError(errorCode);
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
                        evt.setError(errorCode);
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

    private void handle(final DetachIsoFromVmInstanceMsg msg) {
        DetachIsoFromVmInstanceReply reply = new DetachIsoFromVmInstanceReply();

        detachIso(msg.getIsoUuid() ,new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
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
                    evt.setError(allowed);
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                detachIso(msg.getIsoUuid() ,new Completion(msg, chain) {
                    @Override
                    public void success() {
                        self = dbf.reload(self);
                        evt.setInventory(getSelfInventory());
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
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

    private void detachIso(final String isoUuid, final Completion completion) {
        if (!IsoOperator.isIsoAttachedToVm(self.getUuid())) {
            completion.success();
            return;
        }

        if (!IsoOperator.getIsoUuidByVmUuid(self.getUuid()).contains(isoUuid)) {
            completion.success();
            return;
        }

        VmCdRomVO targetVmCdRomVO = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, self.getUuid())
                .eq(VmCdRomVO_.isoUuid, isoUuid)
                .find();
        assert targetVmCdRomVO != null;

        if (self.getState() == VmInstanceState.Stopped || self.getState() == VmInstanceState.Destroyed) {
            targetVmCdRomVO.setIsoUuid(null);
            targetVmCdRomVO.setIsoInstallPath(null);
            dbf.update(targetVmCdRomVO);
            new IsoOperator().syncVmIsoSystemTag(self.getUuid());
            completion.success();
            return;
        }

        VmInstanceSpec spec = buildSpecFromInventory(getSelfInventory(), VmOperation.DetachIso);
        boolean isoNotExist = spec.getDestIsoList().stream().noneMatch(isoSpec -> isoSpec.getImageUuid().equals(isoUuid));
        if (isoNotExist) {
            // the image ISO has been deleted from backup storage
            // try to detach it from the VM anyway
            IsoSpec isoSpec = new IsoSpec();
            isoSpec.setImageUuid(isoUuid);
            spec.getDestIsoList().add(isoSpec);
            logger.debug(String.format("the iso[uuid:%s] has been deleted, try to detach it from the VM[uuid:%s] anyway",
                    isoUuid, self.getUuid()));
        }

        FlowChain chain = getDetachIsoWorkFlowChain(spec.getVmInventory());
        chain.setName(String.format("detach-iso-%s-from-vm-%s", isoUuid, self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.getData().put(VmInstanceConstant.Params.DetachingIsoUuid.toString(), isoUuid);

        setFlowMarshaller(chain);
        setAdditionalFlow(chain, spec);

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                targetVmCdRomVO.setIsoUuid(null);
                targetVmCdRomVO.setIsoInstallPath(null);
                dbf.update(targetVmCdRomVO);
                new IsoOperator().syncVmIsoSystemTag(self.getUuid());
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
        if (self.getClusterUuid() == null){
            return getAttachableL3NetworkWhenClusterUuidSetNull(l3Uuids);
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
                        " and l3.category != :l3Category" +
                        " group by l3.uuid";
                q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
                q.setParameter("l3Category", L3NetworkCategory.System);
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
                        " and l3.category != :l3Category" +
                        " group by l3.uuid";
                q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
                q.setParameter("l3uuids", l3Uuids);
                q.setParameter("l3Category", L3NetworkCategory.System);
            }
        } else {
            if (l3Uuids == null) {
                // accessed by a system admin
                sql = "select l3" +
                        " from L3NetworkVO l3, VmInstanceVO vm, L2NetworkVO l2, L2NetworkClusterRefVO l2ref" +
                        " where l3.uuid not in" +
                        " (select ip.l3NetworkUuid from VmNicVO nic, UsedIpVO ip where ip.vmNicUuid = nic.uuid and nic.vmInstanceUuid = :uuid)" +
                        " and vm.uuid = :uuid" +
                        " and vm.clusterUuid = l2ref.clusterUuid" +
                        " and l2ref.l2NetworkUuid = l2.uuid" +
                        " and l2.uuid = l3.l2NetworkUuid" +
                        " and l3.state = :l3State" +
                        " and l3.category != :l3Category" +
                        " group by l3.uuid";
                q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
                q.setParameter("l3Category", L3NetworkCategory.System);
            } else {
                // accessed by a normal account
                sql = "select l3" +
                        " from L3NetworkVO l3, VmInstanceVO vm, L2NetworkVO l2, L2NetworkClusterRefVO l2ref" +
                        " where l3.uuid not in" +
                        " (select ip.l3NetworkUuid from VmNicVO nic, UsedIpVO ip where ip.vmNicUuid = nic.uuid and nic.vmInstanceUuid = :uuid)" +
                        " and vm.uuid = :uuid" +
                        " and vm.clusterUuid = l2ref.clusterUuid" +
                        " and l2ref.l2NetworkUuid = l2.uuid" +
                        " and l2.uuid = l3.l2NetworkUuid" +
                        " and l3.state = :l3State" +
                        " and l3.category != :l3Category" +
                        " and l3.uuid in (:l3uuids)" +
                        " group by l3.uuid";
                q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
                q.setParameter("l3uuids", l3Uuids);
                q.setParameter("l3Category", L3NetworkCategory.System);
            }
        }

        q.setParameter("l3State", L3NetworkState.Enabled);
        q.setParameter("uuid", self.getUuid());

        List<L3NetworkVO> l3s = q.getResultList();
        l3s = l3s.stream().filter(l3 -> !IpRangeHelper.getNormalIpRanges(l3).isEmpty()).collect(Collectors.toList());

        return L3NetworkInventory.valueOf(l3s);
    }

    @Transactional(readOnly = true)
    private List<L3NetworkInventory> getAttachableL3NetworkWhenClusterUuidSetNull(List<String> uuids){
        return new SQLBatchWithReturn<List<L3NetworkInventory>>() {

            @Override
            protected List<L3NetworkInventory> scripts(){
                String rootPsUuid = self.getRootVolume().getPrimaryStorageUuid();

                //Get Candidate ClusterUuids From Primary Storage
                List<String> clusterUuids =  q(PrimaryStorageClusterRefVO.class)
                        .select(PrimaryStorageClusterRefVO_.clusterUuid)
                        .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, rootPsUuid)
                        .listValues();

                //filtering the ClusterUuid by vmNic L3s one by one
                if (!self.getVmNics().isEmpty()){
                    for (String l3uuid: self.getVmNics().stream().flatMap(nic -> VmNicHelper.getL3Uuids(VmNicInventory.valueOf(nic)).stream())
                            .distinct().collect(Collectors.toList())){
                        clusterUuids = getCandidateClusterUuidsFromAttachedL3(l3uuid, clusterUuids);
                        if (clusterUuids.isEmpty()){
                            return new ArrayList<>();
                        }
                    }
                }

                //Get enabled l3 from the Candidate ClusterUuids
                List<L3NetworkVO> l3s = sql("select l3" +
                        " from L3NetworkVO l3, L2NetworkVO l2, " +
                        " L2NetworkClusterRefVO l2ref" +
                        " where l2.uuid = l3.l2NetworkUuid " +
                        " and l2.uuid = l2ref.l2NetworkUuid " +
                        " and l2ref.clusterUuid in (:Uuids)" +
                        " and l3.state = :l3State " +
                        " and l3.category != :l3Category" +
                        " group by l3.uuid")
                        .param("Uuids", clusterUuids)
                        .param("l3Category", L3NetworkCategory.System)
                        .param("l3State", L3NetworkState.Enabled).list();

               if (l3s.isEmpty()){
                   return new ArrayList<>();
               }

               //filter result if normal user
               if (uuids != null) {
                   l3s = l3s.stream().filter(l3 -> uuids.contains(l3.getUuid())).collect(Collectors.toList());
               }

               if (l3s.isEmpty()){
                   return new ArrayList<>();
               }
               //filter l3 that already attached
               if (!self.getVmNics().isEmpty()) {
                   List<String> vmL3Uuids = self.getVmNics().stream().flatMap(nic -> VmNicHelper.getL3Uuids(VmNicInventory.valueOf(nic)).stream())
                           .distinct().collect(Collectors.toList());
                   l3s = l3s.stream().filter(l3 -> !vmL3Uuids.contains(l3.getUuid())).collect(Collectors.toList());
               }

                l3s = l3s.stream().filter(l3 -> !IpRangeHelper.getNormalIpRanges(l3).isEmpty()).collect(Collectors.toList());
               return L3NetworkInventory.valueOf(l3s);
            }

            private List<String> getCandidateClusterUuidsFromAttachedL3(String l3Uuid, List<String> clusterUuids) {
                return sql("select l2ref.clusterUuid " +
                        " from L3NetworkVO l3, L2NetworkVO l2, L2NetworkClusterRefVO l2ref " +
                        " where l3.uuid = :l3Uuid " +
                        " and l3.l2NetworkUuid = l2.uuid " +
                        " and l2.uuid = l2ref.l2NetworkUuid" +
                        " and l3.category != :l3Category" +
                        " and l2ref.clusterUuid in (:uuids) " +
                        " group by l2ref.clusterUuid", String.class)
                        .param("l3Uuid", l3Uuid)
                        .param("l3Category", L3NetworkCategory.System)
                        .param("uuids", clusterUuids).list();
            }
        }.execute();
    }

    private void handle(APIGetVmAttachableL3NetworkMsg msg) {
        APIGetVmAttachableL3NetworkReply reply = new APIGetVmAttachableL3NetworkReply();
        List<L3NetworkInventory> l3Invs = getAttachableL3Network(msg.getSession().getAccountUuid());
        List<L3NetworkInventory> ret = new ArrayList<>();

        for (L3NetworkInventory l3 : l3Invs) {
            try {
                for (VmPreAttachL3NetworkExtensionPoint ext : pluginRgty.getExtensionList(VmPreAttachL3NetworkExtensionPoint.class)) {
                    ext.vmPreAttachL3Network(VmInstanceInventory.valueOf(self), l3);
                }
                ret.add(l3);
            } catch (Exception e) {
                /* vmPreAttachL3Network will filter out the l3s that can not be attached */
            }
        }

        reply.setInventories(ret);
        bus.reply(msg, reply);
    }

    private void handle(final AttachIsoToVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                final AttachIsoToVmInstanceReply reply = new AttachIsoToVmInstanceReply();

                refreshVO();
                ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    reply.setError(allowed);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                attachIso(msg.getIsoUuid(), msg.getCdRomUuid(), new Completion(msg, chain) {
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
                return String.format("attach-iso-%s-to-vm-%s", msg.getIsoUuid(), self.getUuid());
            }
        });
    }

    private void handle(final APIAttachIsoToVmInstanceMsg msg) {
        APIAttachIsoToVmInstanceEvent evt = new APIAttachIsoToVmInstanceEvent(msg.getId());

        AttachIsoToVmInstanceMsg amsg = new AttachIsoToVmInstanceMsg();
        amsg.setIsoUuid(msg.getIsoUuid());
        amsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        amsg.setCdRomUuid(msg.getCdRomUuid());
        bus.makeTargetServiceIdByResourceUuid(amsg, VmInstanceConstant.SERVICE_ID, amsg.getVmInstanceUuid());
        bus.send(amsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                } else {
                    refreshVO();
                    evt.setInventory(getSelfInventory());
                }

                bus.publish(evt);
            }
        });
    }

    private void attachIso(final String isoUuid, String specifiedCdRomUuid, final Completion completion) {
        checkIfIsoAttachable(isoUuid);
        IsoOperator.checkAttachIsoToVm(self.getUuid(), isoUuid);

        List<VmInstanceInventory> vms = list(VmInstanceInventory.valueOf(self));
        for (VmAttachIsoExtensionPoint ext : pluginRgty.getExtensionList(VmAttachIsoExtensionPoint.class)) {
            ErrorCode err = ext.filtCandidateVms(isoUuid, vms);
            if (err != null) {
                completion.fail(err);
                return;
            }
        }

        VmCdRomVO vmCdRomVO = null;
        if (StringUtils.isNotEmpty(specifiedCdRomUuid)) {
            vmCdRomVO = dbf.findByUuid(specifiedCdRomUuid, VmCdRomVO.class);
        } else {
            vmCdRomVO = IsoOperator.getEmptyCdRom(self.getUuid());
        }

        final VmCdRomVO targetVmCdRomVO = vmCdRomVO;

        if (self.getState() == VmInstanceState.Stopped) {
            targetVmCdRomVO.setIsoUuid(isoUuid);
            dbf.update(targetVmCdRomVO);
            completion.success();
            new IsoOperator().syncVmIsoSystemTag(self.getUuid());
            return;
        }

        final ImageInventory iso = ImageInventory.valueOf(dbf.findByUuid(isoUuid, ImageVO.class));
        VmInstanceSpec spec = buildSpecFromInventory(getSelfInventory(), VmOperation.AttachIso);

        IsoSpec isoSpec = new IsoSpec();
        isoSpec.setImageUuid(isoUuid);
        isoSpec.setDeviceId(targetVmCdRomVO.getDeviceId());
        spec.getDestIsoList().add(isoSpec);

        FlowChain chain = getAttachIsoWorkFlowChain(spec.getVmInventory());
        chain.setName(String.format("attach-iso-%s-to-vm-%s", isoUuid, self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.getData().put(Params.AttachingIsoInventory.toString(), iso);

        setFlowMarshaller(chain);
        setAdditionalFlow(chain, spec);

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                // new IsoOperator().attachIsoToVm(self.getUuid(), isoUuid);
                final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                final VmInstanceSpec.IsoSpec isoSpec = spec.getDestIsoList().stream()
                        .filter(s -> s.getImageUuid().equals(isoUuid))
                        .findAny()
                        .get();
                targetVmCdRomVO.setIsoUuid(isoUuid);
                targetVmCdRomVO.setIsoInstallPath(isoSpec.getInstallPath());
                dbf.update(targetVmCdRomVO);
                new IsoOperator().syncVmIsoSystemTag(self.getUuid());
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
        if (!bsUuids.isEmpty()) {
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

        throw new OperationFailureException(operr("the ISO[uuid:%s] is on backup storage that is not compatible of the primary storage[uuid:%s]" +
                        " where the VM[name:%s, uuid:%s] is on", isoUuid, psUuid, self.getName(), self.getUuid()));
    }

    private void doDetachNic(VmNicInventory vmNic, boolean releaseNic, boolean isRollback, Completion completion) {
        FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
        fchain.setName(String.format("detach-l3-network-from-vm-%s", vmNic.getVmInstanceUuid()));
        fchain.then(new NoRollbackFlow() {
            String __name__ = "before-detach-nic";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                beforeDetachNic(vmNic, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "detach-nic";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                detachNic(vmNic.getUuid(), releaseNic, isRollback, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(final APIDetachL3NetworkFromVmMsg msg) {
        VmNicVO vmNicVO = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        String vmNicAccountUuid = acntMgr.getOwnerAccountUuidOfResource(vmNicVO.getUuid());

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
                    evt.setError(allowed);
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                String releaseNicFlag = msg.getSystemTags() == null ? null : SystemTagUtils.findTagValue(msg.getSystemTags(), VmSystemTags.RELEASE_NIC_AFTER_DETACH_NIC, VmSystemTags.RELEASE_NIC_AFTER_DETACH_NIC_TOKEN);
                boolean releaseNic = releaseNicFlag == null || Boolean.parseBoolean(releaseNicFlag);

                doDetachNic(VmNicInventory.valueOf(vmNicVO), releaseNic, false, new Completion(chain) {
                    @Override
                    public void success() {
                        self = dbf.reload(self);
                        evt.setInventory(VmInstanceInventory.valueOf(self));
                        bus.publish(evt);
                        chain.next();

                        VmNicInventory vmNicInventory = VmNicInventory.valueOf(vmNicVO);
                        VmNicCanonicalEvents.VmNicEventData vmNicEventData = new VmNicCanonicalEvents.VmNicEventData();
                        vmNicEventData.setCurrentStatus(VmInstanceState.Destroyed.toString());
                        vmNicEventData.setAccountUuid(vmNicAccountUuid);
                        vmNicEventData.setInventory(vmNicInventory);
                        evtf.fire(VmNicCanonicalEvents.VM_NIC_DELETED_PATH, vmNicEventData);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
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

    protected void beforeDetachNic(VmNicInventory nicInventory, Completion completion) {
        completion.success();
    }

    // switch vm default nic if vm current default nic is input parm nic
    protected void selectDefaultL3(VmNicInventory nic) {
        if (self.getDefaultL3NetworkUuid() != null && !VmNicHelper.isDefaultNic(nic, VmInstanceInventory.valueOf(self))) {
            return;
        }

        final VmInstanceInventory vm = getSelfInventory();
        final String previousDefaultL3 = vm.getDefaultL3NetworkUuid();

        // the nic has been removed, reload
        self = dbf.reload(self);

        final VmNicVO candidate = CollectionUtils.find(self.getVmNics(), new Function<VmNicVO, VmNicVO>() {
            @Override
            public VmNicVO call(VmNicVO arg) {
                return arg.getUuid().equals(nic.getUuid()) ? null : arg;
            }
        });

        if (candidate != null) {
            String newDefaultL3 = VmNicHelper.getPrimaryL3Uuid(VmNicInventory.valueOf(candidate));
            CollectionUtils.safeForEach(
                    pluginRgty.getExtensionList(VmDefaultL3NetworkChangedExtensionPoint.class),
                    new ForEachFunction<VmDefaultL3NetworkChangedExtensionPoint>() {
                        @Override
                        public void run(VmDefaultL3NetworkChangedExtensionPoint ext) {
                            ext.vmDefaultL3NetworkChanged(vm, previousDefaultL3, newDefaultL3);
                        }
                    });

            self.setDefaultL3NetworkUuid(newDefaultL3);
            logger.debug(String.format(
                    "after detaching the nic[uuid:%s, L3 uuid:%s], change the default L3 of the VM[uuid:%s]" +
                            " to the L3 network[uuid: %s]", nic.getUuid(), VmNicHelper.getL3Uuids(nic), self.getUuid(),
                    newDefaultL3));
        } else {
            self.setDefaultL3NetworkUuid(null);
            logger.debug(String.format(
                    "after detaching the nic[uuid:%s, L3 uuid:%s], change the default L3 of the VM[uuid:%s]" +
                            " to null, as the VM has no other nics", nic.getUuid(), VmNicHelper.getL3Uuids(nic), self.getUuid()));
        }

        self = dbf.updateAndRefresh(self);
    }

    private void detachNic(final String nicUuid, boolean releaseNic, boolean isRollback, final Completion completion) {
        VmNicVO vmNicVO = CollectionUtils.find(self.getVmNics(), new Function<VmNicVO, VmNicVO>() {
            @Override
            public VmNicVO call(VmNicVO arg) {
                return arg.getUuid().equals(nicUuid) ? arg : null;
            }
        });
        if (vmNicVO == null) {
            completion.success();
            return;
        }
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
        L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class));
        spec.setL3Networks(list(new VmNicSpec(l3Inv)));

        FlowChain flowChain = FlowChainBuilder.newSimpleFlowChain();
        flowChain.setName(String.format("detachNic-vm-%s-nic-%s", self.getUuid(), nicUuid));
        setFlowMarshaller(flowChain);
        flowChain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        flowChain.getData().put(Params.ReleaseNicAfterDetachNic.toString(), releaseNic);
        if (self.getState() == VmInstanceState.Running) {
            flowChain.then(new VmDetachNicOnHypervisorFlow());
        }
        flowChain.then(new VmReleaseResourceOnDetachingNicFlow());
        flowChain.then(new VmDetachNicFlow());
        flowChain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                afterDetachNic(nic, isRollback, new Completion(trigger) {
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
        flowChain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                selectDefaultL3(nic);
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
                for (UsedIpInventory ip : nic.getUsedIps()) {
                    new StaticIpOperator().deleteStaticIpByVmUuidAndL3Uuid(self.getUuid(), ip.getL3NetworkUuid());
                }
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
                APIChangeInstanceOfferingEvent evt = new APIChangeInstanceOfferingEvent(msg.getId());
                refreshVO();
                ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    evt.setError(allowed);
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                changeOffering(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        refreshVO();
                        evt.setInventory(getSelfInventory());
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });

            }

            @Override
            public String getName() {
                return "change-instance-offering";
            }
        });
    }

    private void changeOffering(APIChangeInstanceOfferingMsg msg, final Completion completion) {
        final InstanceOfferingVO newOfferingVO = dbf.findByUuid(msg.getInstanceOfferingUuid(), InstanceOfferingVO.class);
        final InstanceOfferingInventory inv = InstanceOfferingInventory.valueOf(newOfferingVO);
        final VmInstanceInventory vm = getSelfInventory();

        List<ChangeInstanceOfferingExtensionPoint> exts = pluginRgty.getExtensionList(ChangeInstanceOfferingExtensionPoint.class);
        exts.forEach(ext -> ext.preChangeInstanceOffering(vm, inv));
        CollectionUtils.safeForEach(exts, ext -> ext.beforeChangeInstanceOffering(vm, inv));

        changeCpuAndMemory(inv.getCpuNum(), inv.getMemorySize(), new Completion(completion) {
            @Override
            public void success() {
                self.setAllocatorStrategy(inv.getAllocatorStrategy());
                self.setInstanceOfferingUuid(msg.getInstanceOfferingUuid());
                self = dbf.updateAndRefresh(self);
                CollectionUtils.safeForEach(exts, ext -> ext.afterChangeInstanceOffering(vm, inv));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void changeCpuAndMemory(final int cpuNum, final long memorySize, final Completion completion) {
        if (self.getState() == VmInstanceState.Stopped) {
            self.setCpuNum(cpuNum);
            self.setMemorySize(memorySize);
            self = dbf.updateAndRefresh(self);
            completion.success();
            return;
        }

        final int oldCpuNum = self.getCpuNum();
        final long oldMemorySize = self.getMemorySize();

        class AlignmentStruct {
            long alignedMemory;
        }

        final AlignmentStruct struct = new AlignmentStruct();
        struct.alignedMemory = memorySize;

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("change-cpu-and-memory-of-vm-%s", self.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "align-memory";

            @Override
            public void run(FlowTrigger chain, Map data) {
                // align memory
                long increaseMemory = memorySize - oldMemorySize;
                long remainderMemory = increaseMemory % SizeUnit.MEGABYTE.toByte(128);
                if (increaseMemory != 0 && remainderMemory != 0) {
                    if (remainderMemory < SizeUnit.MEGABYTE.toByte(128) / 2) {
                        increaseMemory = increaseMemory / SizeUnit.MEGABYTE.toByte(128) * SizeUnit.MEGABYTE.toByte(128);
                    } else {
                        increaseMemory = (increaseMemory / SizeUnit.MEGABYTE.toByte(128) + 1) * SizeUnit.MEGABYTE.toByte(128);
                    }

                    if (increaseMemory == 0) {
                        struct.alignedMemory = oldMemorySize + SizeUnit.MEGABYTE.toByte(128);
                    } else {
                        struct.alignedMemory = oldMemorySize + increaseMemory;
                    }

                    logger.debug(String.format("automatically align memory from %s to %s", memorySize, struct.alignedMemory));
                }
                chain.next();
            }
        }).then(new Flow() {
            String __name__ = String.format("allocate-host-capacity-on-host-%s", self.getHostUuid());
            boolean result = false;

            @Override
            public void run(FlowTrigger chain, Map data) {
                DesignatedAllocateHostMsg msg = new DesignatedAllocateHostMsg();
                msg.setCpuCapacity(cpuNum - oldCpuNum);
                msg.setMemoryCapacity(struct.alignedMemory - oldMemorySize);
                msg.setOldMemoryCapacity(oldMemorySize);
                msg.setAllocatorStrategy(HostAllocatorConstant.DESIGNATED_HOST_ALLOCATOR_STRATEGY_TYPE);
                msg.setVmInstance(VmInstanceInventory.valueOf(self));
                if (self.getImageUuid() != null && dbf.isExist(self.getImageUuid(), ImageVO.class)) {
                    msg.setImage(ImageInventory.valueOf(dbf.findByUuid(self.getImageUuid(), ImageVO.class)));
                }
                msg.setHostUuid(self.getHostUuid());
                msg.setFullAllocate(false);
                msg.setL3NetworkUuids(VmNicHelper.getL3Uuids(VmNicInventory.valueOf(self.getVmNics())));
                msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
                bus.send(msg, new CloudBusCallBack(chain) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            ErrorCode err = operr("host[uuid:%s] capacity is not enough to offer cpu[%s], memory[%s bytes]",
                                    self.getHostUuid(), cpuNum - oldCpuNum, struct.alignedMemory - oldMemorySize);
                            err.setCause(reply.getError());
                            chain.fail(err);
                        } else {
                            result = true;
                            logger.debug(String.format("reserve memory %s bytes and cpu %s on host[uuid:%s]", memorySize - self.getMemorySize(), cpuNum - self.getCpuNum(), self.getHostUuid()));
                            chain.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback chain, Map data) {
                if (result) {
                    ReturnHostCapacityMsg msg = new ReturnHostCapacityMsg();
                    msg.setCpuCapacity(cpuNum - oldCpuNum);
                    msg.setMemoryCapacity(struct.alignedMemory - oldMemorySize);
                    msg.setHostUuid(self.getHostUuid());
                    msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
                    bus.send(msg);
                }

                chain.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("change-cpu-of-vm-%s", self.getUuid());

            @Override
            public void run(FlowTrigger chain, Map data) {
                if (cpuNum != self.getCpuNum()) {
                    IncreaseVmCpuMsg msg = new IncreaseVmCpuMsg();
                    msg.setVmInstanceUuid(self.getUuid());
                    msg.setHostUuid(self.getHostUuid());
                    msg.setCpuNum(cpuNum);
                    bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, self.getHostUuid());
                    bus.send(msg, new CloudBusCallBack(chain) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.error("failed to update cpu");
                                chain.fail(reply.getError());
                            } else {
                                IncreaseVmCpuReply r = reply.castReply();
                                self.setCpuNum(r.getCpuNum());
                                chain.next();
                            }
                        }
                    });
                } else {
                    chain.next();
                }
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("change-memory-of-vm-%s", self.getUuid());

            @Override
            public void run(FlowTrigger chain, Map data) {
                if (struct.alignedMemory != self.getMemorySize()) {
                    IncreaseVmMemoryMsg msg = new IncreaseVmMemoryMsg();
                    msg.setVmInstanceUuid(self.getUuid());
                    msg.setHostUuid(self.getHostUuid());
                    msg.setMemorySize(struct.alignedMemory);
                    bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, self.getHostUuid());
                    bus.send(msg, new CloudBusCallBack(chain) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.error("failed to update memory");
                                chain.fail(reply.getError());
                            } else {
                                IncreaseVmMemoryReply r = reply.castReply();
                                self.setMemorySize(r.getMemorySize());
                                chain.next();
                            }
                        }
                    });
                } else {
                    chain.next();
                }
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                dbf.update(self);
                VmCanonicalEvents.VmConfigChangedData d = new VmCanonicalEvents.VmConfigChangedData();
                d.setVmUuid(self.getUuid());
                d.setInv(getSelfInventory());
                d.setAccoundUuid(acntMgr.getOwnerAccountUuidOfResource(self.getUuid()));
                evtf.fire(VmCanonicalEvents.VM_CONFIG_CHANGED_PATH, d);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void doVmInstanceUpdate(final APIUpdateVmInstanceMsg msg, Completion completion) {
        refreshVO();

        List<Runnable> extensions = new ArrayList<Runnable>();
        final VmInstanceInventory vm = getSelfInventory();
        VmInstanceSpec spec = new VmInstanceSpec();
        spec.setVmInventory(vm);
        spec.setCurrentVmOperation(VmOperation.Update);

        FlowChain chain = new SimpleFlowChain();
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
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
                    if (!msg.getPlatform().equals(vm.getPlatform())) {
                        extensions.add(new Runnable() {
                            @Override
                            public void run() {
                                for (VmPlatformChangedExtensionPoint ext :
                                        pluginRgty.getExtensionList(VmPlatformChangedExtensionPoint.class)) {
                                    ext.vmPlatformChange(vm, vm.getPlatform(), msg.getPlatform());
                                }
                            }
                        });
                    }
                }

                if (update) {
                    dbf.update(self);
                }

                updateVmIsoFirstOrder(msg.getSystemTags());

                CollectionUtils.safeForEach(extensions, Runnable::run);

                if (msg.getCpuNum() != null || msg.getMemorySize() != null) {
                    int cpuNum = msg.getCpuNum() == null ? self.getCpuNum() : msg.getCpuNum();
                    long memory = msg.getMemorySize() == null ? self.getMemorySize() : msg.getMemorySize();
                    changeCpuAndMemory(cpuNum, memory, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                } else {
                    trigger.next();
                }
            }
        });

        setAdditionalFlow(chain, spec);

        chain.error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).start();
    }

    private void handle(final APIUpdateVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateVmInstanceEvent evt = new APIUpdateVmInstanceEvent(msg.getId());

                doVmInstanceUpdate(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        refreshVO();
                        evt.setInventory(getSelfInventory());
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "update-vm-info";
            }
        });
    }

    // Specify an iso as the first one, restart vm effective
    private void updateVmIsoFirstOrder(List<String> systemTags){
        if(systemTags == null || systemTags.isEmpty()){
            return;
        }

        String isoUuid = SystemTagUtils.findTagValue(systemTags, VmSystemTags.ISO, VmSystemTags.ISO_TOKEN);
        if (isoUuid == null){
            return;
        }

        String vmUuid = self.getUuid();
        List<String> isoList = IsoOperator.getIsoUuidByVmUuid(vmUuid);
        if (!isoList.contains(isoUuid)) {
            throw new OperationFailureException(operr("ISO[uuid:%s] is not attached to VM[uuid:%s]", isoUuid , self.getUuid()));
        }

        List<VmCdRomVO> cdRomVOS = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, self.getUuid())
                .orderBy(VmCdRomVO_.deviceId, SimpleQuery.Od.ASC)
                .list();
        if (cdRomVOS.size() <= 1) {
            return;
        }

        if (isoUuid.equals(cdRomVOS.get(0).getIsoUuid())) {
            return;
        }

        Optional<VmCdRomVO> opt = cdRomVOS.stream().filter(v -> v.getIsoUuid().equals(isoUuid)).findAny();
        if (!opt.isPresent()) {
            return;
        }

        VmCdRomVO sourceCdRomVO = opt.get();
        VmCdRomVO targetCdRomVO = cdRomVOS.get(0);
        String targetCdRomIsoUuid = targetCdRomVO.getIsoUuid();
        String path = targetCdRomVO.getIsoInstallPath();
        targetCdRomVO.setIsoUuid(sourceCdRomVO.getIsoUuid());
        targetCdRomVO.setIsoInstallPath(sourceCdRomVO.getIsoInstallPath());
        sourceCdRomVO.setIsoUuid(targetCdRomIsoUuid);
        sourceCdRomVO.setIsoInstallPath(path);

        new SQLBatch() {
            @Override
            protected void scripts() {
                merge(targetCdRomVO);
                merge(sourceCdRomVO);
            }
        }.execute();
    }

    @Transactional(readOnly = true)
    private List<VolumeVO> getAttachableVolume(String accountUuid) {
        if (!self.getState().equals(VmInstanceState.Stopped) && self.getPlatform().equals(ImagePlatform.Other.toString())) {
            return Collections.emptyList();
        }
        List<String> volUuids = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, VolumeVO.class);
        if (volUuids != null && volUuids.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> formats = VolumeFormat.getVolumeFormatSupportedByHypervisorTypeInString(self.getHypervisorType());
        if (formats.isEmpty()) {
            throw new CloudRuntimeException(String.format("cannot find volume formats for the hypervisor type[%s]",
                    self.getHypervisorType()));
        }

        String sql;
        List<VolumeVO> vos;

        /*
         * Cluster1: [PS1, PS2, PS3]
         * Cluster2: [PS1, PS2]
         * Cluster3: [PS1, PS2, PS3]
         *
         * Assume a stopped vm which has no clusterUuid and root volume on PS1
         * then it can attach all suitable data volumes from [PS1, PS2]
         * because PS1 is attached to [Cluster1, Cluster2, Cluster3]
         * and they all have [PS1, PS2] attached
         */
        List<String> psUuids = null;
        if (self.getClusterUuid() == null) {
            // 1. get clusterUuids of VM->RV->PS
            sql = "select cls.uuid from" +
                    " ClusterVO cls, VolumeVO vol, VmInstanceVO vm, PrimaryStorageClusterRefVO ref" +
                    " where vm.uuid = :vmUuid" +
                    " and vol.uuid = vm.rootVolumeUuid" +
                    " and ref.primaryStorageUuid = vol.primaryStorageUuid" +
                    " and cls.uuid = ref.clusterUuid" +
                    " and cls.state = :clsState" +
                    " group by cls.uuid";
            List<String> clusterUuids = SQL.New(sql)
                    .param("vmUuid", self.getUuid())
                    .param("clsState", ClusterState.Enabled)
                    .list();

            // 2. get all PS that attachs to clusterUuids
            sql = "select ps.uuid from PrimaryStorageVO ps" +
                    " inner join PrimaryStorageClusterRefVO ref on ref.primaryStorageUuid = ps.uuid" +
                    " inner join ClusterVO cls on cls.uuid = ref.clusterUuid" +
                    " where cls.uuid in (:clusterUuids)" +
                    " and ps.state = :psState" +
                    " and ps.status = :psStatus" +
                    " group by ps.uuid" +
                    " having count(distinct cls.uuid) = :clsCount";
            psUuids = SQL.New(sql)
                    .param("clusterUuids", clusterUuids)
                    .param("psState", PrimaryStorageState.Enabled)
                    .param("psStatus", PrimaryStorageStatus.Connected)
                    .param("clsCount", (long)clusterUuids.size())
                    .list();
        }

        if (volUuids == null) {                             // accessed by a system admin
            // if vm.clusterUuid is not null
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

            // if vm.clusterUuid is null
            if (self.getClusterUuid() == null) {
                // 3. get data volume candidates from psUuids
                sql = "select vol from VolumeVO vol" +
                        " where vol.primaryStorageUuid in (:psUuids)" +
                        " and vol.type = :volType" +
                        " and vol.state = :volState" +
                        " and vol.status = :volStatus" +
                        " and vol.format in (:formats)" +
                        " and vol.vmInstanceUuid is null" +
                        " group by vol.uuid";
                List<VolumeVO> dvs = SQL.New(sql)
                        .param("psUuids", psUuids)
                        .param("volType", VolumeType.Data)
                        .param("volState", VolumeState.Enabled)
                        .param("volStatus", VolumeStatus.Ready)
                        .param("formats", formats)
                        .list();
                vos.addAll(dvs);
            }

            // for NotInstantiated data volumes
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
        } else {                                            // accessed by a normal account
            // if vm.clusterUuid is not null
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

            // if vm.clusterUuid is null
            if (self.getClusterUuid() == null) {
                // 3. get data volume candidates from psUuids
                sql = "select vol from VolumeVO vol" +
                        " where vol.primaryStorageUuid in (:psUuids)" +
                        " and vol.type = :volType" +
                        " and vol.state = :volState" +
                        " and vol.status = :volStatus" +
                        " and vol.format in (:formats)" +
                        " and vol.vmInstanceUuid is null" +
                        " and vol.uuid in (:volUuids)" +
                        " group by vol.uuid";
                List<VolumeVO> dvs = SQL.New(sql)
                        .param("psUuids", psUuids)
                        .param("volType", VolumeType.Data)
                        .param("volState", VolumeState.Enabled)
                        .param("volStatus", VolumeStatus.Ready)
                        .param("formats", formats)
                        .param("volUuids", volUuids)
                        .list();
                vos.addAll(dvs);
            }

            // for NotInstantiated data volumes
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
        attachNicInQueue(msg, msg.getL3NetworkUuid(), msg.isApplyToBackend(), new ReturnValueCompletion<VmNicInventory>(msg) {
            @Override
            public void success(VmNicInventory returnValue) {
                self = dbf.reload(self);
                evt.setInventory(VmInstanceInventory.valueOf(self));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(final APIAttachVmNicToVmMsg msg) {
        final APIAttachVmNicToVmEvent evt = new APIAttachVmNicToVmEvent(msg.getId());
        final String vmNicInvKey = "vmNicInventory";

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("attach-nic-to-vm-%s", msg.getVmInstanceUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "attach-nic";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                attachNic(msg, new ReturnValueCompletion<VmNicInventory>(msg) {
                    @Override
                    public void success(VmNicInventory returnValue) {
                        data.put(vmNicInvKey, returnValue);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "after-attach-nic";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                afterAttachNic((VmNicInventory) data.get(vmNicInvKey), new Completion(trigger) {
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
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                self = dbf.reload(self);
                evt.setInventory(VmInstanceInventory.valueOf(self));
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).start();
    }

    protected void afterAttachNic(VmNicInventory nicInventory, boolean applyToBackend, Completion completion) {
        completion.success();
    }

    protected void afterAttachNic(VmNicInventory nicInventory, Completion completion) {
        afterAttachNic(nicInventory, true, completion);
    }

    protected void afterDetachNic(VmNicInventory nicInventory, boolean isRollback, Completion completion) {
        completion.success();
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
        VolumeVO vvo = dbf.findByUuid(volume.getUuid(), VolumeVO.class);
        // the volume is already detached, skip the bellow actions
        if (!vvo.getAttachedVmUuids().contains(self.getUuid())) {
            extEmitter.afterDetachVolume(getSelfInventory(), volume, new Completion(completion) {
                @Override
                public void success() {
                    bus.reply(msg, reply);
                    completion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                    completion.done();
                }
            });

            return;
        }

        extEmitter.preDetachVolume(getSelfInventory(), volume);
        extEmitter.beforeDetachVolume(getSelfInventory(), volume);

        if (self.getState() == VmInstanceState.Stopped) {
            extEmitter.afterDetachVolume(getSelfInventory(), volume, new Completion(completion) {
                @Override
                public void success() {
                    bus.reply(msg, reply);
                    completion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                    completion.done();
                }
            });
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
                    bus.reply(msg, reply);
                    completion.done();
                } else {
                    extEmitter.afterDetachVolume(getSelfInventory(), volume, new Completion(completion) {
                        @Override
                        public void success() {
                            bus.reply(msg, reply);
                            completion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            reply.setError(errorCode);
                            bus.reply(msg, reply);
                            completion.done();
                        }
                    });
                }
            }
        });
    }

    protected void attachDataVolume(final AttachDataVolumeToVmMsg msg, final NoErrorCompletion completion) {
        final AttachDataVolumeToVmReply reply = new AttachDataVolumeToVmReply();
        refreshVO();
        ErrorCode err = validateOperationByState(msg, self.getState(), VmErrors.ATTACH_VOLUME_ERROR);
        if (err != null) {
            throw new OperationFailureException(err);
        }

        Map data = new HashMap();

        final VolumeInventory volume = msg.getVolume();

        new VmAttachVolumeValidator().validate(msg.getVmInstanceUuid(), volume.getUuid());
        extEmitter.preAttachVolume(getSelfInventory(), volume);
        extEmitter.beforeAttachVolume(getSelfInventory(), volume, data);

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

        List<VolumeInventory> attachedVolumes = getAllDataVolumes(getSelfInventory());
        attachedVolumes.removeIf(it -> it.getDeviceId() == null || it.getUuid().equals(volume.getUuid()));

        chain.setName(String.format("vm-%s-attach-volume-%s", self.getUuid(), volume.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.getData().put(VmInstanceConstant.Params.AttachingVolumeInventory.toString(), volume);
        chain.getData().put(Params.AttachedDataVolumeInventories.toString(), attachedVolumes);
        chain.done(new FlowDoneHandler(msg, completion) {
            @Override
            public void handle(Map data) {
                extEmitter.afterAttachVolume(getSelfInventory(), volume);
                reply.setHypervisorType(self.getHypervisorType());
                sendEvent();
                bus.reply(msg, reply);
                completion.done();
            }

            private void sendEvent() {
                VolumeCanonicalEvents.VolumeAttachedData data = new VolumeCanonicalEvents.VolumeAttachedData();
                data.setVolumeUuid(volume.getUuid());
                data.setInventory(volume);
                data.setVmInventory(getSelfInventory());
                evtf.fire(VolumeCanonicalEvents.VOLUME_ATTACHED_VM_PATH, data);
            }
        }).error(new FlowErrorHandler(msg, completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                extEmitter.failedToAttachVolume(getSelfInventory(), volume, errCode, data);
                reply.setError(err(VmErrors.ATTACH_VOLUME_ERROR, errCode, errCode.getDetails()));
                bus.reply(msg, reply);
                completion.done();
            }
        }).start();

    }

    protected void migrateVm(final MigrateVmMessage msg, final Completion completion) {
        refreshVO();
        ErrorCode allowed = validateOperationByState((Message) msg, self.getState(), VmErrors.MIGRATE_ERROR);
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
        spec.setMessage((Message) msg);
        spec.setAllocationScene(msg.getAllocationScene());
        FlowChain chain = getMigrateVmWorkFlowChain(inv);

        setFlowMarshaller(chain);

        String lastHostUuid = self.getHostUuid();
        chain.setName(String.format("migrate-vm-%s", self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(final Map data) {
                VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                HostInventory host = spec.getDestHost();
                self = changeVmStateInDb(VmInstanceStateEvent.running, ()-> {
                    self.setZoneUuid(host.getZoneUuid());
                    self.setClusterUuid(host.getClusterUuid());
                    self.setLastHostUuid(lastHostUuid);
                    self.setHostUuid(host.getUuid());
                });
                VmInstanceInventory vm = VmInstanceInventory.valueOf(self);
                extEmitter.afterMigrateVm(vm, vm.getLastHostUuid());
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                String destHostUuid = spec.getDestHost().getUuid().equals(lastHostUuid) ? null : spec.getDestHost().getUuid();
                extEmitter.failedToMigrateVm(VmInstanceInventory.valueOf(self), destHostUuid, errCode);
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

    protected void handle(CancelMigrateVmMsg msg) {
        CancelMigrateVmReply reply = new CancelMigrateVmReply();

        CancelHostTasksMsg cmsg = new CancelHostTasksMsg();
        cmsg.setCancellationApiId(msg.getCancellationApiId());
        bus.makeLocalServiceId(cmsg, HostConstant.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                }
                bus.reply(msg, reply);
            }
        });
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
                reportProgress("0");
                migrateVm(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        APIMigrateVmEvent evt = new APIMigrateVmEvent(msg.getId());
                        evt.setInventory(VmInstanceInventory.valueOf(self));
                        reportProgress("100");
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        APIMigrateVmEvent evt = new APIMigrateVmEvent(msg.getId());
                        evt.setError(errorCode);
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

        if (self.getState() == VmInstanceState.Running) {
            completion.success();
            return;
        }

        if (self.getState() == VmInstanceState.Created) {
            InstantiateVmFromNewCreatedStruct struct = new JsonLabel().get(
                    InstantiateVmFromNewCreatedStruct.makeLabelKey(self.getUuid()), InstantiateVmFromNewCreatedStruct.class);

            if (msg instanceof StartVmInstanceMsg && ((StartVmInstanceMsg) msg).isStartPaused()) {
                struct.setStrategy(VmCreationStrategy.CreatedPaused);
            } else {
                struct.setStrategy(VmCreationStrategy.InstantStart);
            }

            instantiateVmFromNewCreate(struct, completion);
            return;
        }

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
            spec.setUsbRedirect(VmSystemTags.USB_REDIRECT.getTokenByResourceUuid(self.getUuid(), VmSystemTags.USB_REDIRECT_TOKEN));
            spec.setEnableRDP(VmSystemTags.RDP_ENABLE.getTokenByResourceUuid(self.getUuid(), VmSystemTags.RDP_ENABLE_TOKEN));
            spec.setVDIMonitorNumber(VmSystemTags.VDI_MONITOR_NUMBER.getTokenByResourceUuid(self.getUuid(), VmSystemTags.VDI_MONITOR_NUMBER_TOKEN));
        }

        if (msg instanceof HaStartVmInstanceMsg) {
            spec.setSoftAvoidHostUuids(((HaStartVmInstanceMsg) msg).getSoftAvoidHostUuids());
            spec.setAllocationScene(AllocationScene.Auto);
        } else if (msg instanceof StartVmInstanceMsg) {
            spec.setSoftAvoidHostUuids(((StartVmInstanceMsg) msg).getSoftAvoidHostUuids());
            if (((StartVmInstanceMsg) msg).getAllocationScene() != null) {
                spec.setAllocationScene(((StartVmInstanceMsg) msg).getAllocationScene());
            }
            spec.setAvoidHostUuids(((StartVmInstanceMsg) msg).getAvoidHostUuids());
            spec.setCreatePaused(((StartVmInstanceMsg) msg).isStartPaused());
        } else if (msg instanceof RestoreVmInstanceMsg) {
            spec.setMemorySnapshotUuid(((RestoreVmInstanceMsg) msg).getMemorySnapshotUuid());
        }

        if (spec.getDestNics().isEmpty()) {
            throw new OperationFailureException(operr("unable to start the vm[uuid:%s]." +
                            " It doesn't have any nic, please attach a nic and try again", self.getUuid()));
        }

        final VmInstanceState originState = self.getState();
        changeVmStateInDb(VmInstanceStateEvent.starting);

        logger.debug("we keep vm state on 'Starting' until startVm over or restart mn.");

        extEmitter.beforeStartVm(VmInstanceInventory.valueOf(self));

        FlowChain chain = new SimpleFlowChain();
        setFlowBeforeFormalWorkFlow(chain, spec);
        chain.getFlows().addAll(getStartVmWorkFlowChain(inv).getFlows());
        setFlowMarshaller(chain);
        setAdditionalFlow(chain, spec);

        String recentHostUuid = self.getHostUuid() == null ? self.getLastHostUuid() : self.getHostUuid();
        String vmHostUuid = self.getHostUuid();
        String vmLastHostUuid = self.getLastHostUuid();
        chain.setName(String.format("start-vm-%s", self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(final Map data) {
                VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                self = changeVmStateInDb(VmInstanceStateEvent.running, ()-> new SQLBatch() {
                    @Override
                    protected void scripts() {
                        // reload self because some nics may have been deleted in start phase because a former L3Network deletion.
                        // reload to avoid JPA EntityNotFoundException
                        self = findByUuid(self.getUuid(), VmInstanceVO.class);
                        if (q(HostVO.class).eq(HostVO_.uuid, recentHostUuid).isExists()) {
                            self.setLastHostUuid(recentHostUuid);
                        } else {
                            self.setLastHostUuid(null);
                        }
                        self.setHostUuid(spec.getDestHost().getUuid());
                        self.setClusterUuid(spec.getDestHost().getClusterUuid());
                        self.setZoneUuid(spec.getDestHost().getZoneUuid());
                    }
                }.execute());
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
                VmInstanceSpec spec = (VmInstanceSpec) data.get(Params.VmInstanceSpec.toString());

                // update vm state to origin state before checking state
                // avoid sending redundant vm state change event
                // refer to: ZSTAC-18174
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        self.setState(originState);
                        self.setHostUuid(vmHostUuid);
                        self.setLastHostUuid(q(HostVO.class).eq(HostVO_.uuid, vmLastHostUuid).isExists() ? vmLastHostUuid : null);
                        self = merge(self);
                    }
                }.execute();

                if (HostErrors.FAILED_TO_START_VM_ON_HYPERVISOR.isEqual(errCode.getCode())) {
                    checkState(spec.getDestHost().getUuid(), new NoErrorCompletion(completion) {
                        @Override
                        public void done() {
                            completion.fail(errCode);
                        }
                    });
                    return;
                }

                completion.fail(errCode);
            }
        }).start();
    }

    private VmInstanceSpec buildVmInstanceSpecFromStruct(InstantiateVmFromNewCreatedStruct struct) {
        final VmInstanceSpec spec = new VmInstanceSpec();
        spec.setRequiredPrimaryStorageUuidForRootVolume(struct.getPrimaryStorageUuidForRootVolume());
        spec.setRequiredPrimaryStorageUuidForDataVolume(struct.getPrimaryStorageUuidForDataVolume());
        spec.setDataVolumeSystemTags(struct.getDataVolumeSystemTags());
        spec.setRootVolumeSystemTags(struct.getRootVolumeSystemTags());
        spec.setRequiredHostUuid(struct.getRequiredHostUuid());

        spec.setVmInventory(getSelfInventory());
        if (struct.getL3NetworkUuids() != null && !struct.getL3NetworkUuids().isEmpty()) {
            SimpleQuery<L3NetworkVO> nwquery = dbf.createQuery(L3NetworkVO.class);
            nwquery.add(L3NetworkVO_.uuid, Op.IN, VmNicSpec.getL3UuidsOfSpec(struct.getL3NetworkUuids()));
            List<L3NetworkVO> vos = nwquery.list();
            List<L3NetworkInventory> nws = L3NetworkInventory.valueOf(vos);

            // order L3 networks by the order they specified in the API
            List<VmNicSpec> nicSpecs = new ArrayList<>();
            for (VmNicSpec nicSpec : struct.getL3NetworkUuids()) {
                List<L3NetworkInventory> l3s = new ArrayList<>();
                for (L3NetworkInventory inv : nicSpec.l3Invs) {
                    L3NetworkInventory l3 = CollectionUtils.find(nws, new Function<L3NetworkInventory, L3NetworkInventory>() {
                        @Override
                        public L3NetworkInventory call(L3NetworkInventory arg) {
                            return arg.getUuid().equals(inv.getUuid()) ? arg : null;
                        }
                    });

                    if (l3 == null) {
                        throw new OperationFailureException(operr(
                                "Unable to find L3Network[uuid:%s] to start the current vm, it may have been deleted, " +
                                        "Operation suggestion: delete this vm, recreate a new vm", inv.getUuid()));
                    }
                    l3s.add(l3);
                }
                if (!l3s.isEmpty()) {
                    VmNicSpec nicSpec1 = new VmNicSpec(l3s);
                    nicSpec1.setNicDriverType(nicSpec.getNicDriverType());
                    nicSpecs.add(nicSpec1);
                }
            }

            spec.setL3Networks(nicSpecs);
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
                if (dvo != null) {
                    disks.add(DiskOfferingInventory.valueOf(dvo));
                }
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
        List<CdRomSpec> cdRomSpecs = buildVmCdRomSpecsForNewCreated(spec);
        spec.setCdRomSpecs(cdRomSpecs);

        spec.getImageSpec().setInventory(ImageInventory.valueOf(imvo));
        spec.setCurrentVmOperation(VmOperation.NewCreate);
        if (struct.getRequiredHostUuid() != null) {
            spec.setHostAllocatorStrategy(HostAllocatorConstant.DESIGNATED_HOST_ALLOCATOR_STRATEGY_TYPE);
        }
        buildHostname(spec);

        spec.setUserdataList(buildUserdata());
        selectBootOrder(spec);
        spec.setConsolePassword(VmSystemTags.CONSOLE_PASSWORD.
                getTokenByResourceUuid(self.getUuid(), VmSystemTags.CONSOLE_PASSWORD_TOKEN));
        spec.setUsbRedirect(VmSystemTags.USB_REDIRECT.getTokenByResourceUuid(self.getUuid(), VmSystemTags.USB_REDIRECT_TOKEN));
        if (struct.getStrategy() == VmCreationStrategy.CreateStopped || struct.getStrategy() == VmCreationStrategy.CreatedPaused) {
            spec.setCreatePaused(true);
        }

        if (struct.getSoftAvoidHostUuids() != null && !struct.getSoftAvoidHostUuids().isEmpty()) {
            spec.setSoftAvoidHostUuids(struct.getSoftAvoidHostUuids());
        }

        if (struct.getAvoidHostUuids() != null && !struct.getAvoidHostUuids().isEmpty()) {
            spec.setAvoidHostUuids(struct.getAvoidHostUuids());
        }

        for (BuildVmSpecExtensionPoint ext : pluginRgty.getExtensionList(BuildVmSpecExtensionPoint.class)) {
            ext.afterBuildVmSpec(spec);
        }

        return spec;
    }

    private List<CdRomSpec> buildVmCdRomSpecsForNewCreated(VmInstanceSpec vmSpec) {
        List<VmInstanceSpec.CdRomSpec> cdRomSpecs = new ArrayList<>();

        VmInstanceInventory vmInventory = vmSpec.getVmInventory();
        String vmUuid = vmInventory.getUuid();

        // vm image is iso
        ImageVO imvo = dbf.findByUuid(vmInventory.getImageUuid(), ImageVO.class);
        if (imvo.getMediaType() == ImageMediaType.ISO) {
            CdRomSpec cdRomSpec = new CdRomSpec();
            cdRomSpec.setDeviceId(cdRomSpecs.size());
            cdRomSpec.setImageUuid(imvo.getUuid());
            cdRomSpecs.add(cdRomSpec);
        }

        // createWithoutCdRom
        boolean hasTag = VmSystemTags.CREATE_WITHOUT_CD_ROM.hasTag(vmUuid);
        boolean flagWithoutCdRom = false;
        if (hasTag) {
            String withoutCdRom = VmSystemTags.CREATE_WITHOUT_CD_ROM.getTokenByResourceUuid(vmUuid, VmSystemTags.CREATE_WITHOUT_CD_ROM_TOKEN);
            flagWithoutCdRom = Boolean.parseBoolean(withoutCdRom);
        }
        if (flagWithoutCdRom) {
            return cdRomSpecs;
        }

        // cdroms
        hasTag = VmSystemTags.CREATE_VM_CD_ROM_LIST.hasTag(vmUuid);
        if (hasTag) {
            Map<String, String> tokens = VmSystemTags.CREATE_VM_CD_ROM_LIST.getTokensByResourceUuid(vmUuid);
            List<String> cdRoms = new ArrayList<>();
            cdRoms.add(tokens.get(VmSystemTags.CD_ROM_0));
            cdRoms.add(tokens.get(VmSystemTags.CD_ROM_1));
            cdRoms.add(tokens.get(VmSystemTags.CD_ROM_2));
            // remove vm image iso, image iso has been added
            cdRoms.removeAll(cdRomSpecs.stream().map(CdRomSpec::getImageUuid).collect(Collectors.toList()));

            for (String cdRom : cdRoms) {
                if (cdRom == null || VmInstanceConstant.NONE_CDROM.equalsIgnoreCase(cdRom)) {
                    continue;
                }

                CdRomSpec cdRomSpec = new CdRomSpec();
                cdRomSpec.setDeviceId(cdRomSpecs.size());
                String imageUuid = VmInstanceConstant.EMPTY_CDROM.equalsIgnoreCase(cdRom) ? null : cdRom;
                cdRomSpec.setImageUuid(imageUuid);
                cdRomSpecs.add(cdRomSpec);
            }
        } else {
            int defaultCdRomNum = VmGlobalConfig.VM_DEFAULT_CD_ROM_NUM.value(Integer.class);

            while (defaultCdRomNum > cdRomSpecs.size()) {
                CdRomSpec cdRomSpec = new CdRomSpec();
                cdRomSpec.setDeviceId(cdRomSpecs.size());
                cdRomSpecs.add(cdRomSpec);
            }
        }

        int max = VmGlobalConfig.MAXIMUM_CD_ROM_NUM.value(Integer.class);
        if (cdRomSpecs.size() > max) {
            throw new OperationFailureException(operr("One vm cannot create %s CDROMs, vm can only add %s CDROMs", cdRomSpecs.size(), max));
        }

        return cdRomSpecs;
    }

    protected void instantiateVmFromNewCreate(InstantiateVmFromNewCreatedStruct struct, Completion completion) {
        VmInstanceSpec spec = buildVmInstanceSpecFromStruct(struct);

        changeVmStateInDb(VmInstanceStateEvent.starting);

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(BeforeStartNewCreatedVmExtensionPoint.class),
                new ForEachFunction<BeforeStartNewCreatedVmExtensionPoint>() {
                    @Override
                    public void run(BeforeStartNewCreatedVmExtensionPoint ext) {
                        ext.beforeStartNewCreatedVm(spec);
                    }
                });

        extEmitter.beforeStartNewCreatedVm(VmInstanceInventory.valueOf(self));
        FlowChain chain = getCreateVmWorkFlowChain(getSelfInventory());
        setFlowMarshaller(chain);

        chain.setName(String.format("create-vm-%s", self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.then(new NoRollbackFlow() {
            String __name__ = "after-started-vm-" + self.getUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                changeVmStateInDb(struct.getStrategy() == VmCreationStrategy.InstantStart ?
                        VmInstanceStateEvent.running : VmInstanceStateEvent.paused, ()-> {
                    self.setLastHostUuid(spec.getDestHost().getUuid());
                    self.setHostUuid(spec.getDestHost().getUuid());
                    self.setClusterUuid(spec.getDestHost().getClusterUuid());
                    self.setZoneUuid(spec.getDestHost().getZoneUuid());
                    self.setHypervisorType(spec.getDestHost().getHypervisorType());
                    self.setRootVolumeUuid(spec.getDestRootVolume().getUuid());
                });
                logger.debug(String.format("vm[uuid:%s] is started ..", self.getUuid()));
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                extEmitter.afterStartNewCreatedVm(inv);
                trigger.next();
            }
        });

        if (struct.getStrategy() == VmCreationStrategy.CreateStopped) {
            chain.then(new NoRollbackFlow() {
                String __name__ = "stop-vm-" + self.getUuid();

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    StopVmInstanceMsg smsg = new StopVmInstanceMsg();
                    smsg.setVmInstanceUuid(self.getUuid());
                    smsg.setGcOnFailure(true);
                    smsg.setType(StopVmType.cold.toString());
                    stopVm(smsg, new Completion(trigger) {
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

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(final Map data) {
                logger.debug(String.format("vm[uuid:%s] is created ..", self.getUuid()));
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                extEmitter.failedToStartNewCreatedVm(VmInstanceInventory.valueOf(self), errCode);
                dbf.remove(self);
                // clean up EO, otherwise API-retry may cause conflict if
                // the resource uuid is set
                try {
                    dbf.eoCleanup(VmInstanceVO.class, self.getUuid());
                } catch (Exception e) {
                    logger.warn(e.getMessage());
                }

                completion.fail(operr(errCode, errCode.getDetails()));
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
                reply.setError(err(VmErrors.START_ERROR, errorCode, errorCode.getDetails()));
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
                evt.setError(err(VmErrors.START_ERROR, errorCode, errorCode.getDetails()));
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
        final APIDestroyVmInstanceEvent evt = new APIDestroyVmInstanceEvent(msg.getId());
        destroyVm(msg, new Completion(msg) {
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
                completion.fail(err(SysErrors.DELETE_RESOURCE_ERROR, errCode, errCode.getDetails()));
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

        spec.setUserdataList(buildUserdata());

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
        List<VmNicSpec> nicSpecs = new ArrayList<>();
        for (VmNicInventory nic : inv.getVmNics()) {
            List<L3NetworkInventory> l3Invs = new ArrayList<>();
            /* if destroy vm, then recover vm, ip address of nic has been deleted */
            if (nic.getUsedIps() != null && !nic.getUsedIps().isEmpty()) {
                for (UsedIpInventory ip : nic.getUsedIps()) {
                    L3NetworkVO l3Vo = dbf.findByUuid(ip.getL3NetworkUuid(), L3NetworkVO.class);
                    if (l3Vo != null) {
                        l3Invs.add(L3NetworkInventory.valueOf(l3Vo));
                    }
                }
            }

            if (l3Invs.isEmpty()) {
                L3NetworkVO l3Vo = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
                if (l3Vo != null) {
                    l3Invs.add(L3NetworkInventory.valueOf(l3Vo));
                }
            }
            nicSpecs.add(new VmNicSpec(l3Invs));
        }
        spec.setL3Networks(nicSpecs);

        String huuid = inv.getHostUuid() == null ? inv.getLastHostUuid() : inv.getHostUuid();
        if (huuid != null) {
            HostVO hvo = dbf.findByUuid(huuid, HostVO.class);
            if (hvo != null) {
                spec.setDestHost(HostInventory.valueOf(hvo));
            }
        }

        VolumeInventory rootVol = inv.getRootVolume();
        Optional.ofNullable(rootVol).ifPresent(it -> {
            spec.setDestRootVolume(it);
            spec.setRequiredPrimaryStorageUuidForRootVolume(it.getPrimaryStorageUuid());
        });
        spec.setDestDataVolumes(getAllDataVolumes(inv));

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

        List<VmCdRomVO> cdRomVOS = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, inv.getUuid())
                .orderBy(VmCdRomVO_.deviceId, SimpleQuery.Od.ASC)
                .list();
        for (VmCdRomVO cdRomVO : cdRomVOS) {
            CdRomSpec cdRomSpec = new CdRomSpec();
            cdRomSpec.setUuid(cdRomVO.getUuid());

            String isoUuid = cdRomVO.getIsoUuid();
            if (isoUuid != null) {
                if(dbf.isExist(isoUuid, ImageVO.class)) {
                    cdRomSpec.setImageUuid(isoUuid);
                    cdRomSpec.setInstallPath(cdRomVO.getIsoInstallPath());
                } else {
                    //TODO
                    logger.warn(String.format("iso[uuid:%s] is deleted, however, the VM[uuid:%s] still has it attached",
                            isoUuid, self.getUuid()));
                }
            }

            cdRomSpec.setDeviceId(cdRomVO.getDeviceId());
            spec.getCdRomSpecs().add(cdRomSpec);
        }

        spec.setCurrentVmOperation(operation);
        selectBootOrder(spec);
        spec.setConsolePassword(VmSystemTags.CONSOLE_PASSWORD.
                getTokenByResourceUuid(self.getUuid(), VmSystemTags.CONSOLE_PASSWORD_TOKEN));
        spec.setVDIMonitorNumber(VmSystemTags.VDI_MONITOR_NUMBER.getTokenByResourceUuid(self.getUuid(), VmSystemTags.VDI_MONITOR_NUMBER_TOKEN));

        for (BuildVmSpecExtensionPoint ext : pluginRgty.getExtensionList(BuildVmSpecExtensionPoint.class)) {
            ext.afterBuildVmSpec(spec);
        }

        return spec;
    }

    private List<VolumeInventory> getAllDataVolumes(VmInstanceInventory inv) {
        List<VolumeInventory> dataVols = inv.getAllVolumes().stream()
                .filter(it -> it.getType().equals(VolumeType.Data.toString()) && !it.isShareable())
                .collect(Collectors.toList());

        List<BuildVolumeSpecExtensionPoint> exts = pluginRgty.getExtensionList(BuildVolumeSpecExtensionPoint.class);
        exts.forEach(e -> dataVols.addAll(e.supplyAdditionalVolumesForVmInstance(inv.getUuid())));
        return dataVols;
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
        FlowChain chain = new SimpleFlowChain();
        setFlowBeforeFormalWorkFlow(chain, spec);
        chain.getFlows().addAll(getRebootVmWorkFlowChain(inv).getFlows());
        setFlowMarshaller(chain);
        setAdditionalFlow(chain, spec);

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
                            self = refreshVO();
                            if ((originState == VmInstanceState.Running || originState == VmInstanceState.Paused) &&
                                    self.getState() == VmInstanceState.Stopped) {
                                returnHostCpacity(spec.getDestHost().getUuid());
                            }
                            completion.fail(errCode);
                        }
                    });
                } else {
                    VmInstanceState currentState = Q.New(VmInstanceVO.class)
                            .select(VmInstanceVO_.state)
                            .eq(VmInstanceVO_.uuid, self.getUuid())
                            .findValue();
                    if (currentState == VmInstanceState.Rebooting) {
                        SQL.New(VmInstanceVO.class)
                                .set(VmInstanceVO_.state, originState)
                                .eq(VmInstanceVO_.uuid, self.getUuid())
                                .update();
                    }

                    completion.fail(errCode);
                }
            }
        }).start();
    }

    protected void returnHostCpacity(String hostUuid) {
        ReturnHostCapacityMsg rmsg = new ReturnHostCapacityMsg();
        rmsg.setCpuCapacity(self.getCpuNum());
        rmsg.setMemoryCapacity(self.getMemorySize());
        rmsg.setHostUuid(hostUuid);
        rmsg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        bus.send(rmsg);
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
                evt.setError(err(VmErrors.REBOOT_ERROR, errorCode, errorCode.getDetails()));
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
                evt.setError(err(VmErrors.STOP_ERROR, errorCode, errorCode.getDetails()));
                bus.publish(evt);
                taskChain.next();
            }
        });
    }

    protected void stopVm(final Message msg, final Completion completion) {
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

        if (msg instanceof ReleaseResourceMessage) {
            spec.setIgnoreResourceReleaseFailure(((ReleaseResourceMessage) msg).isIgnoreResourceReleaseFailure());
        }

        final VmInstanceState originState = self.getState();
        changeVmStateInDb(VmInstanceStateEvent.stopping);

        extEmitter.beforeStopVm(VmInstanceInventory.valueOf(self));

        FlowChain chain;
        if (msg instanceof APIStopVmInstanceMsg) {
            chain = new SimpleFlowChain();
            setFlowBeforeFormalWorkFlow(chain, spec);
            chain.getFlows().addAll(getStopVmWorkFlowChain(inv).getFlows());
            setFlowMarshaller(chain);
        } else {
            chain = getStopVmWorkFlowChain(inv);
        }

        chain.setName(String.format("stop-vm-%s", self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
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
                            self = dbf.reload(self);
                            if (self.getState() == VmInstanceState.Running) {
                                for (DeleteInhibitHASystemTagExtensionPoint ext : pluginRgty.getExtensionList(DeleteInhibitHASystemTagExtensionPoint.class)) {
                                    ext.deleteInhibitHaSystemTag(self.getUuid());
                                }
                            }

                            completion.fail(errCode);
                            extEmitter.failedToStopVm(inv, errCode);
                        }
                    });
                } else if (HostErrors.OPERATION_FAILURE_GC_ELIGIBLE.isEqual(errCode.getCode()) && !spec.isGcOnStopFailure()) {
                    self.setState(originState);
                    self = dbf.updateAndRefresh(self);

                    if (self.getState() == VmInstanceState.Running) {
                        for (DeleteInhibitHASystemTagExtensionPoint ext : pluginRgty.getExtensionList(DeleteInhibitHASystemTagExtensionPoint.class)) {
                            ext.deleteInhibitHaSystemTag(self.getUuid());
                        }
                    }

                    completion.fail(errCode);
                    extEmitter.failedToStopVm(inv, errCode);
                } else {
                    self.setState(HostErrors.HOST_IS_DISCONNECTED.isEqual(errCode.getCode()) ? VmInstanceState.Unknown : originState);
                    self = dbf.updateAndRefresh(self);
                    completion.fail(errCode);
                    extEmitter.failedToStopVm(inv, errCode);
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

    protected void pauseVm(final APIPauseVmInstanceMsg msg, final SyncTaskChain taskChain) {
        pauseVm(msg, new Completion(taskChain) {
            @Override
            public void success() {
                APIPauseVmInstanceEvent evt = new APIPauseVmInstanceEvent(msg.getId());
                VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                evt.setInventory(inv);
                bus.publish(evt);
                taskChain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                APIPauseVmInstanceEvent evt = new APIPauseVmInstanceEvent(msg.getId());
                evt.setError(err(VmErrors.SUSPEND_ERROR, errorCode, errorCode.getDetails()));
                bus.publish(evt);
                taskChain.next();
            }
        });
    }

    protected void pauseVm(final Message msg, Completion completion) {
        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), null);
        if (allowed != null) {
            completion.fail(allowed);
            return;
        }
        if (self.getState() == VmInstanceState.Paused) {
            completion.success();
            return;
        }
        VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
        final VmInstanceSpec spec = buildSpecFromInventory(inv, VmOperation.Pause);
        spec.setMessage(msg);
        final VmInstanceState originState = self.getState();
        changeVmStateInDb(VmInstanceStateEvent.pausing);

        FlowChain chain = getPauseVmWorkFlowChain(inv);
        setFlowMarshaller(chain);

        chain.setName(String.format("pause-vm-%s", self.getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map Data) {
                self = changeVmStateInDb(VmInstanceStateEvent.paused);
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

    protected void handle(final APIPauseVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                pauseVm(msg, chain);
            }

            @Override
            public String getName() {
                return String.format("pause-vm-%s", msg.getVmInstanceUuid());
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
                evt.setError(err(VmErrors.RESUME_ERROR, errorCode, errorCode.getDetails()));
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
        final VmInstanceSpec spec = buildSpecFromInventory(inv, VmOperation.Resume);
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

    private void handle(final APIReimageVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                reimageVmInstance(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "reimage-vminstance";
            }
        });
    }

    private void handle(final APIDeleteVmCdRomMsg msg) {
        APIDeleteVmCdRomEvent event = new APIDeleteVmCdRomEvent(msg.getId());

        DeleteVmCdRomMsg deleteVmCdRomMsg = new DeleteVmCdRomMsg();
        deleteVmCdRomMsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        deleteVmCdRomMsg.setCdRomUuid(msg.getUuid());
        bus.makeLocalServiceId(deleteVmCdRomMsg, VmInstanceConstant.SERVICE_ID);

        bus.send(deleteVmCdRomMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    event.setInventory(VmInstanceInventory.valueOf(self));
                } else {
                    event.setError(reply.getError());
                }

                bus.publish(event);
            }
        });
    }

    private void handle(final DeleteVmCdRomMsg msg) {
        DeleteVmCdRomReply reply = new DeleteVmCdRomReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    reply.setError(allowed);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                deleteVmCdRom(msg.getCdRomUuid(), new Completion(chain) {
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
                return String.format("delete-vm-cdRom-%s", msg.getCdRomUuid());
            }
        });
    }

    private void deleteVmCdRom(String cdRomUuid, Completion completion) {
        boolean exist = dbf.isExist(cdRomUuid, VmCdRomVO.class);
        if (!exist) {
            completion.success();
            return;
        }

        dbf.removeByPrimaryKey(cdRomUuid, VmCdRomVO.class);
        completion.success();
    }

    private void doCreateVmCdRom(CreateVmCdRomMsg msg, ReturnValueCompletion<VmCdRomInventory> completion) {
        long vmCdRomNum = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .count();
        int max = VmGlobalConfig.MAXIMUM_CD_ROM_NUM.value(Integer.class);
        if (max <= vmCdRomNum) {
            completion.fail(operr("VM[uuid:%s] can only add %s CDROMs", msg.getVmInstanceUuid(), max));
            return;
        }

        if (msg.getIsoUuid() != null) {
            boolean targetIsoUsed = Q.New(VmCdRomVO.class)
                    .eq(VmCdRomVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .eq(VmCdRomVO_.isoUuid, msg.getIsoUuid())
                    .isExists();
            if (targetIsoUsed) {
                completion.fail(operr("VM[uuid:%s] already has an ISO[uuid:%s] attached", msg.getVmInstanceUuid(), msg.getIsoUuid()));
                return;
            }
        }

        List<Integer> deviceIds = Q.New(VmCdRomVO.class)
                .select(VmCdRomVO_.deviceId)
                .eq(VmCdRomVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .listValues();
        BitSet full = new BitSet(deviceIds.size() + 1);
        deviceIds.forEach(full::set);
        int targetDeviceId = full.nextClearBit(0);
        if (targetDeviceId >= max) {
            completion.fail(operr("VM[uuid:%s] can only add %s CDROMs", msg.getVmInstanceUuid(), max));
            return;
        }

        VmCdRomVO cdRomVO = new VmCdRomVO();
        String cdRomUuid = msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid();
        cdRomVO.setUuid(cdRomUuid);
        cdRomVO.setDeviceId(targetDeviceId);
        cdRomVO.setIsoUuid(msg.getIsoUuid());
        cdRomVO.setVmInstanceUuid(msg.getVmInstanceUuid());
        cdRomVO.setName(msg.getName());
        String acntUuid = Account.getAccountUuidOfResource(msg.getVmInstanceUuid());
        cdRomVO.setAccountUuid(acntUuid);
        cdRomVO.setDescription(msg.getDescription());
        cdRomVO = dbf.persistAndRefresh(cdRomVO);

        completion.success(VmCdRomInventory.valueOf(cdRomVO));
    }

    private void handle(final APICreateVmCdRomMsg msg) {
        APICreateVmCdRomEvent event = new APICreateVmCdRomEvent(msg.getId());

        CreateVmCdRomMsg cmsg = new CreateVmCdRomMsg();
        cmsg.setResourceUuid(msg.getResourceUuid());
        cmsg.setName(msg.getName());
        cmsg.setIsoUuid(msg.getIsoUuid());
        cmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        cmsg.setDescription(msg.getDescription());
        bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, cmsg.getVmInstanceUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                }

                CreateVmCdRomReply r1 = reply.castReply();
                event.setInventory(r1.getInventory());
                bus.publish(event);
            }
        });
    }

    private void updateVmPriority(APIUpdateVmPriorityMsg msg, Completion completion) {
        VmPriorityLevel oldLevel = priorityOperator.getVmPriority(self.getUuid());
        VmPriorityLevel newLevel = VmPriorityLevel.valueOf(msg.getPriority());
        if (oldLevel.equals(newLevel)) {
            completion.success();
            return;
        }

        if (!priorityOperator.needEffectImmediately(self.getState())) {
            priorityOperator.setVmPriority(self.getUuid(), newLevel);
            completion.success();
            return;
        }

        VmPriorityConfigVO priorityVO = Q.New(VmPriorityConfigVO.class).eq(VmPriorityConfigVO_.level, newLevel).find();

        UpdateVmPriorityMsg smsg = new UpdateVmPriorityMsg();
        smsg.setHostUuid(self.getHostUuid());
        smsg.setPriorityConfigStructs(asList(new PriorityConfigStruct(priorityVO, self.getUuid())));
        bus.makeTargetServiceIdByResourceUuid(smsg, HostConstant.SERVICE_ID, self.getHostUuid());
        bus.send(smsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    ErrorCode err = operr("update vm[%s] priority to [%s] failed,because %s",
                            self.getUuid(), msg.getPriority(), reply.getError());
                    completion.fail(err);
                    return;
                }

                priorityOperator.setVmPriority(self.getUuid(), newLevel);
                completion.success();
            }
        });

    }

    private void handle(APIUpdateVmPriorityMsg msg) {
        final APIUpdateVmPriorityEvent evt = new APIUpdateVmPriorityEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                updateVmPriority(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("change-vm-%s-priority", self.getUuid());
            }
        });
    }

    private void handle(APIUpdateVmCdRomMsg msg) {
        APIUpdateVmCdRomEvent event = new APIUpdateVmCdRomEvent(msg.getId());

        VmCdRomVO vmCdRomVO = dbf.findByUuid(msg.getUuid(), VmCdRomVO.class);
        boolean update = false;

        if (msg.getName() != null) {
            vmCdRomVO.setName(msg.getName());
            update = true;
        }

        if (msg.getDescription() != null ) {
            vmCdRomVO.setDescription(msg.getDescription());
            update = true;
        }

        if (update) {
            vmCdRomVO = dbf.updateAndRefresh(vmCdRomVO);
        }

        event.setInventory(VmCdRomInventory.valueOf(vmCdRomVO));
        bus.publish(event);
    }

    private void handle(final APIUpdateVmNicDriverMsg msg) {
        APIUpdateVmNicDriverEvent event = new APIUpdateVmNicDriverEvent(msg.getId());
        VmNicVO nicVO = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        boolean update = false;

        if (!msg.getDriverType().equals(nicVO.getDriverType())) {
            nicVO.setDriverType(msg.getDriverType());
            update = true;
        }

        if (update) {
            nicVO = dbf.updateAndRefresh(nicVO);
        }

        event.setInventory(VmNicInventory.valueOf(nicVO));
        bus.publish(event);
    }

    private void handle(APISetVmInstanceDefaultCdRomMsg msg) {
        APISetVmInstanceDefaultCdRomEvent event = new APISetVmInstanceDefaultCdRomEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                setVmInstanceDefaultCdRom(msg.getUuid(), new Completion(chain) {
                    @Override
                    public void success() {
                        VmCdRomVO cdRomVO = dbf.findByUuid(msg.getUuid(), VmCdRomVO.class);
                        event.setInventory(VmCdRomInventory.valueOf(cdRomVO));
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("set-vmInstance-%s-default-cdRom-%s", msg.getVmInstanceUuid(), msg.getUuid());
            }
        });
    }

    private void setVmInstanceDefaultCdRom(String vmCdRomUuid, Completion completion) {
        // update target cdRom deviceId
        // update the source cdRom deviceId
        new SQLBatch(){
            @Override
            protected void scripts() {
                List<VmCdRomVO> cdRomVOS = q(VmCdRomVO.class)
                        .eq(VmCdRomVO_.vmInstanceUuid, self.getUuid())
                        .orderBy(VmCdRomVO_.deviceId, SimpleQuery.Od.ASC)
                        .list();

                Map<String, Integer> cdRomUUidDeviceIdMap = cdRomVOS.stream().collect(Collectors.toMap(VmCdRomVO::getUuid, VmCdRomVO::getDeviceId));
                int deviceId = cdRomUUidDeviceIdMap.get(vmCdRomUuid);

                VmCdRomVO beforeDefaultCdRomVO = null;
                for (VmCdRomVO vmCdRomVO : cdRomVOS) {
                    if (vmCdRomVO.getDeviceId() == 0) {
                        beforeDefaultCdRomVO = vmCdRomVO;
                        sql(VmCdRomVO.class)
                                .eq(VmCdRomVO_.uuid, vmCdRomVO.getUuid())
                                .set(VmCdRomVO_.deviceId, VmInstanceConstant.MAXIMUM_CDROM_NUMBER)
                                .update();
                        continue;
                    }

                    if (vmCdRomUuid.equals(vmCdRomVO.getUuid())) {
                        sql(VmCdRomVO.class)
                                .eq(VmCdRomVO_.uuid, vmCdRomVO.getUuid())
                                .set(VmCdRomVO_.deviceId, 0)
                                .update();
                        continue;
                    }
                }

                if (beforeDefaultCdRomVO != null) {
                    sql(VmCdRomVO.class)
                            .eq(VmCdRomVO_.uuid, beforeDefaultCdRomVO.getUuid())
                            .set(VmCdRomVO_.deviceId, deviceId)
                            .update();
                }
            }
        }.execute();

        completion.success();
    }

    private void reimageVmInstance(final APIReimageVmInstanceMsg msg, NoErrorCompletion completion) {
        final APIReimageVmInstanceEvent evt = new APIReimageVmInstanceEvent(msg.getId());
        String rootVolumeUuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.rootVolumeUuid)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .findValue();

        ReimageVmInstanceMsg rmsg = new ReimageVmInstanceMsg();
        rmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        rmsg.setAccountUuid(msg.getSession().getAccountUuid());
        bus.makeTargetServiceIdByResourceUuid(rmsg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());

        ReimageVolumeOverlayMsg omsg = new ReimageVolumeOverlayMsg();
        omsg.setMessage(rmsg);
        omsg.setVolumeUuid(rootVolumeUuid);
        bus.makeTargetServiceIdByResourceUuid(omsg, VolumeConstant.SERVICE_ID, rootVolumeUuid);

        bus.send(omsg, new CloudBusCallBack(completion, evt) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()){
                    self = refreshVO();
                    VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                    evt.setInventory(inv);
                    bus.publish(evt);
                } else {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                }
                completion.done();
            }
        });
    }

    private void handle(ReimageVmInstanceMsg msg){
        ReimageVmInstanceReply reply = new ReimageVmInstanceReply();

        self = refreshVO();
        VolumeVO rootVolume = dbf.findByUuid(self.getRootVolumeUuid(), VolumeVO.class);
        // check vm stopped
        {
            if (self.getState() != VmInstanceState.Stopped) {
                throw new ApiMessageInterceptionException(err(
                        VmErrors.RE_IMAGE_VM_NOT_IN_STOPPED_STATE,
                        "unable to reset volume[uuid:%s] to origin image[uuid:%s]," +
                                " the vm[uuid:%s] volume attached to is not in Stopped state, current state is %s",
                        self.getRootVolumeUuid(), self.getImageUuid(),
                        self.getUuid(), self.getState()
                ));
            }
        }

        // check image cache to ensure image type is not ISO
        {
            SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
            q.select(ImageCacheVO_.mediaType);
            q.add(ImageCacheVO_.imageUuid, Op.EQ, rootVolume.getRootImageUuid());
            q.setLimit(1);
            ImageConstant.ImageMediaType imageMediaType = q.findValue();
            if (imageMediaType == null) {
                throw new OperationFailureException(err(
                        VmErrors.RE_IMAGE_CANNOT_FIND_IMAGE_CACHE,
                        "unable to reset volume[uuid:%s] to origin image[uuid:%s]," +
                                " cannot find image cache.",
                        rootVolume.getUuid(), rootVolume.getRootImageUuid()
                ));
            }
            if (imageMediaType.toString().equals("ISO")) {
                throw new OperationFailureException(err(
                        VmErrors.RE_IMAGE_IMAGE_MEDIA_TYPE_SHOULD_NOT_BE_ISO,
                        "unable to reset volume[uuid:%s] to origin image[uuid:%s]," +
                                " for image type is ISO",
                        rootVolume.getUuid(), rootVolume.getRootImageUuid()
                ));
            }
        }

        ReInitVolumeMsg rmsg = new ReInitVolumeMsg();
        rmsg.setVolumeUuid(self.getRootVolumeUuid());
        rmsg.setVmInstanceUuid(self.getUuid());
        rmsg.setAccountUuid(msg.getAccountUuid());
        rmsg.setHostUuid(self.getLastHostUuid());
        bus.makeTargetServiceIdByResourceUuid(rmsg, VolumeConstant.SERVICE_ID, rmsg.getVolumeUuid());
        bus.send(rmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void handle(OverlayMessage msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                doOverlayMessage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "overlay-message";
            }
        });
    }

    private void doOverlayMessage(OverlayMessage msg, NoErrorCompletion noErrorCompletion) {
        bus.send(msg.getMessage(), new CloudBusCallBack(msg, noErrorCompletion) {
            @Override
            public void run(MessageReply reply) {
                bus.reply(msg, reply);
                noErrorCompletion.done();
            }
        });
    }
}

