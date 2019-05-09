package org.zstack.network.service.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.ReturnIpMsg;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/11/19.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VipBase {
    protected static final CLogger logger = Utils.getLogger(VipBase.class);

    protected VipVO self;

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected VipManager vipMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected EventFacade evtf;

    protected String getThreadSyncSignature() {
        return String.format("vip-%s-%s", self.getName(), self.getUuid());
    }

    protected VipVO getSelf() {
        return self;
    }

    protected VipInventory getSelfInventory() {
        return VipInventory.valueOf(getSelf());
    }

    public VipBase(VipVO self) {
        this.self = self;
    }

    protected void refresh() {
        VipVO vo = dbf.reload(self);
        if (vo == null) {
            throw new OperationFailureException(err(SysErrors.RESOURCE_NOT_FOUND,
                    "cannot find the vip[name:%s, uuid:%s, ip:%s], it may have been deleted",
                    self.getName(), self.getUuid(), self.getIp()
            ));
        }

        self = vo;
    }

    private void cleanInDB() {
        clearPeerL3Network();
        self.setServiceProvider(null);
        dbf.update(self);
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    protected void passToBackend(Message msg) {
        if (self.getServiceProvider() == null) {
            bus.dealWithUnknownMessage(msg);
            return;
        }

        VipFactory f = vipMgr.getVipFactory(self.getServiceProvider());
        VipBaseBackend bkd = f.getVip(getSelf());
        bkd.handleBackendSpecificMessage(msg);
    }

    protected void handleLocalMessage(Message msg) {
        if (msg instanceof VipDeletionMsg) {
            handle((VipDeletionMsg) msg);
        } else if (msg instanceof AcquireVipMsg) {
            handle((AcquireVipMsg) msg);
        } else if (msg instanceof ReleaseVipMsg) {
            handle((ReleaseVipMsg) msg);
        } else if (msg instanceof StopVipMsg) {
            handle((StopVipMsg) msg);
        } else {
            passToBackend(msg);
        }
    }

    protected boolean acquireCheckModifyVipAttributeStruct(ModifyVipAttributesStruct s) {
        if (s.isServiceProvider()) {
            if (self.getServiceProvider() != null && s.getServiceProvider() != null
                    && !s.getServiceProvider().equals(self.getServiceProvider())) {
                throw new OperationFailureException(operr("service provider of the vip[uuid:%s, name:%s, ip: %s] has been set to %s",
                        self.getUuid(), self.getName(), self.getIp(), self.getServiceProvider()));
            }
            self.setServiceProvider(s.getServiceProvider());
        }

        if (s.isPeerL3NetworkUuid()) {
            try {
                if (s.isServiceProvider()) {
                    s.getPeerL3NetworkUuids().forEach(peer -> addPeerL3NetworkUuid(peer));
                }
            } catch (CloudRuntimeException e) {
                throw new OperationFailureException(operr(e.getMessage()));
            }
        }

        self = dbf.updateAndRefresh(self);

        /* snat service is bound the router interface, don't need to bound to backend */
        if (s.getUseFor().equals(NetworkServiceType.SNAT.toString())) {
            return false;
        }

        return s.isPeerL3NetworkUuid() && s.isServiceProvider();
    }

    protected boolean CheckModifyVipAttributeStructWithoutReleaseService( ModifyVipAttributesStruct s, List<String> services) {
        if (services == null || services.isEmpty()){
            /*there are no any services using this vip*/
            return false;
        }

        if (s.isUserFor() && !services.contains(s.getUseFor())) {
            /*the service using this vip has been deleted. don't delete repeat*/
            return false;
        }

        if ( services.contains(NetworkServiceType.SNAT.toString())) {
            /* snat is bound to router public interface, it is created automatically,
             * so it should be deleted automatically, but don't need to remove from backend */
            return false;
        }

        long activeNetworks = 0;
        long activeServices = 0;
        for (VipGetServiceReferencePoint ext : pluginRgty.getExtensionList(VipGetServiceReferencePoint.class)) {
            VipGetServiceReferencePoint.ServiceReference service = ext.getServiceReference(self.getUuid());
            activeServices += service.serviceUids.size();
            activeNetworks += service.count;
        }

        if (activeServices > 1) {
            return false;
        }

        /*the vip is active in this service, in another word, there are at least one nic/l3network to attach
             * with the vip */
        int deleting = 0;
        if (s.getPeerL3NetworkUuids() != null) {
            deleting = s.getPeerL3NetworkUuids().size();
        }
        return activeNetworks <= deleting;
    }

    protected boolean releaseCheckModifyVipAttributeStruct( ModifyVipAttributesStruct s, List<String> services) {
        if (services == null || services.isEmpty()){
            /*there are no any services using this vip*/
            return false;
        }

        if (s.isUserFor() && !services.contains(s.getUseFor())) {
            /*the service using this vip has been deleted. don't delete repeat*/
            return false;
        }

        if (services.size() == 1) {
            if (s.isUserFor() && s.getUseFor().equals(NetworkServiceType.SNAT.toString())) {
                /* snat is bound to router public interface, it is created automatically,
                 * so it should be deleted automatically, but don't need to remove from backend */
                dbf.remove(self);
                return false;
            }

            return true;
        }

        if ( services.contains(NetworkServiceType.SNAT.toString())) {
            /* snat is bound to router public interface, it is created automatically,
             * so it should be deleted automatically, but don't need to remove from backend */
            return false;
        }

        long activeNetworks = 0;
        long activeServices = 0;
        for (VipGetServiceReferencePoint ext : pluginRgty.getExtensionList(VipGetServiceReferencePoint.class)) {
            VipGetServiceReferencePoint.ServiceReference service = ext.getServiceReference(self.getUuid());
            activeNetworks += service.count;
            activeServices += service.serviceUids.size();
        }

        if (activeServices > 1) {
            return false;
        }
        /*the vip is active in this service, in another word, there are at least one peerL3network to attach
         * with the vip */
        //return activeServices == 0;
        int deleting = 0;
        if (s.getPeerL3NetworkUuids() != null) {
            deleting = s.getPeerL3NetworkUuids().size();
        }
        return activeNetworks <= deleting;
    }

    private void handle(ReleaseVipMsg msg) {
        ReleaseVipReply reply = new ReleaseVipReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {

                releaseVip(msg.getStruct(), true, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        VipInventory vip = VipInventory.valueOf(self);
                        reply.setInventory(vip);
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
                return "release-vip";
            }
        });
    }

    private void handle(StopVipMsg msg) {
        StopVipReply reply = new StopVipReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {

                releaseVip(msg.getStruct(), false, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        VipInventory vip = VipInventory.valueOf(self);
                        reply.setInventory(vip);
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
                return "release-vip";
            }
        });
    }

    protected void releaseVip(ModifyVipAttributesStruct s, Boolean releaseServices, Completion completion) {

        refresh();

        if (self.getServicesRefs() == null || self.getServicesRefs().isEmpty()){
            /*there are no any services using this vip*/
            /* no need to remove vip from backend */
            completion.success();
            return;
        }

        List<String> services = self.getServicesRefs().stream().map(VipNetworkServicesRefVO::getServiceType).collect(Collectors.toList());
        /* s == null is called from VipDeleteMsg, all service has been released */
        if ((s != null) && !releaseServices && (!CheckModifyVipAttributeStructWithoutReleaseService(s, services)) ||
                (s != null) && releaseServices && (!releaseCheckModifyVipAttributeStruct(s, services))) {
            try {
                if (s.getUseFor() != null && releaseServices) {
                    delServicesRef(s.getServiceUuid(),s.getUseFor());
                }
                if (s.isPeerL3NetworkUuid() && s.isServiceProvider()) {
                    s.getPeerL3NetworkUuids().forEach(peer -> deletePeerL3Network(peer));
                }
            } catch (CloudRuntimeException e) {
                throw new OperationFailureException(operr(e.getMessage()));
            }
            /* no need to remove vip from backend */
            completion.success();
            return;
        }

        if (self.getServiceProvider() == null) {
            logger.debug(String.format("the serviceProvider field is null, the vip[uuid:%s, name:%s, ip:%s] has been released" +
                    " by other service", self.getUuid(), self.getName(), self.getIp()));
            for (VipCleanupExtensionPoint ext : pluginRgty.getExtensionList(VipCleanupExtensionPoint.class)) {
                ext.cleanupVip(self.getUuid());
            }
            if (s != null && s.getUseFor() != null && releaseServices) {
                delServicesRef(s.getServiceUuid(),s.getUseFor());
            }
            completion.success();
            return;
        }

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(BeforeReleaseVipExtensionPoint.class),
                new ForEachFunction<BeforeReleaseVipExtensionPoint>() {
                    @Override
                    public void run(BeforeReleaseVipExtensionPoint ext) {
                        logger.debug(String.format("execute before release vip extension point %s", ext));
                        ext.beforeReleaseVip(VipInventory.valueOf(getSelf()));
                    }
                });

        VipFactory f = vipMgr.getVipFactory(self.getServiceProvider());
        VipBaseBackend vip = f.getVip(getSelf());
        vip.releaseVipOnBackend(new Completion(completion) {
            @Override
            public void success() {
                logger.debug(String.format("successfully released vip[uuid:%s, name:%s, ip:%s] on service[%s]",
                        self.getUuid(), self.getName(), self.getIp(), self.getServiceProvider()));
                if (releaseServices) {
                    clearServicesRefs();
                }
                cleanInDB();
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to release vip[uuid:%s, name:%s, ip:%s] on service[%s], its garbage collector should" +
                        " handle this", self.getUuid(), self.getName(), self.getIp(), self.getServiceProvider()));
                completion.fail(errorCode);
            }
        });
    }

    protected void handle(AcquireVipMsg msg) {
        AcquireVipReply reply = new AcquireVipReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {

                acquireVip(msg.getStruct(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        VipInventory vip = VipInventory.valueOf(self);
                        reply.setVip(vip);
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
                return "acquire-vip";
            }
        });
    }

    protected void acquireVip(ModifyVipAttributesStruct s, Completion completion) {

        refresh();

        if (s.isUserFor() && s.getServiceUuid() != null) {
            addServicesRef(s.getServiceUuid(),s.getUseFor());
        }

        if (!acquireCheckModifyVipAttributeStruct(s)) {
            /* no need to install vip to backend */
            completion.success();
            return;
        }

        VipFactory f = vipMgr.getVipFactory(self.getServiceProvider());
        VipBaseBackend vip = f.getVip(getSelf());
        vip.acquireVipOnBackend(new Completion(completion) {
            @Override
            public void success() {
                logger.debug(String.format("successfully acquired vip[uuid:%s, name:%s, ip:%s] on service[%s]",
                        self.getUuid(), self.getName(), self.getIp(), s.getServiceProvider()));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (s.isUserFor() && s.getServiceUuid() != null) {
                    delServicesRef(s.getServiceUuid(),s.getUseFor());
                }

                s.getPeerL3NetworkUuids().forEach(peer -> deletePeerL3Network(peer));
                completion.fail(errorCode);
            }
        });
    }

    protected void handle(VipDeletionMsg msg) {
        VipDeletionReply reply = new VipDeletionReply();
        VipInventory inventory = VipInventory.valueOf(self);
        String accountUuid = Q.New(AccountResourceRefVO.class)
                .select(AccountResourceRefVO_.accountUuid)
                .eq(AccountResourceRefVO_.resourceUuid, msg.getVipUuid())
                .findValue();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                deleteVip(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);

                        VipCanonicalEvents.VipEventData vipEventData = new VipCanonicalEvents.VipEventData();
                        vipEventData.setVipUuid(msg.getVipUuid());
                        vipEventData.setCurrentStatus(VipCanonicalEvents.VIP_STATUS_DELETED);
                        vipEventData.setInventory(inventory);
                        vipEventData.setDate(new Date());
                        vipEventData.setAccountUuid(accountUuid);
                        evtf.fire(VipCanonicalEvents.VIP_DELETED_PATH, vipEventData);

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
                return "delete-vip";
            }
        });
    }

    protected void returnVip(Completion completion) {
        ReturnIpMsg msg = new ReturnIpMsg();
        msg.setL3NetworkUuid(self.getL3NetworkUuid());
        msg.setUsedIpUuid(self.getUsedIpUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, self.getL3NetworkUuid());
        bus.send(msg, new CloudBusCallBack(completion){
            @Override
            public void run(MessageReply reply) {
                completion.success();
            }
        });
    }

    private void releaseServicesOnVip(Iterator<String> it, FlowTrigger trigger){
        if(!it.hasNext()){
            trigger.next();
            return;
        }

        String useFor = it.next();
        if (useFor.equals(VipUseForList.SNAT_NETWORK_SERVICE_TYPE)){
            releaseServicesOnVip(it, trigger);
        }
        else if (useFor != null && !useFor.equals("null")) {
            VipReleaseExtensionPoint ext = vipMgr.getVipReleaseExtensionPoint(useFor);
            ext.releaseServicesOnVip(getSelfInventory(), new Completion(trigger) {
                @Override
                public void success() {
                    releaseServicesOnVip(it, trigger);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    trigger.fail(errorCode);
                }
            });
        } else {
            trigger.next();
            return;
        }
    }

    protected void deleteVip(Completion completion) {
        refresh();
        Set<String> services = self.getServicesTypes();

        if(services == null || services.isEmpty()){
            /*there are no any services using this vip*/
            dbf.remove(self);
            returnVip(completion);

            logger.debug(String.format("no services using this vip, released vip[uuid:%s, ip:%s] on l3Network[uuid:%s]",
                    self.getUuid(), self.getIp(), self.getL3NetworkUuid()));

            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-vip-uuid-%s-ip-%s-name-%s", self.getUuid(), self.getIp(), self.getName()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "pre-release-services-on-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<PreVipReleaseExtensionPoint> exts = pluginRgty.getExtensionList(PreVipReleaseExtensionPoint.class);
                        if(exts.isEmpty()){
                            trigger.next();
                        }
                        
                        for (PreVipReleaseExtensionPoint ext : exts){
                            ext.preReleaseServicesOnVip(getSelfInventory(), new Completion(trigger) {
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

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "release-services-on-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> types = new ArrayList<>(services);
                        Iterator<String> it = types.iterator();
                        releaseServicesOnVip(it, trigger);
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        releaseVip(null, true, new Completion(trigger) {
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

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        refresh();
                        dbf.remove(self);
                        returnVip(completion);
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

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeVipStateMsg) {
            handle((APIChangeVipStateMsg) msg);
        } else if (msg instanceof APIDeleteVipMsg) {
            handle((APIDeleteVipMsg) msg);
        } else if (msg instanceof APIUpdateVipMsg)  {
            handle((APIUpdateVipMsg) msg);
        } else {
            passToBackend(msg);
        }
    }

    protected void handle(APIUpdateVipMsg msg) {
        VipVO vo = dbf.findByUuid(msg.getUuid(), VipVO.class);
        boolean update = false;
        if (msg.getName() != null) {
            vo.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            vo = dbf.updateAndRefresh(vo);
        }

        APIUpdateVipEvent evt = new APIUpdateVipEvent(msg.getId());
        evt.setInventory(VipInventory.valueOf(vo));
        bus.publish(evt);
    }

    protected void handle(APIDeleteVipMsg msg) {
        final APIDeleteVipEvent evt = new APIDeleteVipEvent(msg.getId());
        refresh();
        /* virtual router public nic vip can not be deleted */
        Set<String> services = self.getServicesTypes();
        if(services != null && services.contains( VipUseForList.SNAT_NETWORK_SERVICE_TYPE)) {
            evt.setError(operr("Vip [uuid %s, ip %s] of router public interface can not be deleted", self.getUuid(), self.getIp()));
            bus.publish(evt);
            return;
        }

        final String issuer = VipVO.class.getSimpleName();
        final List<VipInventory> ctx = Arrays.asList(VipInventory.valueOf(self));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-vip-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-vip-permissive-check";

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
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-vip-permissive-delete";

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
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-vip-force-delete";

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

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errCode, errCode.getDetails()));
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    protected void handle(APIChangeVipStateMsg msg) {
        VipVO vip = dbf.findByUuid(msg.getUuid(), VipVO.class);
        VipStateEvent sevt = VipStateEvent.valueOf(msg.getStateEvent());
        vip.setState(vip.getState().nextState(sevt));
        vip = dbf.updateAndRefresh(vip);

        APIChangeVipStateEvent evt = new APIChangeVipStateEvent(msg.getId());
        evt.setInventory(VipInventory.valueOf(vip));
        bus.publish(evt);
    }

    public Boolean checkPeerL3Additive(String peerL3NetworkUuid) {
        if (peerL3NetworkUuid == null) {
            return false;
        }

        if (self.getPeerL3NetworkRefs() == null || self.getPeerL3NetworkRefs().isEmpty()) {
            return true;
        }

        if (self.getPeerL3NetworkRefs().stream()
                .anyMatch(ref -> ref.getL3NetworkUuid().equals(peerL3NetworkUuid))) {
            logger.debug(String.format("peer l3 [uuid:%s] has already add to vip[uuid:%s], skip to add",
                    peerL3NetworkUuid, self.getUuid()));
            return false;
        }

        VmNicVO nnic = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, peerL3NetworkUuid)
                .notNull(VmNicVO_.metaData).limit(1).find();
        if (nnic == null) {
            logger.debug(String.format("add peer l3[uuid:%s] to vip[uuid:%s], the l3 has no vr attached now",
                    peerL3NetworkUuid, self.getUuid()));
            return true;
        }

        for (VipPeerL3NetworkRefVO ref : self.getPeerL3NetworkRefs()) {
            VmNicVO enic = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, ref.getL3NetworkUuid())
                    .notNull(VmNicVO_.metaData).limit(1).find();
            if (enic == null || enic.getVmInstanceUuid().equals(nnic.getVmInstanceUuid())) {
                continue;
            }
            throw new CloudRuntimeException(String.format("the request to add peer l3[uuid:%s] with vip[uuid:%s] has " +
                            "attched vr[uuid:%s] and is not vr[uuid:%s] which exists peer l3 attached", peerL3NetworkUuid, self.getUuid(),
                    nnic.getVmInstanceUuid(), enic.getVmInstanceUuid()));
        }

        return true;
    }

    private Boolean checkPeerL3Deleteable(String peerL3NetworkUuid) {
        refresh();

        if (self.getPeerL3NetworkRefs() == null || self.getPeerL3NetworkRefs().isEmpty()) {
            return false;
        }

        if (self.getPeerL3NetworkRefs().stream()
                .anyMatch(ref -> ref.getL3NetworkUuid().equals(peerL3NetworkUuid))) {
            /*
            * check if there are services to use the l3
            * */
            int useCount = 0;
            int uuidCount = 0;
            for (VipGetServiceReferencePoint ext : pluginRgty.getExtensionList(VipGetServiceReferencePoint.class)) {
                VipGetServiceReferencePoint.ServiceReference service = ext.getServicePeerL3Reference(self.getUuid(), peerL3NetworkUuid);
                uuidCount += service.serviceUids.size();
                useCount += service.count ;
            }

            if ( uuidCount <= 1) {
                return useCount <= 1;
            }
        }

        return false;
    }

    public void addPeerL3NetworkUuid(String peerL3NetworkUuid) {
        if (checkPeerL3Additive(peerL3NetworkUuid) == false) {
            logger.debug(String.format("can not add peer l3[uuid:%s] to vip[uuid:%s]",
                    peerL3NetworkUuid, self.getUuid()));
            return;
        }

        if (Q.New(VipPeerL3NetworkRefVO.class)
                .eq(VipPeerL3NetworkRefVO_.vipUuid, self.getUuid())
                .eq(VipPeerL3NetworkRefVO_.l3NetworkUuid, peerL3NetworkUuid)
                .find() != null) {
            logger.debug(String.format("peer l3 [uuid:%s] has already add to vip[uuid:%s], skip to add",
                    peerL3NetworkUuid, self.getUuid()));
            return;
        }
        VipPeerL3NetworkRefVO vo = new VipPeerL3NetworkRefVO();
        vo.setVipUuid(self.getUuid());
        vo.setL3NetworkUuid(peerL3NetworkUuid);
        dbf.persistAndRefresh(vo);
        logger.debug(String.format("added peer l3[uuid:%s] to vip[uuid:%s]",
                peerL3NetworkUuid, self.getUuid()));
    }

    private void deletePeerL3Network(String peerL3NetworkUuid) {
        if (checkPeerL3Deleteable(peerL3NetworkUuid)) {
            deletePeerL3NetworkUuid(peerL3NetworkUuid);
        }
    }

    public void deletePeerL3NetworkUuid(String peerL3NetworkUuid) {
        refresh();
        VipPeerL3NetworkRefVO vo = Q.New(VipPeerL3NetworkRefVO.class)
                .eq(VipPeerL3NetworkRefVO_.vipUuid, self.getUuid())
                .eq(VipPeerL3NetworkRefVO_.l3NetworkUuid, peerL3NetworkUuid)
                .find();
        if (vo != null) {
            dbf.remove(vo);
        }
        logger.debug(String.format("deleted peer l3[uuid:%s] from vip[uuid:%s]",
                peerL3NetworkUuid, self.getUuid()));
    }

    public void clearPeerL3Network() {
        List<VipPeerL3NetworkRefVO> vos = Q.New(VipPeerL3NetworkRefVO.class)
                .eq(VipPeerL3NetworkRefVO_.vipUuid, self.getUuid())
                .list();
        if (vos != null && !vos.isEmpty()) {
            dbf.removeCollection(vos, VipPeerL3NetworkRefVO.class);
        }
        self.setPeerL3NetworkRefs(null);
        refresh();
    }

    private void addServicesRef(String uuid, String type) {
        VipNetworkServicesRefVO vipRef = new VipNetworkServicesRefVO();

        if (dbf.findByUuid(uuid, VipNetworkServicesRefVO.class) != null) {
            logger.debug(String.format("repeat to add the servicesRef [type:%s:uuid:%s] with vip[uuid:%s]",
                    type, uuid, self.getUuid()));
            return;
        }
        vipRef.setUuid(uuid);
        vipRef.setServiceType(type);
        vipRef.setVipUuid(self.getUuid());
        dbf.persist(vipRef);
        Set<String> types = self.getServicesTypes();
        if (  types == null ) {
            self.setUseFor(type);
            self = dbf.updateAndRefresh(self);
        } else if (!types.contains(type)) {
            types.add(type);
            self.setUseFor(new VipUseForList(types).toString());
            self = dbf.updateAndRefresh(self);
        }

        logger.debug(String.format("add the servicesRef [type:%s:uuid:%s] with vip[uuid:%s]",
                type, uuid, self.getUuid()));
    }

    private void delServicesRef(String uuid, String type) {
        DebugUtils.Assert((uuid != null) && (type != null), "the parameter can't be null");
        VipNetworkServicesRefVO vipRef = dbf.findByUuid(uuid, VipNetworkServicesRefVO.class);
        if ( vipRef == null) {
            logger.error(String.format("the servicesRef [type:%s:uuid:%s] with vip[uuid:%s] doesn't exist",
                    type, uuid, self.getUuid()));
            return;
        }

        dbf.remove(vipRef);
        refresh();
        Set<String> types = self.getServicesTypes();
        if (  types == null ) {
            self.setUseFor(null);
            self = dbf.updateAndRefresh(self);
        } else if (!types.contains(type)) {
            self.setUseFor(new VipUseForList(types).toString());
            self = dbf.updateAndRefresh(self);
        }

        logger.debug(String.format("delete the servicesRef [type:%s:uuid:%s] with vip[uuid:%s]",
                type, uuid, self.getUuid()));
    }

    private void clearServicesRefs() {
        List<VipNetworkServicesRefVO> vipRefs = Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, self.getUuid()).list();
        if (vipRefs != null && !vipRefs.isEmpty()) {
            dbf.removeCollection(vipRefs, VipNetworkServicesRefVO.class);
            self.setUseFor(null);
            self.setServicesRefs(null);
            self = dbf.updateAndRefresh(self);
        }
        logger.debug(String.format("clear the servicesRefs with vip[uuid:%s]",self.getUuid()));
    }
}