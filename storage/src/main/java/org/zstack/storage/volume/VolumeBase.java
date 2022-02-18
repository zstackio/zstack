package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIDeleteMessage.DeletionMode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.OverlayMessage;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.header.volume.VolumeConstant.Capability;
import org.zstack.header.volume.VolumeDeletionPolicyManager.VolumeDeletionPolicy;
import org.zstack.identity.AccountManager;
import org.zstack.storage.snapshot.group.VolumeSnapshotGroupCreationValidator;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:20 PM
 * To change this template use File | Settings | File Templates.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeBase implements Volume {
    private static final CLogger logger = Utils.getLogger(VolumeBase.class);
    protected String syncThreadId;
    protected VolumeVO self;
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CascadeFacade casf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private VolumeDeletionPolicyManager deletionPolicyMgr;

    public VolumeBase(VolumeVO vo) {
        self = vo;
        syncThreadId = String.format("volume-%s", self.getUuid());
    }

    protected void refreshVO() {
        self = dbf.reload(self);
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage) msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof VolumeDeletionMsg) {
            handle((VolumeDeletionMsg) msg);
        } else if (msg instanceof DeleteVolumeMsg) {
            handle((DeleteVolumeMsg) msg);
        } else if (msg instanceof VolumeCreateSnapshotMsg) {
            handle((VolumeCreateSnapshotMsg) msg);
        } else if (msg instanceof CreateImageCacheFromVolumeMsg) {
            handle((CreateImageCacheFromVolumeMsg) msg);
        } else if (msg instanceof CreateDataVolumeTemplateFromDataVolumeMsg) {
            handle((CreateDataVolumeTemplateFromDataVolumeMsg) msg);
        } else if (msg instanceof ExpungeVolumeMsg) {
            handle((ExpungeVolumeMsg) msg);
        } else if (msg instanceof RecoverVolumeMsg) {
            handle((RecoverVolumeMsg) msg);
        } else if (msg instanceof EstimateVolumeTemplateSizeMsg) {
            handle((EstimateVolumeTemplateSizeMsg) msg);
        }  else if (msg instanceof SyncVolumeSizeMsg) {
            handle((SyncVolumeSizeMsg) msg);
        } else if (msg instanceof InstantiateVolumeMsg) {
            handle((InstantiateVolumeMsg) msg);
        } else if (msg instanceof OverlayMessage) {
            handle((OverlayMessage) msg);
        } else if (msg instanceof ChangeVolumeStatusMsg) {
            handle((ChangeVolumeStatusMsg) msg);
        } else if (msg instanceof OverwriteVolumeMsg) {
            handle((OverwriteVolumeMsg) msg);
        } else if (msg instanceof ReInitVolumeMsg) {
            handle((ReInitVolumeMsg) msg);
        } else if (msg instanceof GetVolumeBackingInstallPathMsg) {
            handle((GetVolumeBackingInstallPathMsg) msg);
        } else if (msg instanceof SetVmBootVolumeMsg) {
            handle((SetVmBootVolumeMsg) msg);
        } else if (msg instanceof CreateVolumeSnapshotGroupMsg) {
            handle((CreateVolumeSnapshotGroupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(ReInitVolumeMsg msg) {
        ReInitVolumeReply reply = new ReInitVolumeReply();
        refreshVO();

        VolumeInventory rootVolumeInventory = VolumeInventory.valueOf(self);
        final long originSize = Q.New(ImageCacheVO.class)
                .select(ImageCacheVO_.size)
                .limit(1)
                .eq(ImageCacheVO_.imageUuid, self.getRootImageUuid())
                .findValue();
        // do the re-image op
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("reset-root-volume-%s-from-image-%s", self.getUuid(), self.getRootImageUuid()));
        chain.then(new ShareFlow() {
            VolumeVO vo = self;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "allocate-primary-storage";

                    boolean success;
                    String allocatedInstallUrl;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                        amsg.setRequiredPrimaryStorageUuid(self.getPrimaryStorageUuid());
                        amsg.setSize(originSize);
                        amsg.setRequiredHostUuid(msg.getHostUuid());

                        bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }
                                AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                                allocatedInstallUrl = ar.getAllocatedInstallUrl();
                                success = true;
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (success) {
                            ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                            rmsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
                            rmsg.setDiskSize(originSize);
                            rmsg.setAllocatedInstallUrl(allocatedInstallUrl);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                            bus.send(rmsg);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "mark-root-volume-as-snapshot-on-primary-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        MarkRootVolumeAsSnapshotMsg gmsg = new MarkRootVolumeAsSnapshotMsg();
                        rootVolumeInventory.setDescription(String.format("save snapshot for reimage vm [uuid:%s]", msg.getVmInstanceUuid()));
                        rootVolumeInventory.setName(String.format("reimage-vm-point-%s-%s", msg.getVmInstanceUuid(), TimeUtils.getCurrentTimeStamp("yyyyMMddHHmmss")));
                        gmsg.setVolume(rootVolumeInventory);
                        gmsg.setAccountUuid(msg.getAccountUuid());
                        bus.makeLocalServiceId(gmsg, VolumeSnapshotConstant.SERVICE_ID);
                        bus.send(gmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "reset-root-volume-from-image-on-primary-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ReInitRootVolumeFromTemplateOnPrimaryStorageMsg rmsg = new ReInitRootVolumeFromTemplateOnPrimaryStorageMsg();
                        rmsg.setVolume(rootVolumeInventory);
                        rmsg.setOriginSize(originSize);
                        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, rootVolumeInventory.getPrimaryStorageUuid());
                        bus.send(rmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    ReInitRootVolumeFromTemplateOnPrimaryStorageReply re = (ReInitRootVolumeFromTemplateOnPrimaryStorageReply) reply;
                                    vo.setInstallPath(re.getNewVolumeInstallPath());
                                    vo = dbf.updateAndRefresh(vo);
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "sync-volume-size-after-reimage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        SyncVolumeSizeMsg smsg = new SyncVolumeSizeMsg();
                        smsg.setVolumeUuid(vo.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(smsg, VolumeConstant.SERVICE_ID, rootVolumeInventory.getUuid());
                        bus.send(smsg, new CloudBusCallBack(msg) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                vo.setSize(((SyncVolumeSizeReply) reply).getSize());
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        dbf.update(vo);

                        List<AfterReimageVmInstanceExtensionPoint> list = pluginRgty.getExtensionList(
                                AfterReimageVmInstanceExtensionPoint.class);
                        for (AfterReimageVmInstanceExtensionPoint ext : list) {
                            ext.afterReimageVmInstance(rootVolumeInventory);
                        }

                        self = dbf.reload(self);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        logger.warn(String.format("failed to restore volume[uuid:%s] to image[uuid:%s], %s",
                                rootVolumeInventory.getUuid(), rootVolumeInventory.getRootImageUuid(), errCode));
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void handle(ChangeVolumeStatusMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            @Deferred
            public void run(SyncTaskChain chain) {
                refreshVO();

                Defer.defer(() -> {
                    ChangeVolumeStatusReply reply = new ChangeVolumeStatusReply();
                    bus.reply(msg, reply);
                });

                if (self == null) {
                    // volume has been deleted by previous request
                    // this happens when delete vm request queued before
                    // migrating trigger not by api
                    // in this case, ignore change state request
                    logger.debug(String.format("volume[uuid:%s] has been deleted, ignore change volume state request",
                            msg.getVolumeUuid()));
                    chain.next();
                    return;
                }

                VolumeStatus bs = self.getStatus();
                SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).set(VolumeVO_.status, msg.getStatus()).update();
                refreshVO();
                logger.debug(String.format("volume[uuid:%s] status changed from %s to %s in db", self.getUuid(), bs, self.getStatus()));
                chain.next();
            }

            @Override
            public String getName() {
                return "change-volume-status";
            }
        });
    }

    private void handle(InstantiateVolumeMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("instantiate-volume-%s", self.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doInstantiateVolume(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "instantiate-volume";
            }
        });
    }

    private void doInstantiateVolume(InstantiateVolumeMsg msg, NoErrorCompletion completion) {
        InstantiateVolumeReply reply = new InstantiateVolumeReply();

        // InstantiateVolumeMsg is queued, but we need to check whether the volume has been instantiated
        self = dbf.reload(self);
        if (self.getStatus() == VolumeStatus.Ready) {
            reply.setVolume(getSelfInventory());
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        List<PreInstantiateVolumeExtensionPoint> exts = pluginRgty.getExtensionList(PreInstantiateVolumeExtensionPoint.class);
        for (PreInstantiateVolumeExtensionPoint ext : exts) {
            ext.preInstantiateVolume(msg);
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("instantiate-volume-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            String installPath;
            String format;

            @Override
            public void setup() {
                if (!msg.isPrimaryStorageAllocated()) {
                    flow(new Flow() {
                        String __name__ = "allocate-primary-storage";

                        boolean success;
                        String allocateInstallUrl;

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                            amsg.setSystemTags(msg.getSystemTags());
                            amsg.setRequiredHostUuid(msg.getHostUuid());
                            amsg.setRequiredPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                            amsg.setSize(self.getSize());

                            bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                        return;
                                    }
                                    success = true;
                                    AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                                    allocateInstallUrl = ar.getAllocatedInstallUrl();
                                    trigger.next();
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            if (success) {
                                ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                                rmsg.setAllocatedInstallUrl(allocateInstallUrl);
                                rmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                                rmsg.setDiskSize(self.getSize());
                                bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                                bus.send(rmsg);
                            }

                            trigger.rollback();
                        }
                    });
                }

                flow(new Flow() {
                    String __name__ = "instantiate-volume-on-primary-storage";

                    boolean success;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
                        q.select(PrimaryStorageVO_.type);
                        q.add(PrimaryStorageVO_.uuid, Op.EQ, msg.getPrimaryStorageUuid());
                        String psType = q.findValue();

                        InstantiateDataVolumeOnCreationExtensionPoint ext = pluginRgty.getExtensionFromMap(psType, InstantiateDataVolumeOnCreationExtensionPoint.class);
                        if (ext != null) {
                            ext.instantiateDataVolumeOnCreation(msg, getSelfInventory(), new ReturnValueCompletion<VolumeInventory>(trigger) {
                                @Override
                                public void success(VolumeInventory ret) {
                                    success = true;
                                    installPath = ret.getInstallPath();
                                    format = ret.getFormat();
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        } else {
                            if (msg instanceof InstantiateTemporaryRootVolumeMsg) {
                                instantiateTemporaryRootVolume((InstantiateTemporaryRootVolumeMsg) msg , trigger);
                            } else if (msg instanceof InstantiateRootVolumeMsg) {
                                instantiateRootVolume((InstantiateRootVolumeMsg) msg, trigger);
                            } else if (msg instanceof InstantiateMemoryVolumeMsg) {
                                instantiateMemoryVolume(msg, trigger);
                            } else {
                                instantiateDataVolume(msg, trigger);
                            }
                        }
                    }

                    private void instantiateMemoryVolume(InstantiateVolumeMsg msg, FlowTrigger trigger) {
                        InstantiateMemoryVolumeOnPrimaryStorageMsg imsg = new InstantiateMemoryVolumeOnPrimaryStorageMsg();
                        prepareMsg(msg, imsg);
                        doInstantiateVolume(imsg, trigger);
                    }

                    private void instantiateTemporaryRootVolume(InstantiateTemporaryRootVolumeMsg msg , FlowTrigger trigger) {
                        InstantiateVolumeOnPrimaryStorageMsg imsg;
                        if (ImageConstant.ImageMediaType.RootVolumeTemplate.toString().equals(msg.getTemplateSpec().getInventory().getMediaType())) {
                            imsg = new InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg();
                            ((InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg)imsg).setOriginVolumeUuid(msg.getOriginVolumeUuid());
                            ((InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg)imsg).setTemplateSpec(msg.getTemplateSpec());
                        } else {
                            imsg = new InstantiateTemporaryVolumeOnPrimaryStorageMsg();
                            ((InstantiateTemporaryVolumeOnPrimaryStorageMsg)imsg).setOriginVolumeUuid(msg.getOriginVolumeUuid());
                        }

                        prepareMsg(msg, imsg);
                        doInstantiateVolume(imsg, trigger);
                    }

                    private void instantiateRootVolume(InstantiateRootVolumeMsg msg, FlowTrigger trigger) {
                        InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg imsg = new InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg();
                        prepareMsg(msg, imsg);
                        imsg.setTemplateSpec(msg.getTemplateSpec());

                        doInstantiateVolume(imsg, trigger);
                    }

                    private void instantiateDataVolume(InstantiateVolumeMsg msg, FlowTrigger trigger) {
                        InstantiateVolumeOnPrimaryStorageMsg imsg = new InstantiateVolumeOnPrimaryStorageMsg();
                        prepareMsg(msg, imsg);
                        doInstantiateVolume(imsg, trigger);
                    }

                    private void prepareMsg(InstantiateVolumeMsg msg, InstantiateVolumeOnPrimaryStorageMsg imsg) {
                        imsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                        imsg.setVolume(getSelfInventory());
                        imsg.setSystemTags(msg.getSystemTags());
                        imsg.setSkipIfExisting(msg.isSkipIfExisting());
                        if (msg.getHostUuid() != null) {
                            imsg.setDestHost(HostInventory.valueOf(dbf.findByUuid(msg.getHostUuid(), HostVO.class)));
                        }
                        bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                    }

                    private void doInstantiateVolume(InstantiateVolumeOnPrimaryStorageMsg imsg, FlowTrigger trigger) {
                        bus.send(imsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                success = true;
                                InstantiateVolumeOnPrimaryStorageReply ir = reply.castReply();
                                installPath = ir.getVolume().getInstallPath();
                                format = ir.getVolume().getFormat();

                                List<AfterInstantiateVolumeExtensionPoint> exts = pluginRgty.getExtensionList(AfterInstantiateVolumeExtensionPoint.class);
                                for (AfterInstantiateVolumeExtensionPoint ext : exts) {
                                    ext.afterInstantiateVolume(imsg);
                                }
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (success) {
                            DeleteVolumeOnPrimaryStorageMsg dmsg = new DeleteVolumeOnPrimaryStorageMsg();
                            dmsg.setUuid(msg.getPrimaryStorageUuid());
                            dmsg.setVolume(getSelfInventory());
                            bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                            bus.send(dmsg);
                        }

                        trigger.rollback();
                    }
                });

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        VolumeStatus oldStatus = self.getStatus();
                        self.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                        self.setInstallPath(installPath);
                        DebugUtils.Assert(format != null, "format cannot be null");
                        self.setFormat(format);
                        self.setStatus(VolumeStatus.Ready);
                        self = dbf.updateAndRefresh(self);

                        VolumeInventory vol = getSelfInventory();
                        new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, vol);

                        reply.setVolume(vol);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });

                Finally(new FlowFinallyHandler(msg, completion) {
                    @Override
                    public void Finally() {
                        completion.done();
                    }
                });
            }

        }).start();
    }

    private void handle(OverlayMessage msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
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

    private void handle(final SyncVolumeSizeMsg msg) {
        final SyncVolumeSizeReply reply = new SyncVolumeSizeReply();
        syncVolumeVolumeSize(new ReturnValueCompletion<VolumeSize>(msg) {
            @Override
            public void success(VolumeSize ret) {
                reply.setActualSize(ret.actualSize);
                reply.setSize(ret.size);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final EstimateVolumeTemplateSizeMsg msg) {
        final EstimateVolumeTemplateSizeReply reply = new EstimateVolumeTemplateSizeReply();
        syncVolumeVolumeSize(new ReturnValueCompletion<VolumeSize>(msg) {
            @Override
            public void success(VolumeSize ret) {
                reply.setActualSize(Math.min(ret.actualSize, ret.size));
                reply.setSize(ret.size);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final RecoverVolumeMsg msg) {
        final RecoverVolumeReply reply = new RecoverVolumeReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                refreshVO();
                recoverVolume(new Completion(chain, msg) {
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
                return RecoverVolumeMsg.class.getName();
            }
        });
    }

    private void expunge(final Completion completion) {
        if (self.getStatus() != VolumeStatus.Deleted) {
            completion.fail(operr("the volume[uuid:%s, name:%s] is not deleted yet, can't expunge it",
                            self.getUuid(), self.getName()));
            return;
        }

        final VolumeInventory inv = getSelfInventory();
        pluginRgty.getExtensionList(VolumeBeforeExpungeExtensionPoint.class).forEach(ext -> ext.volumePreExpunge(inv));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("expunge-volume");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "call-before-expunge-volume-extensions";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(pluginRgty.getExtensionList(VolumeBeforeExpungeExtensionPoint.class)).each((ext, c) -> {
                            ext.volumeBeforeExpunge(inv, new Completion(c) {
                                @Override
                                public void success() {
                                    c.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    logger.debug(String.format("failed to execute extension, because %s", errorCode.getDetails()));
                                    c.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                if (self.getPrimaryStorageUuid() != null) {
                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("delete-volume-%s-on-primary-storage", inv.getUuid());

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            DeleteVolumeOnPrimaryStorageMsg dmsg = new DeleteVolumeOnPrimaryStorageMsg();
                            dmsg.setVolume(getSelfInventory());
                            dmsg.setUuid(self.getPrimaryStorageUuid());
                            bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                            bus.send(dmsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply r) {
                                    if (!r.isSuccess() && dbf.isExist(dmsg.getPrimaryStorageUuid(), PrimaryStorageVO.class)) {
                                        trigger.fail(r.getError());
                                        return;
                                    }

                                    trigger.next();
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("return-volume-%s-size-to-primary-storage", inv.getUuid());

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            IncreasePrimaryStorageCapacityMsg msg = new IncreasePrimaryStorageCapacityMsg();
                            msg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
                            msg.setDiskSize(self.getSize());
                            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                            bus.send(msg);

                            /**
                             * After volume is deleted on primary storage and volume capacity returned,
                             * set primaryStorageUuid to null to confirm IncreasePrimaryStorageCapacityMsg
                             * only be send once.
                             */
                            self.setPrimaryStorageUuid(null);
                            self = dbf.updateAndRefresh(self);

                            trigger.next();
                        }
                    });
                }
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeAfterExpungeExtensionPoint.class), arg -> arg.volumeAfterExpunge(inv));

                VolumeInventory volumeInventory = getSelfInventory();
                String accountUuid = acntMgr.getOwnerAccountUuidOfResource(self.getUuid());
                dbf.remove(self);
                cleanupVolumeEO(self.getUuid());
                completion.success();
                new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(VolumeStatus.Deleted, volumeInventory, accountUuid);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void cleanupVolumeEO(String volumeUuid) {
        boolean exists = Q.New(VolumeSnapshotEO.class)
                .eq(VolumeSnapshotEO_.volumeUuid, volumeUuid)
                .isExists();
        if (exists) {
            return;
        }

        dbf.eoCleanup(VolumeVO.class, self.getUuid());
    }

    private void handle(final ExpungeVolumeMsg msg) {
        final ExpungeVolumeReply reply = new ExpungeVolumeReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                refreshVO();
                expunge(new Completion(msg, chain) {
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
                return ExpungeVolumeMsg.class.getName();
            }
        });
    }

    private void handle(final CreateImageCacheFromVolumeMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                doCreateImageCacheFromVolume(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("create-data-volume-template-from-data-volume-%s", msg.getVolumeUuid());
            }
        });
    }

    private void handle(final CreateDataVolumeTemplateFromDataVolumeMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                refreshVO();
                doCreateDataVolumeTemplateFromDataVolumeMsg(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("create-data-volume-template-from-data-volume-%s", msg.getVolumeUuid());
            }
        });
    }

    private void doCreateImageCacheFromVolume(CreateImageCacheFromVolumeMsg msg, NoErrorCompletion completion) {
        CreateImageCacheFromVolumeReply outReply = new CreateImageCacheFromVolumeReply();

        final CreateImageCacheFromVolumeOnPrimaryStorageMsg cmsg = new CreateImageCacheFromVolumeOnPrimaryStorageMsg();
        cmsg.setImageInventory(msg.getImage());
        cmsg.setVolumeInventory(getSelfInventory());
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    outReply.setError(r.getError());
                } else {
                    CreateImageCacheFromVolumeOnPrimaryStorageReply reply = r.castReply();
                    outReply.setLocateHostUuid(reply.getLocateHostUuid());
                }

                bus.reply(msg, outReply);
                completion.done();
            }
        });
    }

    private void doCreateDataVolumeTemplateFromDataVolumeMsg(CreateDataVolumeTemplateFromDataVolumeMsg msg, NoErrorCompletion noErrorCompletion) {
        final CreateTemplateFromVolumeOnPrimaryStorageMsg cmsg = new CreateTemplateFromVolumeOnPrimaryStorageMsg();
        cmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        cmsg.setImageInventory(ImageInventory.valueOf(dbf.findByUuid(msg.getImageUuid(), ImageVO.class)));
        cmsg.setVolumeInventory(getSelfInventory());
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
        bus.send(cmsg, new CloudBusCallBack(msg, noErrorCompletion) {
            @Override
            public void run(MessageReply r) {
                CreateDataVolumeTemplateFromDataVolumeReply reply = new CreateDataVolumeTemplateFromDataVolumeReply();
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                } else {
                    CreateTemplateFromVolumeOnPrimaryStorageReply creply = r.castReply();
                    String backupStorageInstallPath = creply.getTemplateBackupStorageInstallPath();
                    reply.setFormat(creply.getFormat());
                    reply.setInstallPath(backupStorageInstallPath);
                    reply.setMd5sum(null);
                    reply.setBackupStorageUuid(msg.getBackupStorageUuid());
                }

                bus.reply(msg, reply);
                noErrorCompletion.done();
            }
        });
    }

    private void handle(final DeleteVolumeMsg msg) {
        final DeleteVolumeReply reply = new DeleteVolumeReply();
        delete(true, VolumeDeletionPolicy.valueOf(msg.getDeletionPolicy()),
                msg.isDetachBeforeDeleting(), new Completion(msg) {
                    @Override
                    public void success() {
                        logger.debug(String.format("deleted data volume[uuid: %s]", msg.getUuid()));
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
    }

    private void deleteVolume(final VolumeDeletionMsg msg, final NoErrorCompletion completion) {
        final VolumeDeletionReply reply = new VolumeDeletionReply();
        for (VolumeDeletionExtensionPoint extp : pluginRgty.getExtensionList(VolumeDeletionExtensionPoint.class)) {
            extp.preDeleteVolume(getSelfInventory());
        }

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeDeletionExtensionPoint.class), new ForEachFunction<VolumeDeletionExtensionPoint>() {
            @Override
            public void run(VolumeDeletionExtensionPoint arg) {
                arg.beforeDeleteVolume(getSelfInventory());
            }
        });


        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-volume-%s", self.getUuid()));
        // for NotInstantiated Volume, no flow to execute
        chain.allowEmptyFlow();
        chain.then(new ShareFlow() {
            VolumeDeletionPolicy deletionPolicy;

            {

                if (msg.getDeletionPolicy() == null) {
                    deletionPolicy = deletionPolicyMgr.getDeletionPolicy(self.getUuid());
                } else {
                    deletionPolicy = VolumeDeletionPolicy.valueOf(msg.getDeletionPolicy());
                }
            }

            @Override
            public void setup() {
                if (self.getVmInstanceUuid() != null && self.getType() == VolumeType.Data && msg.isDetachBeforeDeleting() &&
                        self.getStatus() != VolumeStatus.NotInstantiated && dbf.isExist(self.getVmInstanceUuid(), VmInstanceVO.class)) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "detach-volume-from-vm";

                        public void run(final FlowTrigger trigger, Map data) {
                            DetachDataVolumeFromVmMsg dmsg = new DetachDataVolumeFromVmMsg();
                            dmsg.setVolume(getSelfInventory());
                            String vmUuid;
                            if (dmsg.getVmInstanceUuid() == null) {
                                vmUuid = getSelfInventory().getVmInstanceUuid();
                            } else {
                                vmUuid = dmsg.getVmInstanceUuid();
                            }
                            dmsg.setVmInstanceUuid(vmUuid);
                            bus.makeTargetServiceIdByResourceUuid(dmsg, VmInstanceConstant.SERVICE_ID, vmUuid);
                            bus.send(dmsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                        return;
                                    }
                                    self = dbf.reload(self);
                                    trigger.next();
                                }
                            });
                        }
                    });
                }

                if (deletionPolicy == VolumeDeletionPolicy.Direct) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-volume-from-primary-storage";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            if (self.getStatus() == VolumeStatus.Ready &&
                                 self.getPrimaryStorageUuid() != null) {
                                DeleteVolumeOnPrimaryStorageMsg dmsg = new DeleteVolumeOnPrimaryStorageMsg();
                                dmsg.setVolume(getSelfInventory());
                                dmsg.setUuid(self.getPrimaryStorageUuid());
                                bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                                logger.debug(String.format("Asking primary storage[uuid:%s] to remove data volume[uuid:%s]", self.getPrimaryStorageUuid(),
                                        self.getUuid()));
                                bus.send(dmsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            logger.warn(String.format("failed to delete volume[uuid:%s, name:%s], %s",
                                                    self.getUuid(), self.getName(), reply.getError()));
                                        }

                                        trigger.next();
                                    }
                                });
                            } else {
                                trigger.next();
                            }
                        }
                    });
                }


                if (self.getPrimaryStorageUuid() != null && deletionPolicy == VolumeDeletionPolicy.Direct) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "return-primary-storage-capacity";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                            imsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
                            imsg.setDiskSize(self.getSize());
                            bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                            bus.send(imsg);
                            trigger.next();
                        }
                    });
                }

                flow(new Flow() {
                    String __name__ = "after-delete-volume-extension";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ErrorCodeList errList = new ErrorCodeList();
                        new While<>(pluginRgty.getExtensionList(VolumeDeletionExtensionPoint.class)).each((ext, c) -> {
                            ext.afterDeleteVolume(getSelfInventory(), new Completion(c) {
                                @Override
                                public void success() {
                                    c.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errList.getCauses().add(errorCode);
                                    c.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errList.getCauses().size() > 0) {
                                    trigger.fail(errList.getCauses().get(0));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        trigger.rollback();
                    }
                });


                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        VolumeStatus oldStatus = self.getStatus();

                        if (deletionPolicy == VolumeDeletionPolicy.Direct) {
                            callVmJustBeforeDeleteFromDbExtensionPoint();
                            self.setStatus(VolumeStatus.Deleted);
                            self = dbf.updateAndRefresh(self);
                            String accountUuid = acntMgr.getOwnerAccountUuidOfResource(self.getUuid());
                            VolumeInventory volumeInventory = getSelfInventory();
                            dbf.remove(self);
                            cleanupVolumeEO(self.getUuid());
                            new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, volumeInventory, accountUuid);
                        } else if (deletionPolicy == VolumeDeletionPolicy.Delay) {
                            self.setStatus(VolumeStatus.Deleted);
                            self = dbf.updateAndRefresh(self);
                            new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, getSelfInventory());
                        } else if (deletionPolicy == VolumeDeletionPolicy.Never) {
                            self.setStatus(VolumeStatus.Deleted);
                            self = dbf.updateAndRefresh(self);
                            new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, getSelfInventory());
                        } else if (deletionPolicy == VolumeDeletionPolicy.DBOnly) {
                            callVmJustBeforeDeleteFromDbExtensionPoint();
                            VolumeInventory inventory = getSelfInventory();
                            inventory.setStatus(VolumeStatus.Deleted.toString());
                            String accountUuid = acntMgr.getOwnerAccountUuidOfResource(self.getUuid());
                            dbf.remove(self);
                            new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, inventory, accountUuid);
                        } else {
                            throw new CloudRuntimeException(String.format("Invalid deletionPolicy:%s", deletionPolicy));
                        }

                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(final ErrorCode errCode, Map data) {
                        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeDeletionExtensionPoint.class),
                                new ForEachFunction<VolumeDeletionExtensionPoint>() {
                                    @Override
                                    public void run(VolumeDeletionExtensionPoint arg) {
                                        arg.failedToDeleteVolume(getSelfInventory(), errCode);
                                    }
                                });

                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });

                Finally(new FlowFinallyHandler(msg) {
                    @Override
                    public void Finally() {
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handle(OverwriteVolumeMsg msg) {
        OverwriteVolumeReply reply = new OverwriteVolumeReply();
        VolumeInventory volume = msg.getOriginVolume(),
                transientVolume = msg.getTransientVolume();

        if (transientVolume.isAttached() && !transientVolume.getAttachedVmUuids().equals(volume.getAttachedVmUuids())) {
            throw new CloudRuntimeException(String.format("transient volume[uuid:%s] has attached a different vm " +
                            "with origin volume[uuid:%s].",transientVolume.getUuid(), volume.getUuid()));
        }

        FlowChain chain = new SimpleFlowChain();
        chain.setName("cover-volume-from-transient-volume");
        chain.then(new NoRollbackFlow() {
            String __name__ = "swap-volume-path";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        // Do a swap to keep volume uuid ...
                        sql(VolumeVO.class)
                                .eq(VolumeVO_.uuid, transientVolume.getUuid())
                                .set(VolumeVO_.installPath, volume.getInstallPath())
                                .set(VolumeVO_.size, volume.getSize())
                                .set(VolumeVO_.rootImageUuid, volume.getRootImageUuid())
                                .set(VolumeVO_.primaryStorageUuid, volume.getPrimaryStorageUuid())
                                .set(VolumeVO_.actualSize, volume.getActualSize())
                                .update();

                        sql(VolumeVO.class)
                                .eq(VolumeVO_.uuid, volume.getUuid())
                                .set(VolumeVO_.installPath, transientVolume.getInstallPath())
                                .set(VolumeVO_.size, transientVolume.getSize())
                                .set(VolumeVO_.rootImageUuid, transientVolume.getRootImageUuid())
                                .set(VolumeVO_.primaryStorageUuid, transientVolume.getPrimaryStorageUuid())
                                .set(VolumeVO_.actualSize, transientVolume.getActualSize())
                                .update();
                        flush();
                    }
                }.execute();
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "run-extension";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<ChangeVolumeInstallPathExtensionPoint> exts = pluginRgty.getExtensionList(ChangeVolumeInstallPathExtensionPoint.class);

                if (!exts.isEmpty()) {
                    runExtensions(exts.iterator(), volume.getUuid(), transientVolume, trigger);
                } else {
                    trigger.next();
                }
            }

            private void runExtensions(final Iterator<ChangeVolumeInstallPathExtensionPoint> it, String volumeUuid, final VolumeInventory transientVolume, final FlowTrigger chain) {
                if (!it.hasNext()) {
                    chain.next();
                    return;
                }

                ChangeVolumeInstallPathExtensionPoint extp = it.next();

                logger.debug(String.format("run ChangeVolumeInstallPathExtensionPoint[%s]", extp.getClass()));
                extp.afterChangeVmVolumeInstallPath(volumeUuid, transientVolume, new Completion(chain) {
                    @Override
                    public void success() {
                        runExtensions(it, volumeUuid, transientVolume, chain);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-transient-volume-" + transientVolume.getUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                DeleteVolumeMsg dmsg = new DeleteVolumeMsg();
                dmsg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString());
                dmsg.setUuid(transientVolume.getUuid());
                dmsg.setDetachBeforeDeleting(true);
                bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeConstant.SERVICE_ID, transientVolume.getUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                afterOverwriteVolume();
                createSystemTag();
                bus.reply(msg, reply);
            }

            @ExceptionSafe
            private void afterOverwriteVolume() {
                pluginRgty.getExtensionList(OverwriteVolumeExtensionPoint.class).forEach(it ->
                        it.afterOverwriteVolume(volume, transientVolume));
            }

            private void createSystemTag() {
                SystemTagCreator creator = VolumeSystemTags.OVERWRITED_VOLUME.newSystemTagCreator(volume.getUuid());
                creator.setTagByTokens(Collections.singletonMap(VolumeSystemTags.OVERWRITED_VOLUME_TOKEN, transientVolume.getUuid()));
                creator.inherent = false;
                creator.create();
            }

        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void callVmJustBeforeDeleteFromDbExtensionPoint() {
        VolumeInventory inv = getSelfInventory();
        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeJustBeforeDeleteFromDbExtensionPoint.class), p -> p.volumeJustBeforeDeleteFromDb(inv));
    }

    private void handle(final VolumeDeletionMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                self = dbf.reload(self);
                if (self == null || self.getStatus() == VolumeStatus.Deleted) {
                    VolumeDeletionPolicy deletionPolicy;
                    if (msg.getDeletionPolicy() == null) {
                        deletionPolicy = deletionPolicyMgr.getDeletionPolicy(self.getUuid());
                    } else {
                        deletionPolicy = VolumeDeletionPolicy.valueOf(msg.getDeletionPolicy());
                    }
                    if (deletionPolicy == VolumeDeletionPolicy.DBOnly) {
                        callVmJustBeforeDeleteFromDbExtensionPoint();
                        VolumeInventory inventory = getSelfInventory();
                        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(self.getUuid());
                        dbf.remove(self);
                        new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(self.getStatus(), inventory, accountUuid);
                    }

                    // the volume has been deleted
                    // we run into this case because the cascading framework
                    // will send duplicate messages when deleting a vm as the cascading
                    // framework has no knowledge about if the volume has been deleted
                    VolumeDeletionReply reply = new VolumeDeletionReply();
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                deleteVolume(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-volume-%s", self.getUuid());
            }
        });
    }

    private void handle(GetVolumeBackingInstallPathMsg msg) {
        GetVolumeBackingInstallPathReply reply = new GetVolumeBackingInstallPathReply();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("get volume backing install path");
        chain.then(new ShareFlow() {
            String currentRootPath;
            List<String> previousRootPath;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "get-snapshot-root-node";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        GetVolumeSnapshotTreeRootNodeMsg smsg = new GetVolumeSnapshotTreeRootNodeMsg();
                        smsg.setVolumeUuid(msg.getVolumeUuid());
                        bus.makeLocalServiceId(smsg, VolumeSnapshotConstant.SERVICE_ID);
                        bus.send(smsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply r) {
                                if (!r.isSuccess()) {
                                    trigger.fail(r.getError());
                                    return;
                                }

                                GetVolumeSnapshotTreeRootNodeReply gr = r.castReply();
                                currentRootPath = gr.getCurrentRootInstallPath() == null ? self.getInstallPath() :
                                        gr.getCurrentRootInstallPath();
                                previousRootPath = gr.getPreviousRootInstallPaths();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "get-root-node-from-ps";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        GetVolumeBackingChainFromPrimaryStorageMsg pmsg = new GetVolumeBackingChainFromPrimaryStorageMsg();
                        pmsg.setVolumeUuid(self.getUuid());
                        pmsg.getRootInstallPaths().addAll(previousRootPath);
                        pmsg.getRootInstallPaths().add(currentRootPath);
                        pmsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
                        bus.makeLocalServiceId(pmsg, PrimaryStorageConstant.SERVICE_ID);
                        bus.send(pmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply r) {
                                 if (!r.isSuccess()) {
                                     trigger.fail(r.getError());
                                     return;
                                 }

                                 GetVolumeBackingChainFromPrimaryStorageReply gr = r.castReply();
                                 reply.setCurrentBackingChain(gr.getBackingChainInstallPath(currentRootPath));
                                 reply.setCurrentBackingChainSize(gr.getBackingChainSize(currentRootPath));
                                 previousRootPath.forEach(path -> {
                                     reply.addPreviousBackingChain(gr.getBackingChainInstallPath(path));
                                     reply.addPreviousBackingChainSize(gr.getBackingChainSize(path));
                                 });
                                 trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void handle(SetVmBootVolumeMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                setBootVolume(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("set-vm-boot-volume-%s", self.getUuid());
            }
        });
    }

    private void setBootVolume(SetVmBootVolumeMsg msg, NoErrorCompletion completion) {
        SetVmBootVolumeReply reply = new SetVmBootVolumeReply();

        self = dbf.reload(self);
        if (self.getType() == VolumeType.Root && msg.getVmInstanceUuid().equals(self.getVmInstanceUuid())) {
            bus.reply(msg, reply);
            completion.done();
            return;
        }


        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("set-boot-volume");
        chain.then(new ShareFlow() {
            List<VolumeSnapshotInventory> newRootSnapshots = new ArrayList<>();
            List<VolumeSnapshotInventory> oldRootSnapshots = new ArrayList<>();
            VolumeInventory newRootVol;
            VolumeInventory oldRootVol;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "change-new-root-volume-type";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ChangeVolumeTypeOnPrimaryStorageMsg cmsg = new ChangeVolumeTypeOnPrimaryStorageMsg();
                        cmsg.setSnapshots(VolumeSnapshotInventory.valueOf(Q.New(VolumeSnapshotVO.class)
                                .eq(VolumeSnapshotVO_.volumeUuid, self.getUuid())
                                .list()));
                        cmsg.setTargetType(VolumeType.Root);
                        cmsg.setVolume(getSelfInventory());
                        bus.makeLocalServiceId(cmsg, PrimaryStorageConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                ChangeVolumeTypeOnPrimaryStorageReply cr = reply.castReply();
                                newRootSnapshots.addAll(cr.getSnapshots());
                                newRootVol = cr.getVolume();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "change-origin-root-volume-type";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VolumeVO originVol = Q.New(VolumeVO.class).eq(VolumeVO_.type, VolumeType.Root)
                                .eq(VolumeVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                                .find();

                        ChangeVolumeTypeOnPrimaryStorageMsg cmsg = new ChangeVolumeTypeOnPrimaryStorageMsg();
                        cmsg.setTargetType(VolumeType.Data);
                        cmsg.setVolume(VolumeInventory.valueOf(originVol));
                        cmsg.setSnapshots(VolumeSnapshotInventory.valueOf(Q.New(VolumeSnapshotVO.class)
                                .eq(VolumeSnapshotVO_.volumeUuid, originVol.getUuid())
                                .list()));
                        bus.makeLocalServiceId(cmsg, PrimaryStorageConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                ChangeVolumeTypeOnPrimaryStorageReply cr = reply.castReply();
                                oldRootSnapshots.addAll(cr.getSnapshots());
                                oldRootVol = cr.getVolume();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "set-boot-volume-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                VmInstanceVO vm = findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);

                                VolumeVO oldRootVolumeVO = vm.getRootVolume();
                                VolumeVO newRootVolumeVO = vm.getAllVolumes().stream().filter(it -> it.getUuid().equals(msg.getVolumeUuid()))
                                        .findFirst().orElseThrow(() -> new OperationFailureException(
                                                operr("volume[uuid%s] should be attached.")
                                        ));

                                oldRootVolumeVO.setType(VolumeType.Data);
                                oldRootVolumeVO.setInstallPath(oldRootVol.getInstallPath());
                                oldRootVolumeVO.setDeviceId(newRootVolumeVO.getDeviceId());
                                merge(oldRootVolumeVO);

                                newRootVolumeVO.setType(VolumeType.Root);
                                newRootVolumeVO.setInstallPath(newRootVol.getInstallPath());
                                newRootVolumeVO.setDeviceId(0);
                                merge(newRootVolumeVO);

                                for (VolumeSnapshotInventory newRootSnapshot : newRootSnapshots) {
                                    VolumeSnapshotVO snapshot = findByUuid(newRootSnapshot.getUuid(), VolumeSnapshotVO.class);
                                    snapshot.setVolumeType(VolumeType.Root.toString());
                                    snapshot.setPrimaryStorageInstallPath(newRootSnapshot.getPrimaryStorageInstallPath());
                                    merge(snapshot);
                                }

                                for (VolumeSnapshotInventory oldRootSnapshot : oldRootSnapshots) {
                                    VolumeSnapshotVO snapshot = findByUuid(oldRootSnapshot.getUuid(), VolumeSnapshotVO.class);
                                    snapshot.setVolumeType(VolumeType.Data.toString());
                                    snapshot.setPrimaryStorageInstallPath(oldRootSnapshot.getPrimaryStorageInstallPath());
                                    merge(snapshot);
                                }

                                vm.setRootVolumeUuid(newRootVolumeVO.getUuid());
                                merge(vm);
                            }
                        }.execute();
                        trigger.next();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handle(CreateVolumeSnapshotGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("create-volume-%s-snapshot-group", msg.getVolumeUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                CreateVolumeSnapshotGroupReply reply = new CreateVolumeSnapshotGroupReply();
                doCreateVolumeSnapshotGroup(msg, new ReturnValueCompletion<VolumeSnapshotGroupInventory>(chain) {
                    @Override
                    public void success(VolumeSnapshotGroupInventory inv) {
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
                return getSyncSignature();
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeVolumeStateMsg) {
            handle((APIChangeVolumeStateMsg) msg);
        } else if (msg instanceof APICreateVolumeSnapshotMsg) {
            handle((APICreateVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateVolumeSnapshotGroupMsg) {
            handle((APICreateVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof APIDeleteDataVolumeMsg) {
            handle((APIDeleteDataVolumeMsg) msg);
        } else if (msg instanceof APIDetachDataVolumeFromVmMsg) {
            handle((APIDetachDataVolumeFromVmMsg) msg);
        } else if (msg instanceof APIAttachDataVolumeToVmMsg) {
            handle((APIAttachDataVolumeToVmMsg) msg);
        } else if (msg instanceof APIGetDataVolumeAttachableVmMsg) {
            handle((APIGetDataVolumeAttachableVmMsg) msg);
        } else if (msg instanceof APIUpdateVolumeMsg) {
            handle((APIUpdateVolumeMsg) msg);
        } else if (msg instanceof APIRecoverDataVolumeMsg) {
            handle((APIRecoverDataVolumeMsg) msg);
        } else if (msg instanceof APIExpungeDataVolumeMsg) {
            handle((APIExpungeDataVolumeMsg) msg);
        } else if (msg instanceof APISyncVolumeSizeMsg) {
            handle((APISyncVolumeSizeMsg) msg);
        } else if (msg instanceof APIGetVolumeCapabilitiesMsg) {
            handle((APIGetVolumeCapabilitiesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetVolumeCapabilitiesMsg msg) {
        APIGetVolumeCapabilitiesReply reply = new APIGetVolumeCapabilitiesReply();
        Map<String, Object> ret = new HashMap<String, Object>();
        if (VolumeStatus.Ready == self.getStatus()) {
            getPrimaryStorageCapacities(ret);
        }
        reply.setCapabilities(ret);
        bus.reply(msg, reply);
    }

    private void getPrimaryStorageCapacities(Map<String, Object> ret) {
        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
        q.select(PrimaryStorageVO_.type);
        q.add(PrimaryStorageVO_.uuid, Op.EQ, self.getPrimaryStorageUuid());
        String type = q.findValue();

        PrimaryStorageType psType = PrimaryStorageType.valueOf(type);
        ret.put(Capability.MigrationInCurrentPrimaryStorage.toString(), psType.isSupportVolumeMigrationInCurrentPrimaryStorage());
        ret.put(Capability.MigrationToOtherPrimaryStorage.toString(), psType.isSupportVolumeMigrationToOtherPrimaryStorage());
    }

    private void syncVolumeVolumeSize(final ReturnValueCompletion<VolumeSize> completion) {
        refreshVO();
        SyncVolumeSizeOnPrimaryStorageMsg smsg = new SyncVolumeSizeOnPrimaryStorageMsg();
        smsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
        smsg.setVolumeUuid(self.getUuid());
        smsg.setInstallPath(self.getInstallPath());
        bus.makeTargetServiceIdByResourceUuid(smsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
        bus.send(smsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                refreshVO();
                SyncVolumeSizeOnPrimaryStorageReply r = reply.castReply();
                self.setSize(r.getSize());
                // the actual size = volume actual size + all snapshot size
                long snapshotSize = calculateSnapshotSize();
                self.setActualSize(r.getActualSize() + snapshotSize);
                self = dbf.updateAndRefresh(self);

                VolumeSize size = new VolumeSize();
                size.actualSize = self.getActualSize();
                size.size = self.getSize();
                completion.success(size);
            }

            @Transactional(readOnly = true)
            private long calculateSnapshotSize() {
                String sql = "select sum(sp.size) from VolumeSnapshotVO sp where sp.volumeUuid = :uuid";
                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("uuid", self.getUuid());
                Long size = q.getSingleResult();
                return size == null ? 0 : size;
            }
        });
    }

    private void handle(APISyncVolumeSizeMsg msg) {
        final APISyncVolumeSizeEvent evt = new APISyncVolumeSizeEvent(msg.getId());
        if (self.getStatus() != VolumeStatus.Ready) {
            evt.setInventory(getSelfInventory());
            bus.publish(evt);
            return;
        }

        syncVolumeVolumeSize(new ReturnValueCompletion<VolumeSize>(msg) {
            @Override
            public void success(VolumeSize ret) {
                evt.setInventory(getSelfInventory());
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIExpungeDataVolumeMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                final APIExpungeDataVolumeEvent evt = new APIExpungeDataVolumeEvent(msg.getId());
                refreshVO();
                expunge(new Completion(msg, chain) {
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
                return msg.getClass().getName();
            }
        });
    }

    protected void recoverVolume(Completion completion) {
        final VolumeInventory vol = getSelfInventory();
        List<RecoverDataVolumeExtensionPoint> exts = pluginRgty.getExtensionList(RecoverDataVolumeExtensionPoint.class);

        CollectionUtils.safeForEach(exts, new ForEachFunction<RecoverDataVolumeExtensionPoint>() {
            @Override
            public void run(RecoverDataVolumeExtensionPoint ext) {
                ext.preRecoverDataVolume(vol);
            }
        });


        CollectionUtils.safeForEach(exts, new ForEachFunction<RecoverDataVolumeExtensionPoint>() {
            @Override
            public void run(RecoverDataVolumeExtensionPoint ext) {
                ext.beforeRecoverDataVolume(vol);
            }
        });

        VolumeStatus oldStatus = self.getStatus();

        if (self.getInstallPath() != null) {
            self.setStatus(VolumeStatus.Ready);
        } else {
            self.setStatus(VolumeStatus.NotInstantiated);
        }
        self = dbf.updateAndRefresh(self);

        new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, getSelfInventory());

        CollectionUtils.safeForEach(exts, new ForEachFunction<RecoverDataVolumeExtensionPoint>() {
            @Override
            public void run(RecoverDataVolumeExtensionPoint ext) {
                ext.afterRecoverDataVolume(vol);
            }
        });
        completion.success();
    }

    private void handle(APIRecoverDataVolumeMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                refreshVO();
                final APIRecoverDataVolumeEvent evt = new APIRecoverDataVolumeEvent(msg.getId());
                recoverVolume(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        evt.setInventory(getSelfInventory());
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setInventory(getSelfInventory());
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return msg.getClass().getName();
            }
        });

    }

    private void handle(APIUpdateVolumeMsg msg) {
        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            self = dbf.updateAndRefresh(self);
        }

        APIUpdateVolumeEvent evt = new APIUpdateVolumeEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    @Transactional
    private List<VmInstanceVO> getCandidateVmForAttaching(String accountUuid) {
        List<String> vmUuids = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, VmInstanceVO.class);
        if (vmUuids != null && vmUuids.isEmpty()) {
            return new ArrayList<>();
        }

        SQL sql = null;
        if (self.getStatus() == VolumeStatus.Ready) {
            List<String> hvTypes = VolumeFormat.valueOf(self.getFormat()).getHypervisorTypesSupportingThisVolumeFormatInString();
            sql = SQL.New("select vm" +
                    " from VmInstanceVO vm, PrimaryStorageClusterRefVO ref, VolumeVO vol" +
                    " where vol.uuid = :volUuid" +
                    " and ref.primaryStorageUuid = vol.primaryStorageUuid" +
                    (vmUuids == null ? "" : " and vm.uuid in (:vmUuids)") +
                    " and vm.clusterUuid = ref.clusterUuid" +
                    " and vm.type in (:vmTypes)" +
                    " and vm.state in (:vmStates)" +
                    " and vm.hypervisorType in (:hvTypes)" +
                    " group by vm.uuid")
                    .param("volUuid", self.getUuid())
                    .param("vmTypes", Arrays.asList(VmInstanceConstant.USER_VM_TYPE, "baremetal2"))
                    .param("hvTypes", hvTypes);
        } else if (self.getStatus() == VolumeStatus.NotInstantiated) {
            sql = SQL.New("select vm" +
                    " from VmInstanceVO vm, PrimaryStorageClusterRefVO ref, PrimaryStorageEO ps, PrimaryStorageCapacityVO capacity" +
                    " where "+ (vmUuids == null ? "" : " vm.uuid in (:vmUuids) and") +
                    " vm.state in (:vmStates)" +
                    " and vm.type in (:vmTypes)" +
                    " and vm.clusterUuid = ref.clusterUuid" +
                    " and capacity.uuid = ps.uuid" +
                    " and capacity.availableCapacity > :volumeSize" +
                    " and ref.primaryStorageUuid = ps.uuid" +
                    " and ps.state in (:psState)" +
                    " group by vm.uuid")
                    .param("volumeSize", self.getSize())
                    .param("vmTypes", Arrays.asList(VmInstanceConstant.USER_VM_TYPE, "baremetal2"))
                    .param("psState", PrimaryStorageState.Enabled);
        } else {
            DebugUtils.Assert(false, String.format("should not reach here, volume[uuid:%s]", self.getUuid()));
        }

        if (vmUuids != null) {
            sql.param("vmUuids", vmUuids);
        }
        List<VmInstanceVO> ret = sql.param("vmStates", Arrays.asList(VmInstanceState.Running, VmInstanceState.Stopped)).list();

        ret.addAll(getVmInstancesWithoutClusterInfo(vmUuids));
        //the vm doesn't suport to online attach volume when vm platform type is other
        ret.removeIf(it -> it.getPlatform().equals(ImagePlatform.Other.toString()) && it.getState() != VmInstanceState.Stopped);
        if (ret.isEmpty()) {
            return ret;
        }

        VolumeInventory vol = getSelfInventory();
        for (VolumeGetAttachableVmExtensionPoint ext : pluginRgty.getExtensionList(VolumeGetAttachableVmExtensionPoint.class)) {
            ret = ext.returnAttachableVms(vol, ret);
        }
        return ret;
    }

    private List<VmInstanceVO> getVmInstancesWithoutClusterInfo(List<String> accessibleVmUuids) {
        SQL sql = null;
        if (self.getStatus() == VolumeStatus.Ready) {
            String primaryStorageUuid = self.getPrimaryStorageUuid();

            List<String> hvTypes = VolumeFormat.valueOf(self.getFormat()).getHypervisorTypesSupportingThisVolumeFormatInString();
            sql = SQL.New("select vm" +
                    " from VmInstanceVO vm, PrimaryStorageClusterRefVO ref, VolumeVO vol" +
                    " where vol.uuid = vm.rootVolumeUuid" +
                    " and ref.primaryStorageUuid = :primaryStorageUuid" +
                    " and vol.primaryStorageUuid = :primaryStorageUuid" +
                    (accessibleVmUuids == null ? "" : " and vm.uuid in (:vmUuids)") +
                    " and vm.clusterUuid is NULL" +
                    " and vm.type = :vmType" +
                    " and vm.state in (:vmStates)" +
                    " and vm.hypervisorType in (:hvTypes)" +
                    " group by vm.uuid")
                    .param("primaryStorageUuid", primaryStorageUuid)
                    .param("vmType", VmInstanceConstant.USER_VM_TYPE)
                    .param("hvTypes", hvTypes);
        } else if (self.getStatus() == VolumeStatus.NotInstantiated) {
            sql = SQL.New("select vm" +
                    " from VmInstanceVO vm, PrimaryStorageClusterRefVO ref, PrimaryStorageEO ps, PrimaryStorageCapacityVO capacity" +
                    " where "+ (accessibleVmUuids == null ? "" : " vm.uuid in (:vmUuids) and") +
                    " vm.state in (:vmStates)" +
                    " and vm.type = :vmType" +
                    " and vm.clusterUuid is NULL" +
                    " and capacity.uuid = ps.uuid" +
                    " and capacity.availableCapacity > :volumeSize" +
                    " and ref.primaryStorageUuid = ps.uuid" +
                    " and ps.state in (:psState)" +
                    " group by vm.uuid")
                    .param("volumeSize", self.getSize())
                    .param("vmType", VmInstanceConstant.USER_VM_TYPE)
                    .param("psState", PrimaryStorageState.Enabled);
        } else {
            DebugUtils.Assert(false, String.format("should not reach here, volume[uuid:%s]", self.getUuid()));
        }

        if (accessibleVmUuids != null) {
            sql.param("vmUuids", accessibleVmUuids);
        }

        return sql.param("vmStates", Arrays.asList(VmInstanceState.Running, VmInstanceState.Stopped)).list();
    }

    private boolean volumeIsAttached(final String volumeUuid) {
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.vmInstanceUuid);
        q.add(VolumeVO_.uuid, Op.EQ, volumeUuid);
        return q.findValue() != null;
    }

    private void handle(APIGetDataVolumeAttachableVmMsg msg) {
        APIGetDataVolumeAttachableVmReply reply = new APIGetDataVolumeAttachableVmReply();
        if (volumeIsAttached(msg.getVolumeUuid())) {
            reply.setInventories(VmInstanceInventory.valueOf(new ArrayList<>()));
        } else {
            reply.setInventories(VmInstanceInventory.valueOf(getCandidateVmForAttaching(msg.getSession().getAccountUuid())));
        }
        bus.reply(msg, reply);
    }

    private void handle(final APIAttachDataVolumeToVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                AttachDataVolumeToVmMsg amsg = new AttachDataVolumeToVmMsg();
                amsg.setVolume(getSelfInventory());
                amsg.setVmInstanceUuid(msg.getVmInstanceUuid());
                bus.makeTargetServiceIdByResourceUuid(amsg, VmInstanceConstant.SERVICE_ID, amsg.getVmInstanceUuid());
                bus.send(amsg, new CloudBusCallBack(msg, chain) {
                    @Override
                    public void run(MessageReply reply) {
                        final APIAttachDataVolumeToVmEvent evt = new APIAttachDataVolumeToVmEvent(msg.getId());
                        self = dbf.reload(self);
                        if (reply.isSuccess()) {
                            evt.setInventory(getSelfInventory());
                        } else {
                            if (self.getVmInstanceUuid() != null) {
                                self.setVmInstanceUuid(null);
                                dbf.update(self);
                            }

                            evt.setError(reply.getError());
                        }

                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return msg.getClass().getName();
            }
        });

    }

    private void handle(final APIDetachDataVolumeFromVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                refreshVO();
                DetachDataVolumeFromVmMsg dmsg = new DetachDataVolumeFromVmMsg();
                dmsg.setVolume(getSelfInventory());
                String vmUuid;
                if (msg.getVmUuid() != null) {
                    vmUuid = msg.getVmUuid();
                } else {
                    vmUuid = getSelfInventory().getVmInstanceUuid();
                }
                dmsg.setVmInstanceUuid(vmUuid);

                bus.makeTargetServiceIdByResourceUuid(dmsg, VmInstanceConstant.SERVICE_ID, vmUuid);
                bus.send(dmsg, new CloudBusCallBack(msg, chain) {
                    @Override
                    public void run(MessageReply reply) {
                        APIDetachDataVolumeFromVmEvent evt = new APIDetachDataVolumeFromVmEvent(msg.getId());
                        if (reply.isSuccess()) {
                            refreshVO();
                            evt.setInventory(getSelfInventory());
                        } else {
                            evt.setError(reply.getError());
                        }

                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return msg.getClass().getName();
            }
        });
    }

    protected VolumeInventory getSelfInventory() {
        return VolumeInventory.valueOf(self);
    }

    private void delete(boolean forceDelete, final Completion completion) {
        delete(forceDelete, true, completion);
    }

    // don't put this in queue, it will eventually send the VolumeDeletionMsg that will be in queue
    private void delete(boolean forceDelete, boolean detachBeforeDeleting, final Completion completion) {
        delete(forceDelete, null, detachBeforeDeleting, completion);
    }

    private void delete(boolean forceDelete, VolumeDeletionPolicy deletionPolicy, boolean detachBeforeDeleting, final Completion completion) {
        final String issuer = VolumeVO.class.getSimpleName();
        VolumeDeletionStruct struct = new VolumeDeletionStruct();
        struct.setInventory(getSelfInventory());
        struct.setDetachBeforeDeleting(detachBeforeDeleting);
        struct.setDeletionPolicy(deletionPolicy != null ? deletionPolicy.toString() : deletionPolicyMgr.getDeletionPolicy(self.getUuid()).toString());
        final List<VolumeDeletionStruct> ctx = list(struct);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("delete-data-volume");
        if (!forceDelete) {
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

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(APIDeleteDataVolumeMsg msg) {
        final APIDeleteDataVolumeEvent evt = new APIDeleteDataVolumeEvent(msg.getId());
        delete(msg.getDeletionMode() == DeletionMode.Enforcing, new Completion(msg) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errorCode, errorCode.getDetails()));
                bus.publish(evt);
            }
        });
    }

    private void handle(final VolumeCreateSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override

            public void run(final SyncTaskChain chain) {
                CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
                cmsg.setName(msg.getName());
                cmsg.setDescription(msg.getDescription());
                cmsg.setResourceUuid(msg.getResourceUuid());
                cmsg.setAccountUuid(msg.getAccountUuid());
                cmsg.setVolumeUuid(msg.getVolumeUuid());
                bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                bus.send(cmsg, new CloudBusCallBack(msg, chain) {
                    @Override
                    public void run(MessageReply reply) {
                        VolumeCreateSnapshotReply r = new VolumeCreateSnapshotReply();
                        if (reply.isSuccess()) {
                            CreateVolumeSnapshotReply creply = (CreateVolumeSnapshotReply) reply;
                            syncVolSize();
                            r.setInventory(creply.getInventory());
                        } else {
                            r.setError(reply.getError());
                        }

                        bus.reply(msg, r);
                        chain.next();
                    }

                    private void syncVolSize() {
                        SyncVolumeSizeMsg syncVolumeSizeMsg = new SyncVolumeSizeMsg();
                        syncVolumeSizeMsg.setVolumeUuid(msg.getVolumeUuid());
                        bus.makeTargetServiceIdByResourceUuid(syncVolumeSizeMsg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
                        bus.send(syncVolumeSizeMsg);
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("create-snapshot-for-volume-%s", self.getUuid());
            }
        });
    }

    private void handle(final APICreateVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(final SyncTaskChain taskChain) {
                FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                chain.setName("");

                chain.then(new NoRollbackFlow() {
                    String __name__ = String.format("create-snapshot-for-volume-%s", msg.getVolumeUuid());
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
                        cmsg.setName(msg.getName());
                        cmsg.setDescription(msg.getDescription());
                        cmsg.setResourceUuid(msg.getResourceUuid());
                        cmsg.setAccountUuid(msg.getSession().getAccountUuid());
                        cmsg.setVolumeUuid(msg.getVolumeUuid());
                        bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    CreateVolumeSnapshotReply creply = (CreateVolumeSnapshotReply) reply;
                                    tagMgr.createTagsFromAPICreateMessage(msg, creply.getInventory().getUuid(), VolumeSnapshotVO.class.getSimpleName());
                                    data.put("uuid", creply.getInventory().getUuid());
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = String.format("sync-volume[uuid: %s]-size-after-create-snapshot", msg.getVolumeUuid());
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SyncVolumeSizeMsg syncVolumeSizeMsg = new SyncVolumeSizeMsg();
                        syncVolumeSizeMsg.setVolumeUuid(msg.getVolumeUuid());
                        bus.makeTargetServiceIdByResourceUuid(syncVolumeSizeMsg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
                        bus.send(syncVolumeSizeMsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                trigger.next();
                            }
                        });
                    }
                }).done(new FlowDoneHandler(taskChain) {
                    @Override
                    public void handle(Map data) {
                        APICreateVolumeSnapshotEvent evt = new APICreateVolumeSnapshotEvent(msg.getId());
                        String snapshotUuid = (String)data.get("uuid");

                        evt.setInventory(VolumeSnapshotInventory.valueOf(dbf.findByUuid(snapshotUuid, VolumeSnapshotVO.class)));
                        bus.publish(evt);
                        taskChain.next();
                    }
                }).error(new FlowErrorHandler(taskChain) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        APICreateVolumeSnapshotEvent evt = new APICreateVolumeSnapshotEvent(msg.getId());
                        evt.setError(errCode);
                        bus.publish(evt);
                        taskChain.next();
                    }
                }).start();
            }

            @Override
            public String getName() {
                return String.format("create-snapshot-for-volume-%s", self.getUuid());
            }
        });
    }

    private void handle(APICreateVolumeSnapshotGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("create-volume-%s-snapshot-group", msg.getVolumeUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APICreateVolumeSnapshotGroupEvent evt = new APICreateVolumeSnapshotGroupEvent(msg.getId());
                doCreateVolumeSnapshotGroup(msg, new ReturnValueCompletion<VolumeSnapshotGroupInventory>(chain) {
                    @Override
                    public void success(VolumeSnapshotGroupInventory inv) {
                        evt.setInventory(inv);
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
                return getSyncSignature();
            }
        });
    }

    private void doCreateVolumeSnapshotGroup(CreateVolumeSnapshotGroupMessage msg, ReturnValueCompletion<VolumeSnapshotGroupInventory> completion) {
        final String SNAPSHOT_GROUP_INV = "SNAPSHOT_GROUP_INV";

        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("create-volume-%s-snapshot-group", msg.getRootVolumeUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "check-operation-on-primary-storage";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                Map<String, List<String>> psVolumeRef = msg.getVmInstance().getAllDiskVolumes().stream()
                        .collect(Collectors.groupingBy(VolumeInventory::getPrimaryStorageUuid,
                                Collectors.mapping(VolumeInventory::getUuid, Collectors.toList())));

                new While<>(psVolumeRef.entrySet()).each((e, c) -> {
                    CheckVolumeSnapshotOperationOnPrimaryStorageMsg cmsg = new CheckVolumeSnapshotOperationOnPrimaryStorageMsg();
                    cmsg.setPrimaryStorageUuid(e.getKey());
                    cmsg.setVolumeUuids(e.getValue());
                    cmsg.setVmInstanceUuid(msg.getVmInstance().getUuid());
                    cmsg.setOperation(msg.getBackendOperation());
                    bus.makeLocalServiceId(cmsg, PrimaryStorageConstant.SERVICE_ID);
                    bus.send(cmsg, new CloudBusCallBack(c) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                c.addError(reply.getError());
                                c.allDone();
                                return;
                            }

                            c.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.getCauses().isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(errorCodeList.getCauses().get(0));
                        }

                    }
                });
            }
        }).then(new Flow() {
            String __name__ = "create-memory-volume-if-with-memory";

            VolumeInventory memoryVolume = null;

            @Override
            public boolean skip(Map data) {
                return msg.getConsistentType() != ConsistentType.Application || Q.New(VolumeVO.class)
                        .eq(VolumeVO_.vmInstanceUuid, msg.getVmInstance().getUuid())
                        .eq(VolumeVO_.type, VolumeType.Memory)
                        .isExists();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                CreateVolumeMsg cmsg = new CreateVolumeMsg();
                cmsg.setAccountUuid(msg.getSession().getAccountUuid());
                cmsg.setSize(0);
                cmsg.setVmInstanceUuid(msg.getVmInstance().getUuid());
                cmsg.setPrimaryStorageUuid(msg.getVmInstance().getRootVolume().getPrimaryStorageUuid());
                cmsg.setName(String.format("memory-volume-of-vm-%s", msg.getVmInstance().getUuid()));
                cmsg.setVolumeType(VolumeType.Memory.toString());
                cmsg.setFormat(msg.getVmInstance().getRootVolume().getFormat());

                bus.makeLocalServiceId(cmsg, VolumeConstant.SERVICE_ID);
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        CreateVolumeReply r = reply.castReply();
                        memoryVolume = r.getInventory();
                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (memoryVolume == null) {
                    trigger.rollback();
                    return;
                }

                DeleteVolumeMsg dmsg = new DeleteVolumeMsg();
                dmsg.setDetachBeforeDeleting(false);
                dmsg.setUuid(memoryVolume.getUuid());
                dmsg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString());
                bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeConstant.SERVICE_ID, memoryVolume.getUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if(!reply.isSuccess()) {
                            logger.debug(String.format("failed to delete volume[uuid: %s]", memoryVolume.getUuid()));
                        }
                        trigger.rollback();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "instantiate-memory-volume-if-with-memory";

            VolumeVO volume;

            @Override
            public boolean skip(Map data) {
                if (msg.getConsistentType() != ConsistentType.Application) {
                    return true;
                }

                volume = Q.New(VolumeVO.class)
                        .eq(VolumeVO_.vmInstanceUuid, msg.getVmInstance().getUuid())
                        .eq(VolumeVO_.type, VolumeType.Memory).find();
                return volume == null || volume.getStatus().equals(VolumeStatus.Ready);
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                InstantiateMemoryVolumeMsg imsg = new InstantiateMemoryVolumeMsg();
                imsg.setHostUuid(msg.getVmInstance().getHostUuid());
                imsg.setPrimaryStorageUuid(msg.getVmInstance().getRootVolume().getPrimaryStorageUuid());
                imsg.setVolumeUuid(volume.getUuid());
                bus.makeTargetServiceIdByResourceUuid(imsg, VolumeConstant.SERVICE_ID, imsg.getVolumeUuid());
                bus.send(imsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        volume.setInstallPath(((InstantiateVolumeReply) reply).getVolume().getInstallPath());
                        volume.setDeviceId(Integer.MAX_VALUE);
                        volume.setStatus(VolumeStatus.Ready);
                        dbf.updateAndRefresh(volume);

                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "take-snapshots";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                msg.setVmInstance(VmInstanceInventory.valueOf(dbf.findByUuid(msg.getVmInstance().getUuid(), VmInstanceVO.class)));
                createSnapshotGroup(msg, new ReturnValueCompletion<VolumeSnapshotGroupInventory>(trigger) {
                    @Override
                    public void success(VolumeSnapshotGroupInventory returnValue) {
                        data.put(SNAPSHOT_GROUP_INV, returnValue);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success((VolumeSnapshotGroupInventory) data.get(SNAPSHOT_GROUP_INV));
            }
        }).start();
    }

    private void createSnapshotGroup(CreateVolumeSnapshotGroupMessage msg, ReturnValueCompletion<VolumeSnapshotGroupInventory> completion) {
        VolumeSnapshotGroupCreationValidator.validate(msg.getVmInstance().getUuid());
        CreateVolumesSnapshotMsg cmsg = new CreateVolumesSnapshotMsg();
        List<CreateVolumesSnapshotsJobStruct> volumesSnapshotsJobs = new ArrayList<>();
        cmsg.setAccountUuid(msg.getSession().getAccountUuid());

        VmInstanceInventory vm = msg.getVmInstance();
        Map<String, VolumeInventory> vols = vm.getAllVolumes().stream()
                .filter(it -> it.isDisk() || msg.getConsistentType() == ConsistentType.Application
                        && it.getType().equals(VolumeType.Memory.toString()))
                .collect(Collectors.toMap(VolumeInventory::getUuid, it -> it));

        for (VolumeInventory vol : vols.values()) {
            CreateVolumesSnapshotsJobStruct volumesSnapshotsJob = new CreateVolumesSnapshotsJobStruct();

            volumesSnapshotsJob.setVolumeUuid(vol.getUuid());
            volumesSnapshotsJob.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
            volumesSnapshotsJob.setResourceUuid(getUuid());
            volumesSnapshotsJob.setName(msg.getName() + "-" + vol.getName());
            volumesSnapshotsJob.setDescription(msg.getDescription());
            volumesSnapshotsJobs.add(volumesSnapshotsJob);
        }
        cmsg.setVolumeSnapshotJobs(volumesSnapshotsJobs);
        cmsg.setConsistentType(msg.getConsistentType());

        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeConstant.SERVICE_ID, msg.getRootVolumeUuid());
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }


                CreateVolumesSnapshotReply r = reply.castReply();
                VolumeSnapshotGroupVO group = createGroup(r);
                logger.debug(String.format("created volume snapshot group[uuid:%s] for vm[uuid:%s]",
                        group.getUuid(), vm.getUuid()));
                completion.success(VolumeSnapshotGroupInventory.valueOf(dbf.reload(group)));
            }

            private VolumeSnapshotGroupVO createGroup(CreateVolumesSnapshotReply r) {
                List<VolumeSnapshotGroupRefVO> refs = new ArrayList<>();
                VolumeSnapshotGroupVO group = new VolumeSnapshotGroupVO();
                if (msg.getResourceUuid() != null) {
                    group.setUuid(msg.getResourceUuid());
                } else {
                    group.setUuid(getUuid());
                }
                group.setSnapshotCount(cmsg.getVolumeSnapshotJobs().size());
                group.setName(msg.getName());
                group.setDescription(msg.getDescription());
                group.setVmInstanceUuid(vm.getUuid());
                group.setAccountUuid(msg.getSession().getAccountUuid());
                for (VolumeSnapshotInventory inv : r.getInventories()) {
                    VolumeSnapshotGroupRefVO ref = new VolumeSnapshotGroupRefVO();
                    ref.setVolumeUuid(inv.getVolumeUuid());
                    ref.setVolumeName(vols.get(inv.getVolumeUuid()).getName());
                    ref.setVolumeType(inv.getVolumeType());
                    ref.setVolumeSnapshotGroupUuid(group.getUuid());
                    ref.setVolumeSnapshotUuid(inv.getUuid());
                    ref.setVolumeSnapshotName(inv.getName());
                    ref.setVolumeSnapshotInstallPath(inv.getPrimaryStorageInstallPath());
                    ref.setDeviceId(vols.get(inv.getVolumeUuid()).getDeviceId());
                    refs.add(ref);
                }

                dbf.persist(group);
                dbf.persistCollection(refs);
                return group;
            }
        });
    }

    private void handle(APIChangeVolumeStateMsg msg) {
        VolumeStateEvent sevt = VolumeStateEvent.valueOf(msg.getStateEvent());
        if (sevt == VolumeStateEvent.enable) {
            self.setState(VolumeState.Enabled);
        } else {
            self.setState(VolumeState.Disabled);
        }
        self = dbf.updateAndRefresh(self);
        VolumeInventory inv = VolumeInventory.valueOf(self);
        APIChangeVolumeStateEvent evt = new APIChangeVolumeStateEvent(msg.getId());
        evt.setInventory(inv);
        bus.publish(evt);
    }

    class VolumeSize {
        long size;
        long actualSize;
    }
}
