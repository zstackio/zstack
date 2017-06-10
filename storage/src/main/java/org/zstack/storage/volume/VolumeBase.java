package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
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
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.header.volume.VolumeConstant.Capability;
import org.zstack.header.volume.VolumeDeletionPolicyManager.VolumeDeletionPolicy;
import org.zstack.identity.AccountManager;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

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
        } else if (msg instanceof CreateDataVolumeTemplateFromDataVolumeMsg) {
            handle((CreateDataVolumeTemplateFromDataVolumeMsg) msg);
        } else if (msg instanceof ExpungeVolumeMsg) {
            handle((ExpungeVolumeMsg) msg);
        } else if (msg instanceof RecoverVolumeMsg) {
            handle((RecoverVolumeMsg) msg);
        } else if (msg instanceof SyncVolumeSizeMsg) {
            handle((SyncVolumeSizeMsg) msg);
        } else if (msg instanceof InstantiateVolumeMsg) {
            handle((InstantiateVolumeMsg) msg);
        } else if (msg instanceof OverlayMessage) {
            handle((OverlayMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
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

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                            amsg.setRequiredPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                            amsg.setSize(self.getSize());
                            bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                    } else {
                                        success = true;
                                        trigger.next();
                                    }
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            if (success) {
                                IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                                imsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                                imsg.setDiskSize(self.getSize());
                                bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                                bus.send(imsg);
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
                            if (msg instanceof InstantiateRootVolumeMsg) {
                                instantiateRootVolume((InstantiateRootVolumeMsg) msg, trigger);
                            } else {
                                instantiateDataVolume(msg, trigger);
                            }

                        }
                    }

                    private void instantiateRootVolume(InstantiateRootVolumeMsg msg, FlowTrigger trigger) {
                        InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg imsg = new InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg();
                        imsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                        imsg.setVolume(getSelfInventory());
                        if (msg.getHostUuid() != null) {
                            imsg.setDestHost(HostInventory.valueOf(dbf.findByUuid(msg.getHostUuid(), HostVO.class)));
                        }
                        imsg.setSystemTags(msg.getSystemTags());
                        imsg.setTemplateSpec(msg.getTemplateSpec());
                        bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
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
                                trigger.next();
                            }
                        });
                    }

                    private void instantiateDataVolume(InstantiateVolumeMsg msg, FlowTrigger trigger) {
                        InstantiateVolumeOnPrimaryStorageMsg imsg = new InstantiateVolumeOnPrimaryStorageMsg();
                        imsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                        imsg.setVolume(getSelfInventory());
                        imsg.setSystemTags(msg.getSystemTags());
                        if (msg.getHostUuid() != null) {
                            imsg.setDestHost(HostInventory.valueOf(dbf.findByUuid(msg.getHostUuid(), HostVO.class)));
                        }
                        bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
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

    private void handle(final RecoverVolumeMsg msg) {
        final RecoverVolumeReply reply = new RecoverVolumeReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
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
            throw new OperationFailureException(operr("the volume[uuid:%s, name:%s] is not deleted yet, can't expunge it",
                            self.getUuid(), self.getName()));
        }

        final VolumeInventory inv = getSelfInventory();
        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeBeforeExpungeExtensionPoint.class),
                new ForEachFunction<VolumeBeforeExpungeExtensionPoint>() {
                    @Override
                    public void run(VolumeBeforeExpungeExtensionPoint arg) {
                        arg.volumeBeforeExpunge(inv);
                    }
                });

        if (self.getPrimaryStorageUuid() != null) {
            DeleteVolumeOnPrimaryStorageMsg dmsg = new DeleteVolumeOnPrimaryStorageMsg();
            dmsg.setVolume(getSelfInventory());
            dmsg.setUuid(self.getPrimaryStorageUuid());
            bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
            bus.send(dmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply r) {
                    if (!r.isSuccess()) {
                        completion.fail(r.getError());
                    } else {
                        IncreasePrimaryStorageCapacityMsg msg = new IncreasePrimaryStorageCapacityMsg();
                        msg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
                        msg.setDiskSize(self.getSize());
                        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                        bus.send(msg);


                        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeAfterExpungeExtensionPoint.class),
                                new ForEachFunction<VolumeAfterExpungeExtensionPoint>() {
                                    @Override
                                    public void run(VolumeAfterExpungeExtensionPoint arg) {
                                        arg.volumeAfterExpunge(inv);
                                    }
                                });

                        dbf.remove(self);
                        completion.success();
                    }
                }
            });
        } else {
            CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeAfterExpungeExtensionPoint.class),
                    new ForEachFunction<VolumeAfterExpungeExtensionPoint>() {
                        @Override
                        public void run(VolumeAfterExpungeExtensionPoint arg) {
                            arg.volumeAfterExpunge(inv);
                        }
                    });

            dbf.remove(self);
            completion.success();
        }
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

    private void handle(final CreateDataVolumeTemplateFromDataVolumeMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                doCreateDataVolumeTemplateFromDataVolumeMsg(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return CreateDataVolumeTemplateFromDataVolumeMsg.class.getName();
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
                if (self.getVmInstanceUuid() != null && self.getType() == VolumeType.Data && msg.isDetachBeforeDeleting()) {
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
                                    self.setVmInstanceUuid(null);
                                    self = dbf.updateAndRefresh(self);
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
                            if (self.getStatus() == VolumeStatus.Ready) {
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

                                            if(reply.getError().getCode().equals(PrimaryStorageErrors.ALLOCATE_ERROR.toString())){
                                                PrimaryStorageVO psv = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, self.getPrimaryStorageUuid()).find();
                                                DeleteVolumeGC gc = new DeleteVolumeGC();
                                                gc.NAME = String.format("gc-volume-%s-on-primary-storage-%s", self.getUuid(), psv.getUuid());
                                                gc.primaryStorageUuid = psv.getUuid();
                                                gc.volumeInventory = VolumeInventory.valueOf(self);
                                                gc.submit();
                                            }
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


                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        VolumeStatus oldStatus = self.getStatus();

                        if (deletionPolicy == VolumeDeletionPolicy.Direct) {
                            self.setStatus(VolumeStatus.Deleted);
                            self = dbf.updateAndRefresh(self);
                            new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, getSelfInventory());
                            dbf.remove(self);
                        } else if (deletionPolicy == VolumeDeletionPolicy.Delay) {
                            self.setStatus(VolumeStatus.Deleted);
                            self = dbf.updateAndRefresh(self);
                            new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, getSelfInventory());
                        } else if (deletionPolicy == VolumeDeletionPolicy.Never) {
                            self.setStatus(VolumeStatus.Deleted);
                            self = dbf.updateAndRefresh(self);
                            new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, getSelfInventory());
                        } else if (deletionPolicy == VolumeDeletionPolicy.DBOnly) {
                            new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, getSelfInventory());
                            dbf.remove(self);
                        } else {
                            throw new CloudRuntimeException(String.format("Invalid deletionPolicy:%s", deletionPolicy));
                        }


                        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeDeletionExtensionPoint.class), new ForEachFunction<VolumeDeletionExtensionPoint>() {
                            @Override
                            public void run(VolumeDeletionExtensionPoint arg) {
                                arg.afterDeleteVolume(getSelfInventory());
                            }
                        });
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

    private void handle(final VolumeDeletionMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                self = dbf.reload(self);
                if (self.getStatus() == VolumeStatus.Deleted) {
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

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeVolumeStateMsg) {
            handle((APIChangeVolumeStateMsg) msg);
        } else if (msg instanceof APICreateVolumeSnapshotMsg) {
            handle((APICreateVolumeSnapshotMsg) msg);
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
        for (RecoverDataVolumeExtensionPoint ext : exts) {
            ext.preRecoverDataVolume(vol);
        }

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

    private List<VmInstanceVO> getCandidateVmForAttaching(String accountUuid) {

        return new SQLBatchWithReturn<List<VmInstanceVO>>(){
            @Override
            protected List<VmInstanceVO> scripts() {

                List<String> vmUuids = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, VmInstanceVO.class);

                if (vmUuids != null && vmUuids.isEmpty()) {
                    return new ArrayList<>();
                }

                SQL sql = null;
                if (vmUuids == null) {
                    // check all vms
                    if (self.getStatus() == VolumeStatus.Ready) {
                        List<String> hvTypes = VolumeFormat.valueOf(self.getFormat()).getHypervisorTypesSupportingThisVolumeFormatInString();
                        sql = SQL.New("select vm " +
                                "from VmInstanceVO vm, PrimaryStorageClusterRefVO ref, VolumeVO vol " +
                                "where vm.state in (:vmStates) " +
                                "and vol.uuid = :volUuid " +
                                "and vm.hypervisorType in (:hvTypes) " +
                                "and vm.clusterUuid = ref.clusterUuid " +
                                "and ref.primaryStorageUuid = vol.primaryStorageUuid " +
                                "group by vm.uuid")
                                .param("volUuid", self.getUuid())
                                .param("hvTypes", hvTypes);
                    } else if (self.getStatus() == VolumeStatus.NotInstantiated) {
                        //not support vmtx volume temporarily, so filter ESX vm when volume is NotInstantiated.
                        sql = SQL.New("select vm " +
                                "from VmInstanceVO vm,PrimaryStorageClusterRefVO ref,PrimaryStorageEO ps " +
                                "where vm.state in (:vmStates) " +
                                "and vm.hypervisorType <> :hvType  " +
                                "and vm.clusterUuid = ref.clusterUuid " +
                                "and ref.primaryStorageUuid = ps.uuid " +
                                "and ps.state in (:psState) " +
                                "group by vm.uuid")
                                //TODO:  this is a dirty fix, delete it when VMWare support DataVolume
                                .param("hvType", "ESX")
                                .param("psState",PrimaryStorageState.Enabled);
                    } else {
                        DebugUtils.Assert(false, String.format("should not reach here, volume[uuid:%s]", self.getUuid()));
                    }

                } else {
                    //check vms that belong to the account
                    if (self.getStatus() == VolumeStatus.Ready) {
                        List<String> hvTypes = VolumeFormat.valueOf(self.getFormat()).getHypervisorTypesSupportingThisVolumeFormatInString();
                        sql = SQL.New("select vm "+
                                "from VmInstanceVO vm, PrimaryStorageClusterRefVO ref, VolumeVO vol " +
                                "where vm.uuid in (:vmUuids) " +
                                "and vm.state in (:vmStates) " +
                                "and vol.uuid = :volUuid " +
                                "and vm.hypervisorType in (:hvTypes) " +
                                "and vm.clusterUuid = ref.clusterUuid " +
                                "and ref.primaryStorageUuid = vol.primaryStorageUuid " +
                                "group by vm.uuid")
                                .param("volUuid", self.getUuid())
                                .param("hvTypes", hvTypes);
                    } else if (self.getStatus() == VolumeStatus.NotInstantiated) {
                        sql = SQL.New("select vm " +
                                "from VmInstanceVO vm,PrimaryStorageClusterRefVO ref,PrimaryStorageEO ps " +
                                "where vm.uuid in (:vmUuids) " +
                                "and vm.state in (:vmStates) " +
                                "and vm.clusterUuid = ref.clusterUuid " +
                                "and ref.primaryStorageUuid = ps.uuid " +
                                "and ps.state in (:psState) " +
                                "group by vm.uuid")
                                .param("psState",PrimaryStorageState.Enabled);
                    } else {
                        DebugUtils.Assert(false, String.format("should not reach here, volume[uuid:%s]", self.getUuid()));
                    }

                    sql.param("vmUuids", vmUuids);
                }
                List<VmInstanceVO> ret = sql.param("vmStates", Arrays.asList(VmInstanceState.Running, VmInstanceState.Stopped)).list();
                if (ret.isEmpty()) {
                    return ret;
                }

                //the vm doesn't suport to online attach volume when  image platform type is other
                List<String> exclude = sql("select vm.uuid" +
                        " from VmInstanceVO vm, ImageVO image" +
                        " where vm.uuid in :vmUuids" +
                        " and vm.imageUuid = image.uuid" +
                        " and image.platform = :platformType" +
                        " and vm.state != :vmState")
                        .param("vmUuids",ret.stream().map(VmInstanceVO::getUuid).collect(Collectors.toList()))
                        .param("vmState", VmInstanceState.Stopped)
                        .param("platformType", ImagePlatform.Other).list();
                ret = ret.stream().filter(vm -> !exclude.contains(vm.getUuid())).collect(Collectors.toList());
                if (ret.isEmpty()) {
                    return ret;
                }

                VolumeInventory vol = getSelfInventory();
                for (VolumeGetAttachableVmExtensionPoint ext : pluginRgty.getExtensionList(VolumeGetAttachableVmExtensionPoint.class)) {
                    ret = ext.returnAttachableVms(vol, ret);
                }

                return ret;
            }
        }.execute();

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
                self = dbf.reload(self);
                self.setVmInstanceUuid(msg.getVmInstanceUuid());
                self = dbf.updateAndRefresh(self);

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
                            AttachDataVolumeToVmReply ar = reply.castReply();
                            self.setVmInstanceUuid(msg.getVmInstanceUuid());
                            self.setFormat(VolumeFormat.getVolumeFormatByMasterHypervisorType(ar.getHypervisorType()).toString());
                            self = dbf.updateAndRefresh(self);

                            evt.setInventory(getSelfInventory());
                        } else {
                            self.setVmInstanceUuid(null);
                            dbf.update(self);
                            evt.setError(reply.getError());
                        }

                        if (self.isShareable()) {
                            self.setVmInstanceUuid(null);
                            dbf.update(self);
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
                            self.setVmInstanceUuid(null);
                            self.setDeviceId(null);
                            self = dbf.updateAndRefresh(self);
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
                evt.setError(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errorCode));
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
                            r.setInventory(creply.getInventory());
                        } else {
                            r.setError(reply.getError());
                        }

                        bus.reply(msg, r);
                        chain.next();
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
            public void run(final SyncTaskChain chain) {
                CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
                cmsg.setName(msg.getName());
                cmsg.setDescription(msg.getDescription());
                cmsg.setResourceUuid(msg.getResourceUuid());
                cmsg.setAccountUuid(msg.getSession().getAccountUuid());
                cmsg.setVolumeUuid(msg.getVolumeUuid());
                bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                bus.send(cmsg, new CloudBusCallBack(msg, chain) {
                    @Override
                    public void run(MessageReply reply) {
                        APICreateVolumeSnapshotEvent evt = new APICreateVolumeSnapshotEvent(msg.getId());
                        if (reply.isSuccess()) {
                            CreateVolumeSnapshotReply creply = (CreateVolumeSnapshotReply) reply;
                            evt.setInventory(creply.getInventory());

                            tagMgr.createTagsFromAPICreateMessage(msg, creply.getInventory().getUuid(), VolumeSnapshotVO.class.getSimpleName());
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
                return String.format("create-snapshot-for-volume-%s", self.getUuid());
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
