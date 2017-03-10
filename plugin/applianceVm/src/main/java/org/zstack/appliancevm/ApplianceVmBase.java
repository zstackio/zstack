package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.appliancevm.ApplianceVmCommands.RefreshFirewallCmd;
import org.zstack.appliancevm.ApplianceVmCommands.RefreshFirewallRsp;
import org.zstack.compute.vm.VmInstanceBase;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.MessageCommandRecorder;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.header.configuration.*;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.Message;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.RangeSet;
import org.zstack.utils.RangeSet.Range;
import org.zstack.utils.function.Function;

import static org.zstack.core.Platform.operr;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

public abstract class ApplianceVmBase extends VmInstanceBase implements ApplianceVm {
    @Autowired
    private RESTFacade restf;

    static {
        allowedOperations.addState(VmInstanceState.Created, StartNewCreatedApplianceVmMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Running, ApplianceVmRefreshFirewallMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Starting, ApplianceVmRefreshFirewallMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Stopping, ApplianceVmRefreshFirewallMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Rebooting, ApplianceVmRefreshFirewallMsg.class.getName());
    }

    @Autowired
    protected ApplianceVmFacade apvmf;

    protected abstract List<Flow> getPostCreateFlows();
    protected abstract List<Flow> getPostStartFlows();
    protected abstract List<Flow> getPostStopFlows();
    protected abstract List<Flow> getPostRebootFlows();
    protected abstract List<Flow> getPostDestroyFlows();
    protected abstract List<Flow> getPostMigrateFlows();

    public ApplianceVmBase(VmInstanceVO vo) {
        super(vo);
    }

    protected ApplianceVmVO getSelf() {
        return (ApplianceVmVO) self;
    }

    protected ApplianceVmInventory getInventory() {
        return ApplianceVmInventory.valueOf(getSelf());
    }

    protected List<Flow> createBootstrapFlows(HypervisorType hvType) {
        Boolean unitTestOn = CoreGlobalProperty.UNIT_TEST_ON;
        List<Flow> flows = new ArrayList<Flow>();

        flows.add(apvmf.createBootstrapFlow(hvType));
        if (!unitTestOn) {
            flows.add(new ApplianceVmConnectFlow());
            flows.add(new ApplianceVmDeployAgentFlow());
        }
        flows.add(new ApplianceVmSetFirewallFlow());

        return flows;
    }

