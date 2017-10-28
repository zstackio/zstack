package org.zstack.network.service.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
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
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.ReturnIpMsg;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.VipUseForList;

import static org.zstack.core.Platform.operr;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
            throw new OperationFailureException(errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND,
                    String.format("cannot find the vip[name:%s, uuid:%s, ip:%s], it may have been deleted",
                            self.getName(), self.getUuid(), self.getIp())
            ));
        }

        self = vo;
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
        }  else {
            passToBackend(msg);
        }
    }

    protected boolean acquireCheckModifyVipAttributeStruct(ModifyVipAttributesStruct s) {
        if (s.isUserFor()) {
             /* snat service is bound the router interface, don't need to bound to backend */
            if (s.getUseFor().equals(NetworkServiceType.SNAT.toString())) {
                return false;
            }
        }

        if (s.isServiceProvider()) {
            if (self.getServiceProvider() != null && s.getServiceProvider() != null
                    && !s.getServiceProvider().equals(self.getServiceProvider())) {
                throw new OperationFailureException(operr("service provider of the vip[uuid:%s, name:%s, ip: %s] has been set to %s",
                        self.getUuid(), self.getName(), self.getIp(), self.getServiceProvider()));
            }
        }

        if (s.isPeerL3NetworkUuid()) {
            if (self.getPeerL3NetworkUuid() != null && s.getPeerL3NetworkUuid() != null
                    && !self.getPeerL3NetworkUuid().equals(s.getPeerL3NetworkUuid())) {
                throw new OperationFailureException(operr("the field 'peerL3NetworkUuid' of the vip[uuid:%s, name:%s, ip: %s] has been set to %s",
                        self.getUuid(), self.getName(), self.getIp(), self.getPeerL3NetworkUuid()));
            }
        }

        if (s.isPeerL3NetworkUuid() && s.isServiceProvider()){
            self.setPeerL3NetworkUuid(s.getPeerL3NetworkUuid());
            self.setServiceProvider(s.getServiceProvider());
            dbf.update(self);
            return true;
        } else {
            return false;
        }
    }

    protected boolean releaseCheckModifyVipAttributeStruct( ModifyVipAttributesStruct s) {

        VipUseForList useForList = new VipUseForList(self.getUseFor());
        if (s.isUserFor() && s.getUseFor().equals(NetworkServiceType.SNAT.toString())) {
            useForList.del(s.getUseFor());
            if (useForList.getUseForList().isEmpty()){
                /* snat is bound to router public interface, it is created automatically,
                 * so it should be deleted automatically, but don't need to remove from backend */
                dbf.remove(self);
            } else {
                self.setUseFor(useForList.toString());
                dbf.update(self);
            }
            return false;
        }

        for (VipGetServiceReferencePoint ext : pluginRgty.getExtensionList(VipGetServiceReferencePoint.class)) {
            VipGetServiceReferencePoint.ServiceReference service = ext.getServiceReference(self.getUuid());
            if (service.useFor.equals(s.getUseFor()) && service.count <= 1){
                useForList.del(s.getUseFor());
                self.setUseFor(useForList.toString());
                dbf.update(self);
            }
        }

        if (useForList.getUseForList().isEmpty()){
            return true;
        } else {
            return false;
        }
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

                releaseVip(msg.getStruct(), new Completion(msg, chain) {
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

    protected void releaseVip(ModifyVipAttributesStruct s, Completion completion) {

        refresh();
        if (self.getServiceProvider() == null) {
            logger.debug(String.format("the serviceProvider field is null, the vip[uuid:%s, name:%s, ip:%s] has been released" +
                    " by other service", self.getUuid(), self.getName(), self.getIp()));
            completion.success();
            return;
        }

        /* s == null is called from VipDeleteMsg, all service has beed released */
        if ((s != null) && (!releaseCheckModifyVipAttributeStruct(s))){
            /* no need to remove vip from backend */
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

                VipUseForList useForList = new VipUseForList(self.getUseFor());
                self.setUseFor(null);
                self.setPeerL3NetworkUuid(null);
                self.setServiceProvider(null);
                dbf.update(self);

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

        if (!acquireCheckModifyVipAttributeStruct(s)){
            /* no need to install vip to backend */
            VipUseForList useForList = new VipUseForList(self.getUseFor());
            useForList.add(s.getUseFor());
            self.setUseFor(useForList.toString());
            dbf.update(self);
            completion.success();
            return;
        }

        VipFactory f = vipMgr.getVipFactory(s.getServiceProvider());
        VipBaseBackend vip = f.getVip(getSelf());
        vip.acquireVipOnBackend(new Completion(completion) {
            @Override
            public void success() {
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(AfterAcquireVipExtensionPoint.class),
                        new ForEachFunction<AfterAcquireVipExtensionPoint>() {
                            @Override
                            public void run(AfterAcquireVipExtensionPoint ext) {
                                logger.debug(String.format("execute after acquire vip extension point %s", ext));
                                ext.afterAcquireVip(VipInventory.valueOf(getSelf()));
                            }
                        });
                logger.debug(String.format("successfully acquired vip[uuid:%s, name:%s, ip:%s] on service[%s]",
                        self.getUuid(), self.getName(), self.getIp(), s.getServiceProvider()));

                VipUseForList useForList = new VipUseForList(self.getUseFor());
                useForList.add(s.getUseFor());
                self.setUseFor(useForList.toString());
                self.setPeerL3NetworkUuid(s.getPeerL3NetworkUuid());
                self.setServiceProvider(s.getServiceProvider());
                dbf.update(self);

                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    protected void handle(VipDeletionMsg msg) {
        VipDeletionReply reply = new VipDeletionReply();
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

    protected void returnVip() {
        ReturnIpMsg msg = new ReturnIpMsg();
        msg.setL3NetworkUuid(self.getL3NetworkUuid());
        msg.setUsedIpUuid(self.getUsedIpUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, self.getL3NetworkUuid());
        bus.send(msg);
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
        else {
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
        }

        return;
    }

    protected void deleteVip(Completion completion) {
        refresh();

        if (self.getUseFor() == null) {
            dbf.remove(self);
            returnVip();
            logger.debug(String.format("'useFor' is not set, released vip[uuid:%s, ip:%s] on l3Network[uuid:%s]",
                    self.getUuid(), self.getIp(), self.getL3NetworkUuid()));
            completion.success();
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
                        VipUseForList useForList = new VipUseForList(self.getUseFor());
                        Iterator<String> it = useForList.getUseForList().iterator();
                        releaseServicesOnVip(it, trigger);
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        releaseVip(null, new Completion(trigger) {
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
                        dbf.remove(self);
                        returnVip();
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

        /* virtual router public nic vip can not be deleted */
        VipUseForList useForList = new VipUseForList(self.getUseFor());
        if(useForList.isIncluded(VipUseForList.SNAT_NETWORK_SERVICE_TYPE)){
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
                        evt.setError(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errCode));
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
}
