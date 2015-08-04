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
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.*;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIDeleteMessage.DeletionMode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.CreateVolumeSnapshotMsg;
import org.zstack.header.storage.snapshot.CreateVolumeSnapshotReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    private VolumeVO self;

    public VolumeBase(VolumeVO vo) {
        self = vo;
    }

    protected void refreshVO() {
        self = dbf.reload(self);
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage)msg);
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
        } else if (msg instanceof CreateDataVolumeTemplateFromDataVolumeMsg) {
            handle((CreateDataVolumeTemplateFromDataVolumeMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final CreateDataVolumeTemplateFromDataVolumeMsg msg) {
        final CreateTemplateFromVolumeOnPrimaryStorageMsg cmsg = new CreateTemplateFromVolumeOnPrimaryStorageMsg();
        cmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        cmsg.setImageInventory(ImageInventory.valueOf(dbf.findByUuid(msg.getImageUuid(), ImageVO.class)));
        cmsg.setVolumeInventory(getSelfInventory());
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
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
            }
        });
    }

    private void handle(final DeleteVolumeMsg msg) {
        final DeleteVolumeReply reply = new DeleteVolumeReply();
        delete(true, msg.isDetachBeforeDeleting(), new Completion(msg) {
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

    private void handle(final VolumeDeletionMsg msg) {
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
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (self.getVmInstanceUuid() != null && self.getType() == VolumeType.Data && msg.isDetachBeforeDeleting()) {
                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("detach-volume-from-vm");

                        public void run(final FlowTrigger trigger, Map data) {
                            DetachDataVolumeFromVmMsg dmsg = new DetachDataVolumeFromVmMsg();
                            dmsg.setVolume(getSelfInventory());
                            bus.makeTargetServiceIdByResourceUuid(dmsg, VmInstanceConstant.SERVICE_ID, dmsg.getVmInstanceUuid());
                            bus.send(dmsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    trigger.next();
                                }
                            });
                        }
                    });
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-data-volume-from-primary-storage";

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
                                        logger.warn(String.format("failed to delete volume[uuid:%s, name:%s], %s", self.getUuid(), self.getName(), reply.getError()));
                                    }

                                    trigger.next();
                                }
                            });
                        } else {
                            trigger.next();
                        }
                    }
                });

                if (self.getPrimaryStorageUuid() != null) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "return-primary-storage-capacity";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            ReturnPrimaryStorageCapacityMsg rmsg = new ReturnPrimaryStorageCapacityMsg();
                            rmsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
                            rmsg.setDiskSize(self.getSize());
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                            bus.send(rmsg);
                            trigger.next();
                        }
                    });
                }


                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        dbf.remove(self);
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
                        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeDeletionExtensionPoint.class), new ForEachFunction<VolumeDeletionExtensionPoint>() {
                            @Override
                            public void run(VolumeDeletionExtensionPoint arg) {
                                arg.failedToDeleteVolume(getSelfInventory(), errCode);
                            }
                        });

                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
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
        } else {
            bus.dealWithUnknownMessage(msg);
        }
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


    @Transactional(readOnly = true)
    private List<VmInstanceVO> getCandidateVmForAttaching(String accountUuid) {
        List<String> vmUuids = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, VmInstanceVO.class);

        if (vmUuids != null && vmUuids.isEmpty()) {
            return new ArrayList<VmInstanceVO>();
        }

        TypedQuery<VmInstanceVO> q = null;
        String sql;
        if (vmUuids == null) {
            // all vms
            if (self.getStatus() == VolumeStatus.Ready) {
                sql = "select vm from VmInstanceVO vm, PrimaryStorageClusterRefVO ref, VolumeVO vol where vm.state in (:vmStates) and vol.uuid = :volUuid and vm.hypervisorType in (:hvTypes) and vm.clusterUuid = ref.clusterUuid and ref.primaryStorageUuid = vol.primaryStorageUuid group by vm.uuid";
                q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
                q.setParameter("volUuid", self.getUuid());
                List<String> hvTypes = VolumeFormat.valueOf(self.getFormat()).getHypervisorTypesSupportingThisVolumeFormatInString();
                q.setParameter("hvTypes", hvTypes);
            } else if (self.getStatus() == VolumeStatus.NotInstantiated) {
                sql = "select vm from VmInstanceVO vm where vm.state in (:vmStates) group by vm.uuid";
                q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
            } else {
                DebugUtils.Assert(false, String.format("should not reach here, volume[uuid:%s]", self.getUuid()));
            }
        } else {
            if (self.getStatus() == VolumeStatus.Ready) {
                sql = "select vm from VmInstanceVO vm, PrimaryStorageClusterRefVO ref, VolumeVO vol where vm.uuid in (:vmUuids) and vm.state in (:vmStates) and vol.uuid = :volUuid and vm.hypervisorType in (:hvTypes) and vm.clusterUuid = ref.clusterUuid and ref.primaryStorageUuid = vol.primaryStorageUuid group by vm.uuid";
                q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
                q.setParameter("volUuid", self.getUuid());
                List<String> hvTypes = VolumeFormat.valueOf(self.getFormat()).getHypervisorTypesSupportingThisVolumeFormatInString();
                q.setParameter("hvTypes", hvTypes);
            } else if (self.getStatus() == VolumeStatus.NotInstantiated) {
                sql = "select vm from VmInstanceVO vm where vm.uuid in (:vmUuids) and vm.state in (:vmStates) group by vm.uuid";
                q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
            } else {
                DebugUtils.Assert(false, String.format("should not reach here, volume[uuid:%s]", self.getUuid()));
            }

            q.setParameter("vmUuids", vmUuids);
        }


        q.setParameter("vmStates", Arrays.asList(VmInstanceState.Running, VmInstanceState.Stopped));
        return q.getResultList();
    }

    private void handle(APIGetDataVolumeAttachableVmMsg msg) {
        APIGetDataVolumeAttachableVmReply reply = new APIGetDataVolumeAttachableVmReply();
        reply.setInventories(VmInstanceInventory.valueOf(getCandidateVmForAttaching(msg.getSession().getAccountUuid())));
        bus.reply(msg, reply);
    }

    private void handle(final APIAttachDataVolumeToVmMsg msg) {
        self.setVmInstanceUuid(msg.getVmInstanceUuid());
        self = dbf.updateAndRefresh(self);

        AttachDataVolumeToVmMsg amsg = new AttachDataVolumeToVmMsg();
        amsg.setVolume(getSelfInventory());
        amsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        bus.makeTargetServiceIdByResourceUuid(amsg, VmInstanceConstant.SERVICE_ID, amsg.getVmInstanceUuid());
        bus.send(amsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                final APIAttachDataVolumeToVmEvent evt = new APIAttachDataVolumeToVmEvent(msg.getId());
                self = dbf.reload(self);
                if (reply.isSuccess()) {
                    AttachDataVolumeToVmReply ar = reply.castReply();
                    self.setVmInstanceUuid(msg.getVmInstanceUuid());
                    self.setFormat(VolumeFormat.getVolumeFormatByMasterHypervisorType(ar.getHypervisorType()).toString());
                    self  = dbf.updateAndRefresh(self);

                    evt.setInventory(getSelfInventory());
                } else {
                    self.setVmInstanceUuid(null);
                    dbf.update(self);
                    evt.setErrorCode(reply.getError());
                }

                bus.publish(evt);
            }
        });
    }

    private void handle(final APIDetachDataVolumeFromVmMsg msg) {
        DetachDataVolumeFromVmMsg dmsg = new DetachDataVolumeFromVmMsg();
        dmsg.setVolume(getSelfInventory());
        bus.makeTargetServiceIdByResourceUuid(dmsg, VmInstanceConstant.SERVICE_ID, dmsg.getVmInstanceUuid());
        bus.send(dmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                APIDetachDataVolumeFromVmEvent evt = new APIDetachDataVolumeFromVmEvent(msg.getId());
                if (reply.isSuccess()) {
                    self.setVmInstanceUuid(null);
                    self.setDeviceId(null);
                    self = dbf.updateAndRefresh(self);
                    evt.setInventory(getSelfInventory());
                } else {
                    evt.setErrorCode(reply.getError());
                }

                bus.publish(evt);
            }
        });
    }

    protected VolumeInventory getSelfInventory() {
        return VolumeInventory.valueOf(self);
    }

    private void delete(boolean forceDelete, final Completion completion) {
        delete(forceDelete, true, completion);
    }

    private void delete(boolean forceDelete, boolean detachBeforeDeleting, final Completion completion) {
        final String issuer = VolumeVO.class.getSimpleName();
        VolumeDeletionStruct struct = new VolumeDeletionStruct(getSelfInventory());
        struct.setDetachBeforeDeleting(detachBeforeDeleting);
        final List<VolumeDeletionStruct> ctx = list(struct);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-data-volume"));
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
                evt.setErrorCode(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errorCode));
                bus.publish(evt);
            }
        });
    }

    private void handle(final APICreateVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("create-snapshot-for-volume-%s", self.getUuid());
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
                bus.send(cmsg, new CloudBusCallBack(chain) {
                    @Override
                    public void run(MessageReply reply) {
                        APICreateVolumeSnapshotEvent evt = new APICreateVolumeSnapshotEvent(msg.getId());
                        if (reply.isSuccess()) {
                            CreateVolumeSnapshotReply creply = (CreateVolumeSnapshotReply) reply;
                            evt.setInventory(creply.getInventory());

                            tagMgr.createTagsFromAPICreateMessage(msg, creply.getInventory().getUuid(), VolumeSnapshotVO.class.getSimpleName());
                        } else {
                            evt.setErrorCode(reply.getError());
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
}
