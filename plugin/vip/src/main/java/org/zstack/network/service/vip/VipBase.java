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
import org.zstack.header.vm.BeforeStartNewCreatedVmExtensionPoint;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import java.util.Arrays;
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
        } else if (msg instanceof ModifyVipAttributesMsg) {
            handle((ModifyVipAttributesMsg) msg);
        } else if (msg instanceof DeleteVipFromBackendMsg) {
            handle((DeleteVipFromBackendMsg) msg);
        } else {
            passToBackend(msg);
        }
    }

    private void handle(DeleteVipFromBackendMsg msg) {
        // DO NOT put it in the sync queue
        // DeleteVipMsg may cause DeleteVipFromBackendMsg, putting DeleteVipFromBackendMsg
        // in the queue will lead to a deadlock
        DeleteVipFromBackendReply reply = new DeleteVipFromBackendReply();
        deleteFromBackend(new Completion(msg) {
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

    private void deleteFromBackend(Completion completion) {
        if (self.getServiceProvider() == null) {
            // this VIP has not bean created on backend yet
            completion.success();
            return;
        }

        VipFactory f = vipMgr.getVipFactory(self.getServiceProvider());
        VipBaseBackend vip = f.getVip(getSelf());
        vip.releaseVipOnBackend(new Completion(completion) {
            @Override
            public void success() {
                logger.debug(String.format("successfully released vip[uuid:%s, name:%s, ip:%s] on service[%s]",
                        self.getUuid(), self.getName(), self.getIp(), self.getServiceProvider()));

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

    private interface Recover {
        void recover();
    }

    protected Recover modifyAttributes(ModifyVipAttributesStruct s) {
        VipVO origin = dbf.findByUuid(self.getUuid(), VipVO.class);
        if (origin == null) {
            logger.debug(String.format("can not modify vip[%s] attributes, it has been deleted", self.getUuid()));
            return () -> {
                throw new OperationFailureException(operr("can not modify vip[%s] attributes, it has been deleted", self.getUuid()));
            };
        }

        if (s.isServiceProvider()) {
            if (self.getServiceProvider() != null && s.getServiceProvider() != null
                    && !s.getServiceProvider().equals(self.getServiceProvider())) {
                throw new OperationFailureException(operr("service provider of the vip[uuid:%s, name:%s, ip: %s] has been set to %s",
                                self.getUuid(), self.getName(), self.getIp(), self.getServiceProvider()));
            }

            self.setServiceProvider(s.getServiceProvider());
        }

        if (s.isUserFor()) {
            if (self.getUseFor() != null && s.getUseFor() != null
                    && !self.getUseFor().equals(s.getUseFor())) {
                throw new OperationFailureException(operr("the field 'useFor' of the vip[uuid:%s, name:%s, ip: %s] has been set to %s",
                                self.getUuid(), self.getName(), self.getIp(), self.getUseFor()));
            }

            self.setUseFor(s.getUseFor());
        }

        if (s.isPeerL3NetworkUuid()) {
            if (self.getPeerL3NetworkUuid() != null && s.getPeerL3NetworkUuid() != null
                    && !self.getPeerL3NetworkUuid().equals(s.getPeerL3NetworkUuid())) {
                throw new OperationFailureException(operr("the field 'peerL3NetworkUuid' of the vip[uuid:%s, name:%s, ip: %s] has been set to %s",
                                self.getUuid(), self.getName(), self.getIp(), self.getPeerL3NetworkUuid()));
            }

            self.setPeerL3NetworkUuid(s.getPeerL3NetworkUuid());
        }

        dbf.update(self);

        return () -> dbf.update(origin);
    }

    private void handle(ModifyVipAttributesMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                refresh();

                ModifyVipAttributesStruct current = new ModifyVipAttributesStruct();
                current.setPeerL3NetworkUuid(self.getPeerL3NetworkUuid());
                current.setUseFor(self.getUseFor());
                current.setServiceProvider(self.getServiceProvider());

                ModifyVipAttributesReply reply = new ModifyVipAttributesReply();
                reply.setStruct(current);

                modifyAttributes(msg.getStruct());
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return "modify-vip-attributes";
            }
        });
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
                if (!msg.isDeleteOnBackend()) {
                    modifyAttributes(msg.getStruct());
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                releaseVip(msg.getStruct(), new Completion(msg, chain) {
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
                return "release-vip";
            }
        });
    }

    protected void releaseVip(ModifyVipAttributesStruct s, Completion completion) {
        if (self.getServiceProvider() == null) {
            // the vip has been released by other descendant network service
            logger.debug(String.format("the serviceProvider field is null, the vip[uuid:%s, name:%s, ip:%s] has been released" +
                    " by other service", self.getUuid(), self.getName(), self.getIp()));
            modifyAttributes(s);
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

                modifyAttributes(s);

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
                refresh();
                if (!msg.isCreateOnBackend()) {
                    // no need to really create the VIP on network devices,
                    // just mark it in database
                    modifyAttributes(msg.getStruct());
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                acquireVip(msg.getStruct(), new Completion(msg, chain) {
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
                return "acquire-vip";
            }
        });
    }

    protected void acquireVip(ModifyVipAttributesStruct s, Completion completion) {
        Recover recover = modifyAttributes(s);

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
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                recover.recover();
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
                        VipReleaseExtensionPoint ext = vipMgr.getVipReleaseExtensionPoint(self.getUseFor());
                        ext.releaseServicesOnVip(getSelfInventory(), new Completion(trigger) {
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
                    String __name__ = "delete-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct s = new ModifyVipAttributesStruct();
                        s.setPeerL3NetworkUuid(null);
                        s.setUseFor(null);
                        releaseVip(s, new Completion(trigger) {
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