    @Override
    protected void destroyHook(VmInstanceDeletionPolicy deletionPolicy, final Completion completion){
        logger.debug(String.format("deleting appliance vm[uuid:%s], always use Direct deletion policy", self.getUuid()));
        super.doDestroy(VmInstanceDeletionPolicy.Direct, completion);
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof ApplianceVmRefreshFirewallMsg) {
            handle((ApplianceVmRefreshFirewallMsg) msg);
        } else if (msg instanceof ApplianceVmAsyncHttpCallMsg) {
            handle((ApplianceVmAsyncHttpCallMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    protected void handle(final ApplianceVmAsyncHttpCallMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public int getSyncLevel() {
                return 10;
            }

            @Override
            public String getSyncSignature() {
                return String.format("appliancevm-async-httpcall-%s", self.getUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final ApplianceVmAsyncHttpCallReply reply = new ApplianceVmAsyncHttpCallReply();
                if (msg.isCheckStatus() && getSelf().getStatus() != ApplianceVmStatus.Connected) {
                    reply.setError(operr("appliance vm[uuid:%s] is in status of %s that cannot make http call to %s",
                            self.getUuid(), getSelf().getStatus(), msg.getPath()));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                MessageCommandRecorder.record(msg.getCommandClassName());

                restf.asyncJsonPost(buildUrl(msg.getPath()), msg.getCommand(), new JsonAsyncRESTCallback<LinkedHashMap>(msg, chain) {
                    @Override
                    public void fail(ErrorCode err) {
                        reply.setError(err);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void success(LinkedHashMap ret) {
                        reply.setResponse(ret);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public Class<LinkedHashMap> getReturnClass() {
                        return LinkedHashMap.class;
                    }
                }, TimeUnit.SECONDS, msg.getCommandTimeout());
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    protected void handle(final ApplianceVmRefreshFirewallMsg msg) {
        if (msg.isInSyncThread()) {
            thdf.chainSubmit(new ChainTask(msg) {
                @Override
                public String getSyncSignature() {
                    return syncThreadName;
                }

                @Override
                public void run(final SyncTaskChain chain) {
                    refreshFirewall(msg, new NoErrorCompletion(chain) {
                        @Override
                        public void done() {
                            chain.next();
                        }
                    });
                }

                @Override
                public String getName() {
                    return "appliance-vm-refresh-firewall";
                }
            });
        } else {
            refreshFirewall(msg, new NoErrorCompletion(msg) {
                @Override
                public void done() {
                    // nothing
                }
            });
        }
    }

    public static String buildAgentUrl(String hostname, String subPath, int port) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(ApplianceVmGlobalProperty.AGENT_URL_SCHEME);
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ub.host("localhost");
        } else {
            ub.host(hostname);
        }
        ub.port(port);
        if (!"".equals(ApplianceVmGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(ApplianceVmGlobalProperty.AGENT_URL_ROOT_PATH);
        }
        ub.path(subPath);
        return ub.build().toUriString();
    }

    private String buildUrl(String path) {
        String mgmtNicIp = CollectionUtils.find(self.getVmNics(), new Function<String, VmNicVO>() {
            @Override
            public String call(VmNicVO arg) {
                if (arg.getL3NetworkUuid().equals(getSelf().getManagementNetworkUuid())) {
                    return arg.getIp();
                }
                return null;
            }
        });

        return buildAgentUrl(mgmtNicIp, path, getSelf().getAgentPort());
    }

    private void refreshFirewall(final ApplianceVmRefreshFirewallMsg msg, final NoErrorCompletion completion) {
        class RuleCombiner {
            long total;
            Map<String, List<ApplianceVmFirewallRuleVO>> rules = new HashMap<String, List<ApplianceVmFirewallRuleVO>>();
            List<ApplianceVmFirewallRuleTO> result = new ArrayList<ApplianceVmFirewallRuleTO>();
            Map<String, String> l3NicMacMap = new HashMap<String, String>();

            {
                for (VmNicVO nic : self.getVmNics()) {
                    l3NicMacMap.put(nic.getL3NetworkUuid(), nic.getMac());
                }

                SimpleQuery<ApplianceVmFirewallRuleVO> q = dbf.createQuery(ApplianceVmFirewallRuleVO.class);
                q.add(ApplianceVmFirewallRuleVO_.applianceVmUuid, Op.EQ, self.getUuid());
                total = q.count();
            }

            List<ApplianceVmFirewallRuleTO> merge() {
                if (total == 0) {
                    return new ArrayList<ApplianceVmFirewallRuleTO>();
                }

                prepare();
                normalize();
                return result;
            }

            private void normalize() {
                for (List<ApplianceVmFirewallRuleVO> vos : rules.values()) {
                    if (!vos.isEmpty()) {
                        normalize(vos);
                    }
                }
            }

            private void normalize(List<ApplianceVmFirewallRuleVO> vos) {
                String l3Uuid = null;
                String sip = null;
                String dip = null;
                String allowedCidr = null;
                ApplianceVmFirewallProtocol protocol = null;

                RangeSet rset = new RangeSet();
                for (ApplianceVmFirewallRuleVO vo : vos) {
                    if (l3Uuid == null) {
                        l3Uuid = vo.getL3NetworkUuid();
                    }
                    if (sip == null) {
                        sip = vo.getSourceIp();
                    }
                    if (dip == null) {
                        dip = vo.getDestIp();
                    }
                    if (allowedCidr == null) {
                        allowedCidr = vo.getAllowCidr();
                    }
                    if (protocol == null) {
                        protocol = vo.getProtocol();
                    }
                    rset.closed(vo.getStartPort(), vo.getEndPort());
                }

                List<Range> rs = rset.merge();
                for (Range r : rs) {
                    ApplianceVmFirewallRuleTO to = new ApplianceVmFirewallRuleTO();
                    to.setDestIp(dip);
                    to.setNicMac(l3NicMacMap.get(l3Uuid));
                    to.setProtocol(protocol.toString());
                    to.setAllowCidr(allowedCidr);
                    to.setSourceIp(sip);
                    to.setStartPort((int) r.getStart());
                    to.setEndPort((int) r.getEnd());
                    result.add(to);
                }
            }

            private void prepare() {
                int offset = 0;
                int step = 1000;
                while (offset < total) {
                    SimpleQuery<ApplianceVmFirewallRuleVO> q = dbf.createQuery(ApplianceVmFirewallRuleVO.class);
                    q.add(ApplianceVmFirewallRuleVO_.applianceVmUuid, Op.EQ, self.getUuid());
                    q.setLimit(step);
                    q.setStart(offset);
                    List<ApplianceVmFirewallRuleVO> vos = q.list();
                    for (ApplianceVmFirewallRuleVO vo : vos) {
                        String key = String.format("%s-%s-%s-%s-%s",
                                vo.getL3NetworkUuid(), vo.getProtocol(), vo.getSourceIp(), vo.getDestIp(), vo.getAllowCidr());
                        List<ApplianceVmFirewallRuleVO> lst = rules.get(key);
                        if (lst == null) {
                            lst = new ArrayList<ApplianceVmFirewallRuleVO>();
                            rules.put(key, lst);
                        }
                        lst.add(vo);
                    }
                    offset += step;
                }
            }
        }

        final ApplianceVmRefreshFirewallReply reply = new ApplianceVmRefreshFirewallReply();
        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
        if (allowed != null) {
            reply.setError(allowed);
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        RefreshFirewallCmd cmd = new RefreshFirewallCmd();
        List<ApplianceVmFirewallRuleTO> tos = new RuleCombiner().merge();
        cmd.setRules(tos);

        restf.asyncJsonPost(buildUrl(ApplianceVmConstant.REFRESH_FIREWALL_PATH), cmd, new JsonAsyncRESTCallback<RefreshFirewallRsp>(msg, completion) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(RefreshFirewallRsp ret) {
                if (!ret.isSuccess()) {
                    logger.warn(String.format("failed to refresh firewall rules on appliance vm[uuid:%s, name:%s], %s",
                            self.getUuid(), self.getName(), ret.getError()));
                    reply.setError(operr(ret.getError()));
                }

                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public Class<RefreshFirewallRsp> getReturnClass() {
                return RefreshFirewallRsp.class;
            }
        });
    }

    @Override
    protected VmInstanceVO refreshVO() {
        self = dbf.findByUuid(self.getUuid(), ApplianceVmVO.class);
        return self;
    }

    @Override
    protected VmInstanceSpec buildSpecFromInventory(VmInstanceInventory inv, VmOperation operation) {
        VmInstanceSpec spec = super.buildSpecFromInventory(inv, operation);
        spec.putExtensionData(ApplianceVmConstant.Params.applianceVmSubType.toString(), getSelf().getApplianceVmType());
        return spec;
    }

    private void prepareLifeCycleInfo(FlowChain chain) {
        ApplianceVmPostLifeCycleInfo info = new ApplianceVmPostLifeCycleInfo();
        ApplianceVmInventory ainv = ApplianceVmInventory.valueOf(getSelf());
        L3NetworkVO defaultRouteL3VO = dbf.findByUuid(ainv.getDefaultRouteL3NetworkUuid(), L3NetworkVO.class);
        info.setDefaultRouteL3Network(L3NetworkInventory.valueOf(defaultRouteL3VO));
        info.setManagementNic(ainv.getManagementNic());
        chain.getData().put(ApplianceVmConstant.Params.applianceVmInfoForPostLifeCycle.toString(), info);
    }

    private void prepareFirewallInfo(FlowChain chain) {
        SimpleQuery<ApplianceVmFirewallRuleVO> q = dbf.createQuery(ApplianceVmFirewallRuleVO.class);
        q.add(ApplianceVmFirewallRuleVO_.applianceVmUuid, Op.EQ, getSelf().getUuid());
        List<ApplianceVmFirewallRuleVO> vos = q.list();
        List<ApplianceVmFirewallRuleInventory> rules = ApplianceVmFirewallRuleInventory.valueOf(vos);
        chain.getData().put(ApplianceVmConstant.Params.applianceVmFirewallRules.toString(), rules);
    }

    @Override
    protected FlowChain getStopVmWorkFlowChain(VmInstanceInventory inv) {
        FlowChain chain = super.getStopVmWorkFlowChain(inv);
        chain.setName(String.format("stop-appliancevm-%s", inv.getUuid()));
        chain.insert(new Flow() {
            String __name__ = "change-appliancevm-status-to-disconnected";
            ApplianceVmStatus originStatus = getSelf().getStatus();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                getSelf().setStatus(ApplianceVmStatus.Disconnected);
                self = dbf.updateAndRefresh(self);
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                self = dbf.reload(self);
                getSelf().setStatus(originStatus);
                self = dbf.updateAndRefresh(self);
                trigger.rollback();
            }
        });

        prepareLifeCycleInfo(chain);
        prepareFirewallInfo(chain);

        List<Flow> subStopFlows = getPostStopFlows();
        if (subStopFlows != null) {
            for (Flow f : subStopFlows) {
                chain.then(f);
            }
        }

        boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
        chain.noRollback(noRollbackOnFailure);
        return chain;
    }

    @Override
    protected FlowChain getDestroyVmWorkFlowChain(VmInstanceInventory inv) {
        FlowChain chain = super.getDestroyVmWorkFlowChain(inv);
        chain.setName(String.format("destroy-appliancevm-%s", inv.getUuid()));
        chain.insert(new Flow() {
            String __name__ = "change-appliancevm-status-to-disconnected";
            ApplianceVmStatus originStatus = getSelf().getStatus();

            public void run(FlowTrigger trigger, Map data) {
                getSelf().setStatus(ApplianceVmStatus.Disconnected);
                self = dbf.updateAndRefresh(self);
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                self = dbf.reload(self);
                getSelf().setStatus(originStatus);
                self = dbf.updateAndRefresh(self);
                trigger.rollback();
            }
        });

        prepareLifeCycleInfo(chain);
        prepareFirewallInfo(chain);

        List<Flow> subDestroyFlows = getPostDestroyFlows();
        if (subDestroyFlows != null) {
            for (Flow f : subDestroyFlows) {
                chain.then(f);
            }
        }

        boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
        chain.noRollback(noRollbackOnFailure);
        return chain;
    }

    @Override
    protected FlowChain getMigrateVmWorkFlowChain(VmInstanceInventory inv) {
        FlowChain chain = super.getMigrateVmWorkFlowChain(inv);
        chain.setName(String.format("migrate-appliancevm-%s", inv.getUuid()));
        prepareLifeCycleInfo(chain);
        prepareFirewallInfo(chain);

        List<Flow> subMigrateFlows = getPostMigrateFlows();
        if (subMigrateFlows != null) {
            for (Flow f : subMigrateFlows) {
                chain.then(f);
            }
        }

        boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
        chain.noRollback(noRollbackOnFailure);
        return chain;
    }

    @Override
    protected FlowChain getRebootVmWorkFlowChain(VmInstanceInventory inv) {
        FlowChain chain = super.getRebootVmWorkFlowChain(inv);
        chain.setName(String.format("reboot-appliancevm-%s", inv.getUuid()));
        chain.insert(new Flow() {
            String __name__ = "change-appliancevm-status-to-disconnected";
            ApplianceVmStatus originStatus;

            @Override
            public void run(FlowTrigger trigger, Map data) {
                originStatus = getSelf().getStatus();
                getSelf().setStatus(ApplianceVmStatus.Disconnected);
                self = dbf.updateAndRefresh(self);
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                self = dbf.reload(self);
                getSelf().setStatus(originStatus);
                self = dbf.updateAndRefresh(self);
                trigger.rollback();
            }
        });

        prepareLifeCycleInfo(chain);
        prepareFirewallInfo(chain);

        addBootstrapFlows(chain, HypervisorType.valueOf(inv.getHypervisorType()));

        List<Flow> subRebootFlows = getPostRebootFlows();
        if (subRebootFlows != null) {
            for (Flow f : subRebootFlows) {
                chain.then(f);
            }
        }

        chain.then(new NoRollbackFlow() {
            String __name__ = "change-appliancevm-status-to-connected";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                getSelf().setStatus(ApplianceVmStatus.Connected);
                self = dbf.updateAndRefresh(self);
                trigger.next();
            }
        });

        boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
        chain.noRollback(noRollbackOnFailure);
        return chain;
    }

    @Override
    protected FlowChain getStartVmWorkFlowChain(VmInstanceInventory inv) {
        FlowChain chain = super.getStartVmWorkFlowChain(inv);
        chain.setName(String.format("start-appliancevm-%s", inv.getUuid()));
        chain.insert(new Flow() {
            String __name__ = "change-appliancevm-status-to-connecting";
            ApplianceVmStatus originStatus = getSelf().getStatus();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                getSelf().setStatus(ApplianceVmStatus.Connecting);
                self = dbf.updateAndRefresh(self);
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                self = dbf.reload(self);
                getSelf().setStatus(originStatus);
                self = dbf.updateAndRefresh(self);
                trigger.rollback();
            }
        });

        prepareLifeCycleInfo(chain);
        prepareFirewallInfo(chain);

        addBootstrapFlows(chain, HypervisorType.valueOf(inv.getHypervisorType()));

        List<Flow> subStartFlows = getPostStartFlows();
        if (subStartFlows != null) {
            for (Flow f : subStartFlows) {
                chain.then(f);
            }
        }

        chain.then(new NoRollbackFlow() {
            String __name__ = "change-appliancevm-status-to-connected";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                getSelf().setStatus(ApplianceVmStatus.Connected);
                self = dbf.updateAndRefresh(self);
                trigger.next();
            }
        });

        boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
        chain.noRollback(noRollbackOnFailure);
        return chain;
    }

    private FlowChain addBootstrapFlows(FlowChain chain, HypervisorType hvType) {
        for (Flow flow : createBootstrapFlows(hvType)) {
            chain.then(flow);
        }

        return chain;
    }

    @Override
    protected void startVmFromNewCreate(final StartNewCreatedVmInstanceMsg msg, final SyncTaskChain taskChain) {
        boolean callNext = true;
        try {
            refreshVO();
            ErrorCode allowed = validateOperationByState(msg, self.getState(), null);
            if (allowed != null) {
                bus.replyErrorByMessageType(msg, allowed);
                return;
            }
            ErrorCode preCreated = extEmitter.preStartNewCreatedVm(msg.getVmInstanceInventory());
            if (preCreated != null) {
                bus.replyErrorByMessageType(msg, preCreated);
                return;
            }

            StartNewCreatedApplianceVmMsg smsg = (StartNewCreatedApplianceVmMsg) msg;
            ApplianceVmSpec aspec = smsg.getApplianceVmSpec();

            final VmInstanceSpec spec = new VmInstanceSpec();
            spec.setVmInventory(msg.getVmInstanceInventory());
            if (msg.getL3NetworkUuids() != null && !msg.getL3NetworkUuids().isEmpty()) {
                SimpleQuery<L3NetworkVO> nwquery = dbf.createQuery(L3NetworkVO.class);
                nwquery.add(L3NetworkVO_.uuid, SimpleQuery.Op.IN, msg.getL3NetworkUuids());
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
                List<DiskOfferingInventory> disks = new ArrayList<>();
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
                spec.setDataDiskOfferings(new ArrayList<>(0));
            }

            ImageVO imvo = dbf.findByUuid(spec.getVmInventory().getImageUuid(), ImageVO.class);
            spec.getImageSpec().setInventory(ImageInventory.valueOf(imvo));

            spec.putExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), aspec);
            spec.setCurrentVmOperation(VmInstanceConstant.VmOperation.NewCreate);
            spec.putExtensionData(ApplianceVmConstant.Params.applianceVmSubType.toString(), getSelf().getApplianceVmType());
            spec.setBootOrders(list(VmBootDevice.HardDisk.toString()));

            changeVmStateInDb(VmInstanceStateEvent.starting);

            extEmitter.beforeStartNewCreatedVm(VmInstanceInventory.valueOf(self));
            FlowChain chain = apvmf.getCreateApplianceVmWorkFlowBuilder().build();
            setFlowMarshaller(chain);

            chain.setName(String.format("create-appliancevm-%s", msg.getVmInstanceUuid()));
            chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
            chain.getData().put(ApplianceVmConstant.Params.applianceVmFirewallRules.toString(), aspec.getFirewallRules());

            addBootstrapFlows(chain, VolumeFormat.getMasterHypervisorTypeByVolumeFormat(imvo.getFormat()));

            List<Flow> subCreateFlows = getPostCreateFlows();
            if (subCreateFlows != null) {
                for (Flow f : subCreateFlows) {
                    chain.then(f);
                }
            }

            chain.then(new NoRollbackFlow() {
                String __name__ = "change-appliancevm-status-to-connected";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    // must reload here, otherwise it will override changes created by previous flows
                    self = dbf.reload(self);
                    getSelf().setStatus(ApplianceVmStatus.Connected);
                    dbf.update(self);
                    trigger.next();
                }
            });

            boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
            chain.noRollback(noRollbackOnFailure);
            chain.done(new FlowDoneHandler(msg, taskChain) {
                @Override
                public void handle(Map data) {
                    VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                    self = dbf.reload(self);
                    self.setLastHostUuid(spec.getDestHost().getUuid());
                    self.setHostUuid(spec.getDestHost().getUuid());
                    self.setClusterUuid(spec.getDestHost().getClusterUuid());
                    self.setZoneUuid(spec.getDestHost().getZoneUuid());
                    self.setHypervisorType(spec.getDestHost().getHypervisorType());
                    self.setRootVolumeUuid(spec.getDestRootVolume().getUuid());
                    changeVmStateInDb(VmInstanceStateEvent.running);
                    logger.debug(String.format("appliance vm[uuid:%s, name: %s, type:%s] is running ..",
                            self.getUuid(), self.getName(), getSelf().getApplianceVmType()));
                    VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                    extEmitter.afterStartNewCreatedVm(inv);
                    StartNewCreatedVmInstanceReply reply = new StartNewCreatedVmInstanceReply();
                    reply.setVmInventory(inv);
                    bus.reply(msg, reply);
                    taskChain.next();
                }
            }).error(new FlowErrorHandler(msg, taskChain) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    extEmitter.failedToStartNewCreatedVm(VmInstanceInventory.valueOf(self), errCode);
                    dbf.remove(self);
                    StartNewCreatedVmInstanceReply reply = new StartNewCreatedVmInstanceReply();
                    reply.setError(errCode);
                    reply.setSuccess(false);
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

    @Override
    protected void selectDefaultL3(VmNicInventory nic) {
        return;
    }
}
