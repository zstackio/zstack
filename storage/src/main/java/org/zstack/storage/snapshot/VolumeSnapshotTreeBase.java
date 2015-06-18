package org.zstack.storage.snapshot;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg.SnapshotDownloadInfo;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotReply.CreateTemplateFromVolumeSnapshotResult;
import org.zstack.header.storage.snapshot.VolumeSnapshotStatus.StatusEvent;
import org.zstack.header.storage.snapshot.VolumeSnapshotTree.SnapshotLeaf;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.function.FunctionNoArg;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.message.OperationChecker;

import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;

import static org.zstack.utils.StringDSL.ln;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeSnapshotTreeBase {
    private static CLogger logger = Utils.getLogger(VolumeSnapshotTreeBase.class);

    protected VolumeSnapshotVO currentRoot;
    protected SnapshotLeaf currentLeaf;
    protected VolumeSnapshotTree fullTree;

    protected String syncSignature;

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CascadeFacade casf;
    protected static OperationChecker allowedStatus = new OperationChecker(true);

    static {
        allowedStatus.addState(VolumeSnapshotStatus.Ready,
                VolumeSnapshotDeletionMsg.class.getName(),
                BackupVolumeSnapshotMsg.class.getName(),
                CreateDataVolumeFromVolumeSnapshotMsg.class.getName(),
                CreateTemplateFromVolumeSnapshotMsg.class.getName(),
                VolumeSnapshotBackupStorageDeletionMsg.class.getName(),
                APIRevertVolumeFromSnapshotMsg.class.getName(),
                APIDeleteVolumeSnapshotFromBackupStorageMsg.class.getName(),
                APIBackupVolumeSnapshotMsg.class.getName()
        );

        allowedStatus.addState(VolumeSnapshotStatus.Deleting,
                VolumeSnapshotDeletionMsg.class.getName());

        allowedStatus.addState(VolumeSnapshotStatus.Creating,
                VolumeSnapshotDeletionMsg.class.getName());
    }

    private ErrorCode isOperationAllowed(Message msg) {
        if (allowedStatus.isOperationAllowed(msg.getClass().getName(), currentRoot.getStatus().toString())) {
            return null;
        } else {
            return errf.instantiateErrorCode(VolumeSnapshotErrors.NOT_IN_CORRECT_STATE,
                    String.format("snapshot[uuid:%s, name:%s]'s status[%s] is not allowed for message[%s], allowed status%s",
                            currentRoot.getUuid(), currentRoot.getName(), currentRoot.getStatus(), msg.getClass().getName(), allowedStatus.getStatesForOperation(msg.getClass().getName())));
        }
    }

    private void refreshVO()  {
        VolumeSnapshotVO vo = dbf.reload(currentRoot);
        if (vo == null) {
            throw new OperationFailureException(errf.stringToOperationError(String.format("cannot find volume snapshot[uuid:%s, name:%s], it may have been deleted by previous operation",
                    currentRoot.getUuid(), currentRoot.getName())));
        }

        currentRoot = vo;
        buildFullSnapshotTree();
        currentLeaf = fullTree.findSnapshot(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                return arg.getUuid().equals(currentRoot.getUuid());
            }
        });
    }

    private VolumeSnapshotInventory getSelfInventory() {
        return VolumeSnapshotInventory.valueOf(currentRoot);
    }

    private void buildFullSnapshotTree() {
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.treeUuid, SimpleQuery.Op.EQ, currentRoot.getTreeUuid());
        List<VolumeSnapshotVO> vos = q.list();

        fullTree = VolumeSnapshotTree.fromVOs(vos);
    }

    public VolumeSnapshotTreeBase(VolumeSnapshotVO vo, boolean syncOnVolume) {
        currentRoot = vo;
        if (syncOnVolume) {
            syncSignature = String.format("volume.snapshot.volume.%s", currentRoot.getVolumeUuid());
        } else {
            syncSignature = String.format("volume.snapshot.tree.%s", currentRoot.getTreeUuid());
        }
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateTemplateFromVolumeSnapshotMsg) {
            handle((CreateTemplateFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof CreateDataVolumeFromVolumeSnapshotMsg) {
            handle((CreateDataVolumeFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof VolumeSnapshotDeletionMsg) {
            handle((VolumeSnapshotDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final VolumeSnapshotDeletionMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                deletion(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-volume-snapshot-%s", currentRoot.getUuid());
            }
        });
    }

    private void deletion(final VolumeSnapshotDeletionMsg msg, final NoErrorCompletion completion) {
        final VolumeSnapshotDeletionReply reply = new VolumeSnapshotDeletionReply();

        try {
            refreshVO();
        } catch (OperationFailureException e) {
            // the snapshot has been deleted
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            reply.setError(err);
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-snapshot-%s", currentRoot.getUuid()));
        chain.allowEmptyFlow();

        boolean ancestorOfLatest = false;
        for (VolumeSnapshotInventory inv : currentLeaf.getDescendants()) {
            if (inv.isLatest()) {
                ancestorOfLatest =  true;
                break;
            }
        }

        chain.then(new Flow() {
            String __name__ = String.format("change-volume-snapshot-status-%s", VolumeSnapshotStatus.Deleting);

            public void run(final FlowTrigger trigger, Map data) {
                changeStatusOfSnapshots(StatusEvent.delete, currentLeaf.getDescendants(), new Completion(trigger) {
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

            @Override
            public void rollback(final FlowTrigger trigger, Map data) {
                changeStatusOfSnapshots(StatusEvent.ready, currentLeaf.getDescendants(), new Completion(trigger) {
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
        });

        if (!msg.isVolumeDeletion()) {
            // this deletion is caused by snapshot deletion, check if merge need
            SimpleQuery<VolumeSnapshotTreeVO> tq  = dbf.createQuery(VolumeSnapshotTreeVO.class);
            tq.select(VolumeSnapshotTreeVO_.current);
            tq.add(VolumeSnapshotTreeVO_.uuid, Op.EQ, currentRoot.getTreeUuid());
            Boolean onCurrentTree = tq.findValue();

            boolean needMerge = onCurrentTree && ancestorOfLatest && currentRoot.getPrimaryStorageUuid() != null && VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString().equals(currentRoot.getType());
            if (needMerge) {
                chain.then(new NoRollbackFlow() {
                    String __name__ = "merge-volume-snapshots-to-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        MergeVolumeSnapshotOnPrimaryStorageMsg mmsg = new MergeVolumeSnapshotOnPrimaryStorageMsg();
                        VolumeSnapshotInventory from = currentLeaf.getParent() == null ? currentLeaf.getInventory() : currentLeaf.getParent().getInventory();
                        mmsg.setFrom(from);
                        VolumeVO vol = dbf.findByUuid(currentRoot.getVolumeUuid(), VolumeVO.class);
                        mmsg.setTo(VolumeInventory.valueOf(vol));
                        mmsg.setFullRebase(currentLeaf.getParent() == null);
                        bus.makeTargetServiceIdByResourceUuid(mmsg, PrimaryStorageConstant.SERVICE_ID, currentRoot.getPrimaryStorageUuid());
                        bus.send(mmsg, new CloudBusCallBack(trigger) {
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
            }

            chain.then(new NoRollbackFlow() {
                String __name__ = "delete-volume-snapshots-from-backup-storage";

                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    final List<VolumeSnapshotBackupStorageDeletionMsg> dmsgs = makeVolumeSnapshotBackupStorageDeletionMsg(null);
                    if (dmsgs.isEmpty()) {
                        trigger.next();
                        return;
                    }

                    bus.send(dmsgs, VolumeSnapshotGlobalConfig.SNAPSHOT_DELETE_PARALLELISM_DEGREE.value(Integer.class), new CloudBusListCallBack(trigger) {
                        @Override
                        public void run(List<MessageReply> replies) {
                            for (MessageReply r : replies) {
                                if (!r.isSuccess()) {
                                    VolumeSnapshotBackupStorageDeletionMsg dmsg = dmsgs.get(replies.indexOf(r));
                                    logger.warn(String.format("failed to delete snapshot[uuid:%s] on backup storage[uuids: %s], the backup storage should cleanup",
                                            dmsg.getSnapshotUuid(), dmsg.getBackupStorageUuids()));
                                }
                            }

                            trigger.next();
                        }
                    });
                }
            });
        }

        chain.then(new NoRollbackFlow() {
            String __name__ = "delete-volume-snapshots-from-primary-storage";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                final List<VolumeSnapshotPrimaryStorageDeletionMsg> pmsgs = CollectionUtils.transformToList(currentLeaf.getDescendants(), new Function<VolumeSnapshotPrimaryStorageDeletionMsg, VolumeSnapshotInventory>() {
                    @Override
                    public VolumeSnapshotPrimaryStorageDeletionMsg call(VolumeSnapshotInventory arg) {
                        if (arg.getPrimaryStorageUuid() == null) {
                            return null;
                        }

                        VolumeSnapshotPrimaryStorageDeletionMsg pmsg = new VolumeSnapshotPrimaryStorageDeletionMsg();
                        pmsg.setUuid(arg.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(pmsg, VolumeSnapshotConstant.SERVICE_ID, arg.getPrimaryStorageUuid());
                        return pmsg;
                    }
                });

                if (pmsgs.isEmpty()) {
                    trigger.next();
                    return;
                }

                bus.send(pmsgs, VolumeSnapshotGlobalConfig.SNAPSHOT_DELETE_PARALLELISM_DEGREE.value(Integer.class), new CloudBusListCallBack(trigger) {
                    @Override
                    public void run(List<MessageReply> replies) {
                        for (MessageReply r : replies) {
                            if (!r.isSuccess()) {
                                VolumeSnapshotPrimaryStorageDeletionMsg pmsg = pmsgs.get(replies.indexOf(r));
                                logger.warn(String.format("failed to delete snapshot[uuid:%s] on primary storage[uuid:%s], the primary storage should cleanup",
                                        pmsg.getSnapshotUuid(), currentRoot.getPrimaryStorageUuid()));
                            }
                        }

                        trigger.next();
                    }
                });
            }
        });

        final boolean finalAncestorOfLatest = ancestorOfLatest;
        chain.done(new FlowDoneHandler(msg, completion) {
            @Override
            public void handle(Map data) {
                if (msg.isVolumeDeletion()) {
                    new Runnable() {
                        @Override
                        @Transactional
                        public void run() {
                            String sql = "update VolumeSnapshotTreeVO tree set tree.volumeUuid = NULL where tree.volumeUuid = :volUuid";
                            Query q = dbf.getEntityManager().createQuery(sql);
                            q.setParameter("volUuid", currentRoot.getVolumeUuid());
                            q.executeUpdate();

                            sql = "update VolumeSnapshotVO s set s.volumeUuid = NULL where s.volumeUuid = :volUuid";
                            q = dbf.getEntityManager().createQuery(sql);
                            q.setParameter("volUuid", currentRoot.getVolumeUuid());
                            q.executeUpdate();
                        }
                    }.run();
                }

                if (!msg.isVolumeDeletion() && finalAncestorOfLatest && currentRoot.getParentUuid() != null) {
                    // reset latest
                    VolumeSnapshotVO vo = dbf.findByUuid(currentRoot.getParentUuid(), VolumeSnapshotVO.class);
                    vo.setLatest(true);
                    dbf.update(vo);
                    logger.debug(String.format("reset latest snapshot of tree[uuid:%s] to snapshot[uuid:%s]",
                            currentRoot.getTreeUuid(), currentRoot.getParentUuid()));
                }


                if (!cleanup()) {
                    changeStatusOfSnapshots(StatusEvent.ready, currentLeaf.getDescendants(), new Completion(msg, completion) {
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
                } else {
                    bus.reply(msg, reply);
                    completion.done();
                }
            }
        }).error(new FlowErrorHandler(msg, completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
                completion.done();
            }
        }).start();
    }

    private void handle(final CreateDataVolumeFromVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                createDataVolume(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("create-data-volume-from-snapshot-%s", currentRoot.getUuid());
            }
        });
    }

    private void createDataVolume(final CreateDataVolumeFromVolumeSnapshotMsg msg, final NoErrorCompletion completion) {
        final CreateDataVolumeFromVolumeSnapshotReply reply = new CreateDataVolumeFromVolumeSnapshotReply();

        refreshVO();
        ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            reply.setError(err);
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        final CreateBitsFromSnapshotInfoForDataVolume info = new CreateBitsFromSnapshotInfoForDataVolume(prepareCreateBitsFromSnapshotInfo(msg.getPrimaryStorageUuid()));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-data-volume-from-snapshot-%s", currentRoot.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (info.needDownload) {
                    flow(new Flow() {
                        String __name__ = "allocateHost-workspace-primary-storage";
                        boolean success;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                            amsg.setPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                            amsg.setSize(info.neededSizeOnWorkspacePrimaryStorage);
                            bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        success = true;
                                        trigger.next();
                                    } else {
                                        trigger.fail(reply.getError());
                                    }
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowTrigger trigger, Map data) {
                            if (success) {
                                ReturnPrimaryStorageCapacityMsg rmsg = new ReturnPrimaryStorageCapacityMsg();
                                rmsg.setDiskSize(info.neededSizeOnWorkspacePrimaryStorage);
                                rmsg.setPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, rmsg.getPrimaryStorageUuid());
                                bus.send(rmsg);
                            }

                            trigger.rollback();
                        }
                    });
                }

                flow(new Flow() {
                    String __name__ = "create-data-volume-on-primary-storage";

                    public void run(final FlowTrigger trigger, Map data) {
                        CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg cmsg = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg();
                        cmsg.setNeedDownload(info.needDownload);
                        cmsg.setSnapshots(info.snapshotDownloadInfos);
                        cmsg.setVolumeUuid(msg.getVolume().getUuid());
                        cmsg.setPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, cmsg.getPrimaryStorageUuid());
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply cr = reply.castReply();
                                    info.bitsInstallPath = cr.getInstallPath();
                                    info.bitsSize = cr.getSize();
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        if (info.bitsInstallPath != null) {
                            DeleteBitsOnPrimaryStorageMsg dmsg = new DeleteBitsOnPrimaryStorageMsg();
                            dmsg.setHypervisorType(VolumeFormat.getMasterHypervisorTypeByVolumeFormat(getSelfInventory().getFormat()).toString());
                            dmsg.setPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                            dmsg.setInstallPath(info.bitsInstallPath);
                            bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, dmsg.getPrimaryStorageUuid());
                            bus.send(dmsg);
                        }

                        trigger.next();
                    }
                });

                if (info.needDownload) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "return-workspace-primary-storage-capacity";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            ReturnPrimaryStorageCapacityMsg rmsg = new ReturnPrimaryStorageCapacityMsg();
                            rmsg.setPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                            rmsg.setDiskSize(info.totalSnapshotSize);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, rmsg.getPrimaryStorageUuid());
                            bus.send(rmsg);
                            trigger.next();
                        }
                    });
                }

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        VolumeInventory inv = msg.getVolume();
                        inv.setInstallPath(info.bitsInstallPath);
                        inv.setSize(info.bitsSize);
                        inv.setPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                        inv.setFormat(currentRoot.getFormat());
                        reply.setInventory(inv);
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handle(final CreateTemplateFromVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                createTemplate(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("create-template-from-volume-snapshot-%s", currentRoot.getUuid());
            }
        });
    }

    private void changeStatusOfSnapshots(final StatusEvent evt, final List<VolumeSnapshotInventory> snapshots, final Completion completion) {
        List<ChangeVolumeSnapshotStatusMsg> msgs = CollectionUtils.transformToList(snapshots, new Function<ChangeVolumeSnapshotStatusMsg, VolumeSnapshotInventory>() {
            @Override
            public ChangeVolumeSnapshotStatusMsg call(VolumeSnapshotInventory arg) {
                ChangeVolumeSnapshotStatusMsg msg = new ChangeVolumeSnapshotStatusMsg();
                msg.setEvent(evt.toString());
                msg.setSnapshotUuid(arg.getUuid());
                bus.makeLocalServiceId(msg, VolumeSnapshotConstant.SERVICE_ID);
                return msg;
            }
        });

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                ErrorCode err = null;
                VolumeSnapshotInventory failSnapshot = null;
                for (MessageReply r : replies) {
                    if (!r.isSuccess()) {
                        err = r.getError();
                        failSnapshot = snapshots.get(replies.indexOf(r));
                        break;
                    }
                }

                if (err != null) {
                    completion.fail(errf.instantiateErrorCode(
                            SysErrors.OPERATION_ERROR,
                            String.format("failed to change status of volume snapshot[uuid:%s, name:%s] by status event[%s]",
                                    failSnapshot.getUuid(), failSnapshot.getName(), evt),
                            err
                    ));
                } else {
                    completion.success();
                }
            }
        });
    }

    class CreateBitsFromSnapshotInfo {
        List<SnapshotDownloadInfo> snapshotDownloadInfos;
        PrimaryStorageInventory workspacePrimaryStorage;
        boolean needDownload;
        long neededSizeOnWorkspacePrimaryStorage;
        long totalSnapshotSize;
        long bitsSize;
        List<String> zoneUuidsForFindingWorkspacePrimaryStorage;
    }

    class CreateBitsFromSnapshotInfoForTemplate extends  CreateBitsFromSnapshotInfo {
        CreateBitsFromSnapshotInfoForTemplate(CreateBitsFromSnapshotInfo info) {
            snapshotDownloadInfos = info.snapshotDownloadInfos;
            workspacePrimaryStorage = info.workspacePrimaryStorage;
            needDownload = info.needDownload;
            neededSizeOnWorkspacePrimaryStorage = info.neededSizeOnWorkspacePrimaryStorage;
            totalSnapshotSize = info.totalSnapshotSize;
            bitsSize = info.bitsSize;
            zoneUuidsForFindingWorkspacePrimaryStorage = info.zoneUuidsForFindingWorkspacePrimaryStorage;
        }

        List<BackupStorageInventory> destBackupStorages = new ArrayList<BackupStorageInventory>();
        List<CreateTemplateFromVolumeSnapshotResult> results = new ArrayList<CreateTemplateFromVolumeSnapshotResult>();
    }

    class CreateBitsFromSnapshotInfoForDataVolume extends CreateBitsFromSnapshotInfo {
        String bitsInstallPath;

        CreateBitsFromSnapshotInfoForDataVolume(CreateBitsFromSnapshotInfo info) {
            snapshotDownloadInfos = info.snapshotDownloadInfos;
            workspacePrimaryStorage = info.workspacePrimaryStorage;
            needDownload = info.needDownload;
            neededSizeOnWorkspacePrimaryStorage = info.neededSizeOnWorkspacePrimaryStorage;
            totalSnapshotSize = info.totalSnapshotSize;
            bitsSize = info.bitsSize;
            zoneUuidsForFindingWorkspacePrimaryStorage = info.zoneUuidsForFindingWorkspacePrimaryStorage;
        }
    }

    private CreateBitsFromSnapshotInfo prepareCreateBitsFromSnapshotInfo(final String requiredPrimaryStorage) {
        final CreateBitsFromSnapshotInfo info = new CreateBitsFromSnapshotInfo();
        final List<VolumeSnapshotInventory> ancestors = currentLeaf.getAncestors();
        for (VolumeSnapshotInventory inv : ancestors) {
            if (inv.getPrimaryStorageUuid() == null) {
                info.needDownload = true;
            }

            info.totalSnapshotSize += inv.getSize();
        }

        info.neededSizeOnWorkspacePrimaryStorage = 2 * info.totalSnapshotSize;
        info.snapshotDownloadInfos = new ArrayList<SnapshotDownloadInfo>();
        for (VolumeSnapshotInventory inv : ancestors) {
            SnapshotDownloadInfo downloadInfo = new SnapshotDownloadInfo();
            downloadInfo.setSnapshot(inv);
            info.snapshotDownloadInfos.add(downloadInfo);
        }

        if (info.needDownload) {
            //1. find zones that has all backup storage attached; these backup storage must contain snapshot and its ancestors
            Set<String> allBsUuids = new HashSet<String>();
            for (SnapshotDownloadInfo dinfo : info.snapshotDownloadInfos) {
                allBsUuids.addAll(CollectionUtils.transformToList(dinfo.getSnapshot().getBackupStorageRefs(), new Function<String, VolumeSnapshotBackupStorageRefInventory>() {
                    @Override
                    public String call(VolumeSnapshotBackupStorageRefInventory arg) {
                        return arg.getBackupStorageUuid();
                    }
                }));
            }

            SimpleQuery<BackupStorageZoneRefVO> q = dbf.createQuery(BackupStorageZoneRefVO.class);
            q.select(BackupStorageZoneRefVO_.zoneUuid, BackupStorageZoneRefVO_.backupStorageUuid);
            q.add(BackupStorageZoneRefVO_.backupStorageUuid, Op.IN, allBsUuids);
            List<Tuple> ts = q.listTuple();

            Map<String, Set<String>> zoneBsMapping = new HashMap<String, Set<String>>();
            for (Tuple t : ts) {
                String zoneUuid = t.get(0, String.class);
                String bsUuid = t.get(1, String.class);
                Set<String> bs = zoneBsMapping.get(zoneUuid);
                if (bs == null) {
                    bs = new HashSet<String>();
                    zoneBsMapping.put(zoneUuid, bs);
                }
                bs.add(bsUuid);
            }

            info.zoneUuidsForFindingWorkspacePrimaryStorage = new ArrayList<String>();
            for (Map.Entry<String, Set<String>> e : zoneBsMapping.entrySet()) {
                Set<String> bs = e.getValue();
                if (bs.size() == allBsUuids.size()) {
                    info.zoneUuidsForFindingWorkspacePrimaryStorage.add(e.getKey());
                }
            }

            if (info.zoneUuidsForFindingWorkspacePrimaryStorage.isEmpty()) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("to create template from snapshot[uuid:%s], we need to download its ancestors from backup storage; however, we can not find a zone that has all ancestors' backup storage attached", currentRoot.getUuid())
                ));
            }

            //2. find backup storage for downloading each volume snapshot
            for (final SnapshotDownloadInfo dinfo : info.snapshotDownloadInfos) {
                final List<String> bsOfSnapshot = CollectionUtils.transformToList(dinfo.getSnapshot().getBackupStorageRefs(), new Function<String, VolumeSnapshotBackupStorageRefInventory>() {
                    @Override
                    public String call(VolumeSnapshotBackupStorageRefInventory arg) {
                        return arg.getBackupStorageUuid();
                    }
                });

                VolumeSnapshotBackupStorageRefInventory targetBs = new FunctionNoArg<VolumeSnapshotBackupStorageRefInventory>() {
                    @Override
                    @Transactional
                    public VolumeSnapshotBackupStorageRefInventory call() {
                        String sql = "select sref from BackupStorageVO bs, VolumeSnapshotBackupStorageRefVO sref, BackupStorageZoneRefVO zref where sref.volumeSnapshotUuid = :snapshotUuid and sref.backupStorageUuid in (:sbsUuids) and bs.status = :bsStatus and bs.state = :bsState and bs.uuid = sref.backupStorageUuid and zref.backupStorageUuid = sref.backupStorageUuid and zref.zoneUuid in (:zoneUuids)";
                        TypedQuery<VolumeSnapshotBackupStorageRefVO> q = dbf.getEntityManager().createQuery(sql, VolumeSnapshotBackupStorageRefVO.class);
                        q.setParameter("bsState", BackupStorageState.Enabled);
                        q.setParameter("bsStatus", BackupStorageStatus.Connected);
                        q.setParameter("sbsUuids", bsOfSnapshot);
                        q.setParameter("snapshotUuid", dinfo.getSnapshot().getUuid());
                        q.setParameter("zoneUuids", info.zoneUuidsForFindingWorkspacePrimaryStorage);
                        List<VolumeSnapshotBackupStorageRefVO> targetBsUuids = q.getResultList();
                        if (targetBsUuids.isEmpty()) {
                            return null;
                        }

                        return VolumeSnapshotBackupStorageRefInventory.valueOf(targetBsUuids.get(0));
                    }
                }.call();

                if (targetBs == null) {
                    throw new OperationFailureException(errf.stringToOperationError(
                            String.format("all backup storage that have volume snapshot[uuid:%s] are not in state[%s] or status[%s]",
                                    dinfo.getSnapshot().getUuid(), BackupStorageState.Enabled, BackupStorageStatus.Connected)
                    ));
                }

                dinfo.setBackupStorageUuid(targetBs.getBackupStorageUuid());
                dinfo.setBackupStorageInstallPath(targetBs.getInstallPath());
            }

            // 3. find workspace primary storage for downloading snapshots and merging final bits
            if (currentRoot.getType().equals(VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString())) {
                new Runnable() {
                    @Override
                    @Transactional(readOnly = true)
                    public void run() {
                        String sql;
                        if (requiredPrimaryStorage != null) {
                            sql = "select pr from PrimaryStorageVO pr, PrimaryStorageClusterRefVO ref, PrimaryStorageCapacityVO cap, ClusterVO cluster where pr.uuid = cap.uuid and pr.zoneUuid in (:zoneUuids) and pr.uuid = ref.primaryStorageUuid and ref.clusterUuid = cluster.uuid and cluster.hypervisorType = :hvType and pr.status = :prStatus and pr.state = :prState and cap.availableCapacity > :size and pr.uuid = :prUuid";
                        } else {
                            sql = "select pr from PrimaryStorageVO pr, PrimaryStorageCapacityVO cap, PrimaryStorageClusterRefVO ref, ClusterVO cluster where pr.uuid = cap.uuid and pr.zoneUuid in (:zoneUuids) and pr.uuid = ref.primaryStorageUuid and ref.clusterUuid = cluster.uuid and cluster.hypervisorType = :hvType and pr.status = :prStatus and pr.state = :prState and cap.availableCapacity > :size";
                        }

                        TypedQuery<PrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
                        if (requiredPrimaryStorage != null) {
                            q.setParameter("prUuid", requiredPrimaryStorage);
                        }

                        q.setParameter("zoneUuids", info.zoneUuidsForFindingWorkspacePrimaryStorage);
                        String hvType = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(currentRoot.getFormat()).toString();
                        q.setParameter("hvType", hvType);
                        q.setParameter("prState", PrimaryStorageState.Enabled);
                        q.setParameter("prStatus", PrimaryStorageStatus.Connected);
                        q.setParameter("size", info.neededSizeOnWorkspacePrimaryStorage);
                        List<PrimaryStorageVO> prs = q.getResultList();

                        logger.debug(ln(
                                "searching primary storage for:",
                                "in zones: {0}",
                                "needed size: {1}",
                                "attached to cluster having hypervisor type: {2}"
                        ).format(info.zoneUuidsForFindingWorkspacePrimaryStorage, info.neededSizeOnWorkspacePrimaryStorage, hvType));

                        if (prs.isEmpty()) {
                            throw new OperationFailureException(errf.stringToOperationError(
                                    String.format("can not find a primary storage in zone[uuids:%s] that satisfies conditions[state:%s, status:%s, available size > %s bytes, attached to cluster having hypervisorType:%s] to download ancestors for volume snapshot[uuid:%s] ",
                                            info.zoneUuidsForFindingWorkspacePrimaryStorage, PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, info.neededSizeOnWorkspacePrimaryStorage, hvType, currentRoot.getUuid())
                            ));
                        }

                        List<String> prUuids = CollectionUtils.transformToList(prs, new Function<String, PrimaryStorageVO>() {
                            @Override
                            public String call(PrimaryStorageVO arg) {
                                return arg.getUuid();
                            }
                        });

                        // filter primary storage that doesn't have ability to handle snapshot of specific hypervisor type
                        final String tag = VolumeSnapshotTag.CAPABILITY_HYPERVISOR_SNAPSHOT.completeTag(hvType);
                        SimpleQuery<SystemTagVO> tagq = dbf.createQuery(SystemTagVO.class);
                        tagq.select(SystemTagVO_.resourceUuid);
                        tagq.add(SystemTagVO_.tag, Op.EQ, tag);
                        tagq.add(SystemTagVO_.resourceUuid, Op.IN, prUuids);
                        final List<String> prHasTag = tagq.listValue();
                        if (prHasTag.isEmpty()) {
                            throw new OperationFailureException(errf.stringToOperationError(
                                    String.format("all candidate primary storage can not handle hypervisor volume snapshot[hypervisorType:%s, uuid:%s]",
                                            hvType, currentRoot.getUuid())
                            ));
                        }

                        PrimaryStorageVO targetPr = CollectionUtils.find(prs, new Function<PrimaryStorageVO, PrimaryStorageVO>() {
                            @Override
                            public PrimaryStorageVO call(PrimaryStorageVO arg) {
                                if (prHasTag.contains(arg.getUuid())) {
                                    return arg;
                                }
                                return null;
                            }
                        });

                        info.workspacePrimaryStorage = PrimaryStorageInventory.valueOf(targetPr);
                    }
                }.run();
            } else {
                if (requiredPrimaryStorage == null) {
                    throw new OperationFailureException(errf.stringToOperationError(
                            String.format("Please tell me which primary storage you want to create this data volume by providing 'primaryStorageUuid' in API. This snapshot is of type of storage snapshot, ZStack cannot figure out which primary storage to use by itself")
                    ));
                } else {
                    PrimaryStorageVO privo = dbf.findByUuid(requiredPrimaryStorage, PrimaryStorageVO.class);
                    info.workspacePrimaryStorage  = PrimaryStorageInventory.valueOf(privo);
                }
            }
        } else {
            PrimaryStorageVO privo = dbf.findByUuid(currentRoot.getPrimaryStorageUuid(), PrimaryStorageVO.class);
            info.workspacePrimaryStorage  = PrimaryStorageInventory.valueOf(privo);
        }

        return info;
    }

    private void createTemplate(final CreateTemplateFromVolumeSnapshotMsg msg, final NoErrorCompletion completion) {
        final CreateTemplateFromVolumeSnapshotReply reply = new CreateTemplateFromVolumeSnapshotReply();

        refreshVO();
        ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            reply.setError(err);
            bus.reply(msg, reply);
            completion.done();
            return;
        }


        final CreateBitsFromSnapshotInfoForTemplate info = new CreateBitsFromSnapshotInfoForTemplate(prepareCreateBitsFromSnapshotInfo(null));

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-template-from-snapshot-%s", currentRoot.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (info.needDownload) {
                    flow(new Flow() {
                        String __name__ = "allocateHost-workspace-primary-storage";
                        boolean success;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                            amsg.setPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                            amsg.setSize(info.neededSizeOnWorkspacePrimaryStorage);
                            bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        success = true;
                                        trigger.next();
                                    } else {
                                        trigger.fail(reply.getError());
                                    }
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowTrigger trigger, Map data) {
                            if (success) {
                                ReturnPrimaryStorageCapacityMsg rmsg = new ReturnPrimaryStorageCapacityMsg();
                                rmsg.setDiskSize(info.neededSizeOnWorkspacePrimaryStorage);
                                rmsg.setPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, rmsg.getPrimaryStorageUuid());
                                bus.send(rmsg);
                            }

                            trigger.rollback();
                        }
                    });
                }

                flow(new Flow() {
                    String __name__ = "allocateHost-destination-backup-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (msg.getBackupStorageUuids() == null) {
                            AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                            amsg.setSize(info.totalSnapshotSize);
                            amsg.setRequiredZoneUuid(info.workspacePrimaryStorage.getZoneUuid());
                            bus.makeLocalServiceId(amsg, BackupStorageConstant.SERVICE_ID);
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        AllocateBackupStorageReply ar = reply.castReply();
                                        info.destBackupStorages.add(ar.getInventory());
                                        trigger.next();
                                    } else {
                                        trigger.fail(reply.getError());
                                    }
                                }
                            });
                        } else {
                            List<AllocateBackupStorageMsg> amsgs = CollectionUtils.transformToList(msg.getBackupStorageUuids(), new Function<AllocateBackupStorageMsg, String>() {
                                @Override
                                public AllocateBackupStorageMsg call(String arg) {
                                    AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                                    amsg.setBackupStorageUuid(arg);
                                    amsg.setSize(info.totalSnapshotSize);
                                    amsg.setRequiredZoneUuid(info.workspacePrimaryStorage.getZoneUuid());
                                    bus.makeLocalServiceId(amsg, BackupStorageConstant.SERVICE_ID);
                                    return amsg;
                                }
                            });

                            bus.send(amsgs, new CloudBusListCallBack(trigger) {
                                @Override
                                public void run(List<MessageReply> replies) {
                                    List<ErrorCode> errs = new ArrayList<ErrorCode>();
                                    for (MessageReply r : replies) {
                                        String bsUuid = msg.getBackupStorageUuids().get(replies.indexOf(r));
                                        if (!r.isSuccess()) {
                                            errs.add(r.getError());
                                            logger.warn(String.format("failed to allocate backup storage[uuid:%s] because %s",
                                                    bsUuid, r.getError()));
                                        } else {
                                            info.destBackupStorages.add(((AllocateBackupStorageReply)r).getInventory());
                                        }
                                    }

                                    if (info.destBackupStorages.isEmpty()) {
                                        trigger.fail(errf.stringToOperationError(String.format("failed to allocate all backup storage[uuids:%s], a list of error: %s",
                                                msg.getBackupStorageUuids(), JSONObjectUtil.toJsonString(errs))));
                                    } else {
                                        trigger.next();
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        if (!info.destBackupStorages.isEmpty()) {
                            List<ReturnBackupStorageMsg> rmsgs = CollectionUtils.transformToList(info.destBackupStorages, new Function<ReturnBackupStorageMsg, BackupStorageInventory>() {
                                @Override
                                public ReturnBackupStorageMsg call(BackupStorageInventory arg) {
                                    ReturnBackupStorageMsg rmsg = new ReturnBackupStorageMsg();
                                    rmsg.setBackupStorageUuid(arg.getUuid());
                                    rmsg.setSize(info.totalSnapshotSize);
                                    bus.makeTargetServiceIdByResourceUuid(rmsg, BackupStorageConstant.SERVICE_ID, arg.getUuid());
                                    return rmsg;
                                }
                            });

                            bus.send(rmsgs, new CloudBusListCallBack() {
                                @Override
                                public void run(List<MessageReply> replies) {
                                    for (MessageReply r : replies) {
                                        if (!r.isSuccess()) {
                                            BackupStorageInventory bs = info.destBackupStorages.get(replies.indexOf(r));
                                            logger.warn(String.format("failed to return capacity[%s bytes] to backup storage[uuid:%s], because %s",
                                                    info.totalSnapshotSize, bs.getUuid(), r.getError()));
                                        }
                                    }
                                }
                            });
                        }

                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-template-from-volume-snapshot-on-primary-storage";

                    private void returnBackupStorageForFailure(CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply reply) {


                        // failed to upload to some backup storage, return capacity to them
                        List<String> successBsUuids = CollectionUtils.transformToList(reply.getResults(), new Function<String, CreateTemplateFromVolumeSnapshotResult>() {
                            @Override
                            public String call(CreateTemplateFromVolumeSnapshotResult arg) {
                                return arg.getBackupStorageUuid();
                            }
                        });

                        List<String> allBsUuids = CollectionUtils.transformToList(info.destBackupStorages, new Function<String, BackupStorageInventory>() {
                            @Override
                            public String call(BackupStorageInventory arg) {
                                return arg.getUuid();
                            }
                        });

                        allBsUuids.removeAll(successBsUuids);

                        logger.debug(String.format("return capacity to backup storage%s, because they fails to upload image", allBsUuids));
                        List<ReturnBackupStorageMsg> rmsgs = CollectionUtils.transformToList(allBsUuids, new Function<ReturnBackupStorageMsg, String>() {
                            @Override
                            public ReturnBackupStorageMsg call(String arg) {
                                ReturnBackupStorageMsg rmsg = new ReturnBackupStorageMsg();
                                rmsg.setBackupStorageUuid(arg);
                                rmsg.setSize(info.totalSnapshotSize);
                                bus.makeTargetServiceIdByResourceUuid(rmsg, BackupStorageConstant.SERVICE_ID, arg);
                                return rmsg;
                            }
                        });

                        bus.send(rmsgs);
                    }

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg cmsg = new CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg();
                        cmsg.setImageUuid(msg.getImageUuid());
                        cmsg.setBackupStorage(info.destBackupStorages);
                        cmsg.setSnapshotsDownloadInfo(info.snapshotDownloadInfos);
                        cmsg.setNeedDownload(info.needDownload);
                        if (info.needDownload) {
                            cmsg.setPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                        } else {
                            cmsg.setPrimaryStorageUuid(currentRoot.getPrimaryStorageUuid());
                        }
                        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, cmsg.getPrimaryStorageUuid());

                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply cr = reply.castReply();
                                    if (cr.getResults().size() != info.destBackupStorages.size()) {
                                        returnBackupStorageForFailure(cr);
                                    }

                                    info.results = cr.getResults();
                                    info.bitsSize = cr.getSize();
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        if (!info.results.isEmpty()) {
                            List<DeleteBitsOnBackupStorageMsg> dmsgs = CollectionUtils.transformToList(info.results, new Function<DeleteBitsOnBackupStorageMsg, CreateTemplateFromVolumeSnapshotResult>() {
                                @Override
                                public DeleteBitsOnBackupStorageMsg call(CreateTemplateFromVolumeSnapshotResult arg) {
                                    DeleteBitsOnBackupStorageMsg dmsg = new DeleteBitsOnBackupStorageMsg();
                                    dmsg.setBackupStorageUuid(arg.getBackupStorageUuid());
                                    dmsg.setInstallPath(arg.getInstallPath());
                                    bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, arg.getBackupStorageUuid());
                                    return dmsg;
                                }
                            });

                            bus.send(dmsgs, new CloudBusListCallBack() {
                                @Override
                                public void run(List<MessageReply> replies) {
                                    for (MessageReply r : replies) {
                                        if (!r.isSuccess()) {
                                            CreateTemplateFromVolumeSnapshotResult ret = info.results.get(replies.indexOf(r));
                                            logger.warn(String.format("failed to delete bit[%s] from backup storage[uuid:%s]",
                                                    ret.getInstallPath(), ret.getBackupStorageUuid()));
                                        }
                                    }
                                }
                            });
                        }

                        trigger.rollback();
                    }
                });

                if (info.needDownload) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "return-workspace-primary-storage";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            ReturnPrimaryStorageCapacityMsg rmsg = new ReturnPrimaryStorageCapacityMsg();
                            rmsg.setDiskSize(info.totalSnapshotSize);
                            rmsg.setPrimaryStorageUuid(info.workspacePrimaryStorage.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, rmsg.getPrimaryStorageUuid());
                            bus.send(rmsg);
                            trigger.next();
                        }
                    });
                }

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        reply.setResults(info.results);
                        reply.setSize(info.bitsSize);
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteVolumeSnapshotMsg) {
            handle((APIDeleteVolumeSnapshotMsg) msg);
        } else if (msg instanceof APIRevertVolumeFromSnapshotMsg) {
            handle((APIRevertVolumeFromSnapshotMsg) msg);
        } else if (msg instanceof APIBackupVolumeSnapshotMsg) {
            handle((APIBackupVolumeSnapshotMsg) msg);
        } else if (msg instanceof APIDeleteVolumeSnapshotFromBackupStorageMsg) {
            handle((APIDeleteVolumeSnapshotFromBackupStorageMsg) msg);
        } else if (msg instanceof APIUpdateVolumeSnapshotMsg) {
            handle((APIUpdateVolumeSnapshotMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateVolumeSnapshotMsg msg) {
        VolumeSnapshotVO self = dbf.findByUuid(msg.getUuid(), VolumeSnapshotVO.class);

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

        APIUpdateVolumeSnapshotEvent evt = new APIUpdateVolumeSnapshotEvent(msg.getId());
        evt.setInventory(VolumeSnapshotInventory.valueOf(self));
        bus.publish(evt);
    }

    private boolean cleanup() {
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.select(VolumeSnapshotVO_.primaryStorageUuid);
        q.add(VolumeSnapshotVO_.uuid, Op.EQ, currentRoot.getUuid());
        String uuid = q.findValue();

        if (uuid != null) {
            return false;
        }

        SimpleQuery<VolumeSnapshotBackupStorageRefVO> bq = dbf.createQuery(VolumeSnapshotBackupStorageRefVO.class);
        bq.add(VolumeSnapshotBackupStorageRefVO_.volumeSnapshotUuid, Op.EQ, currentRoot.getUuid());
        boolean onBs = bq.isExists();

        if (onBs) {
            return false;
        }

        // the snapshot is on neither primary storage nor backup storage. delete it and descendants
        List<String> uuids = CollectionUtils.transformToList(currentLeaf.getDescendants(), new Function<String, VolumeSnapshotInventory>() {
            @Override
            public String call(VolumeSnapshotInventory arg) {
                return arg.getUuid();
            }
        });

        dbf.removeByPrimaryKeys(uuids, VolumeSnapshotVO.class);

        SimpleQuery<VolumeSnapshotVO> tq = dbf.createQuery(VolumeSnapshotVO.class);
        tq.add(VolumeSnapshotVO_.treeUuid, Op.EQ, currentRoot.getTreeUuid());
        if (!tq.isExists()) {
            logger.debug(String.format("volume snapshot tree[uuid:%s] has no leaf, delete it", currentRoot.getTreeUuid()));
            dbf.removeByPrimaryKey(currentRoot.getTreeUuid(), VolumeSnapshotTreeVO.class);
        }

        return true;
    }

    private List<VolumeSnapshotBackupStorageDeletionMsg> makeVolumeSnapshotBackupStorageDeletionMsg(List<String> bsUuids) {
        List<VolumeSnapshotBackupStorageDeletionMsg> msgs = new ArrayList<VolumeSnapshotBackupStorageDeletionMsg>();

        List<String> allMyBsUuids = CollectionUtils.transformToList(currentRoot.getBackupStorageRefs(), new Function<String, VolumeSnapshotBackupStorageRefVO>() {
            @Override
            public String call(VolumeSnapshotBackupStorageRefVO arg) {
                return arg.getBackupStorageUuid();
            }
        });

        if (allMyBsUuids.isEmpty()) {
            return msgs;
        }

        if (bsUuids == null || bsUuids.containsAll(allMyBsUuids)) {
            // delete me and all my descendants
            for (VolumeSnapshotInventory inv : currentLeaf.getDescendants()) {
                List<String> buuids = CollectionUtils.transformToList(inv.getBackupStorageRefs(), new Function<String, VolumeSnapshotBackupStorageRefInventory>() {
                    @Override
                    public String call(VolumeSnapshotBackupStorageRefInventory arg) {
                        return arg.getBackupStorageUuid();
                    }
                });
                VolumeSnapshotBackupStorageDeletionMsg msg = new VolumeSnapshotBackupStorageDeletionMsg();
                msg.setBackupStorageUuids(buuids);
                msg.setSnapshotUuid(inv.getUuid());
                bus.makeLocalServiceId(msg, VolumeSnapshotConstant.SERVICE_ID);
                msgs.add(msg);
            }
        } else {
            // delete me only
            VolumeSnapshotBackupStorageDeletionMsg msg = new VolumeSnapshotBackupStorageDeletionMsg();
            msg.setSnapshotUuid(currentRoot.getUuid());
            msg.setBackupStorageUuids(bsUuids);
            bus.makeLocalServiceId(msg, VolumeSnapshotConstant.SERVICE_ID);
            msgs.add(msg);
        }

        return msgs;
    }

    private void handle(final APIDeleteVolumeSnapshotFromBackupStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                deleteOnBackupStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-volume-snapshot-%s-on-backup-storage", currentRoot.getUuid());
            }
        });
    }

    private void deleteOnBackupStorage(final APIDeleteVolumeSnapshotFromBackupStorageMsg msg, final NoErrorCompletion completion) {
        final APIDeleteVolumeSnapshotFromBackupStorageEvent evt = new APIDeleteVolumeSnapshotFromBackupStorageEvent(msg.getId());

        refreshVO();
        final ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            evt.setErrorCode(err);
            bus.publish(evt);
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-volume-snapshot-%s-from-backup-stroage", currentRoot.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-volume-snapshot-from-backup-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<VolumeSnapshotBackupStorageDeletionMsg> msgs = makeVolumeSnapshotBackupStorageDeletionMsg(msg.getBackupStorageUuids());
                        bus.send(msgs, 1, new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                //delete would always succeed
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        if (!cleanup()) {
                            currentRoot = dbf.reload(currentRoot);
                            evt.setInventory(getSelfInventory());
                        }

                        bus.publish(evt);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setErrorCode(errCode);
                        bus.publish(evt);
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handle(final APIBackupVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask() {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                backup(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("backup-volume-snapshot-%s", msg.getUuid());
            }
        });
    }

    private void backup(final APIBackupVolumeSnapshotMsg msg, final NoErrorCompletion completion) {
        final APIBackupVolumeSnapshotEvent evt = new APIBackupVolumeSnapshotEvent(msg.getId());

        refreshVO();
        final ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            evt.setErrorCode(err);
            bus.publish(evt);
            completion.done();
            return;
        }

        class Info {
            VolumeSnapshotInventory snapshot;
            String backupStorageUuid;
            BackupStorageInventory destBackupStorage;
            boolean backupSuccess;
        }

        final String requiredBsUuid = msg.getBackupStorageUuid();
        final List<Info> needBackup = new ArrayList<Info>();
        currentLeaf.walkUp(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                Info info = new Info();
                info.snapshot = arg;

                if (arg.getUuid().equals(currentRoot.getUuid()) && requiredBsUuid != null) {
                    info.backupStorageUuid = requiredBsUuid;
                    needBackup.add(info);
                    return false;
                }

                if (arg.getBackupStorageRefs().isEmpty()) {
                    needBackup.add(info);
                }

                return false;
            }
        });

        if (needBackup.isEmpty()) {
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("backup-volume-snapshot-%s", currentRoot.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "allocate-backup-storage";

                    private void allocateBackupStorage(final Iterator<Info> it, final FlowTrigger trigger) {
                        if (!it.hasNext()) {
                            trigger.next();
                            return;
                        }

                        final Info info = it.next();
                        AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                        amsg.setBackupStorageUuid(info.backupStorageUuid);
                        amsg.setSize(info.snapshot.getSize());
                        bus.makeLocalServiceId(amsg, BackupStorageConstant.SERVICE_ID);
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    AllocateBackupStorageReply re = reply.castReply();
                                    info.destBackupStorage = re.getInventory();
                                    allocateBackupStorage(it, trigger);
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        allocateBackupStorage(needBackup.iterator(), trigger);
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        for (Info info : needBackup) {
                            if (info.destBackupStorage != null) {
                                ReturnBackupStorageMsg rmsg = new ReturnBackupStorageMsg();
                                rmsg.setBackupStorageUuid(info.destBackupStorage.getUuid());
                                rmsg.setSize(info.snapshot.getSize());
                                bus.makeTargetServiceIdByResourceUuid(rmsg, BackupStorageConstant.SERVICE_ID, info.destBackupStorage.getUuid());
                                bus.send(rmsg);
                            }
                        }

                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "backing-up";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<BackupVolumeSnapshotMsg> bmsgs = CollectionUtils.transformToList(needBackup, new Function<BackupVolumeSnapshotMsg, Info>() {
                            @Override
                            public BackupVolumeSnapshotMsg call(Info arg) {
                                BackupVolumeSnapshotMsg bmsg = new BackupVolumeSnapshotMsg();
                                bmsg.setBackupStorage(arg.destBackupStorage);
                                bmsg.setSnapshotUuid(arg.snapshot.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(bmsg, VolumeSnapshotConstant.SERVICE_ID, arg.snapshot.getUuid());
                                return bmsg;
                            }
                        });

                        bus.send(bmsgs, VolumeSnapshotGlobalConfig.SNAPSHOT_BACKUP_PARALLELISM_DEGREE.value(Integer.class), new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                ErrorCode err = null;
                                for (MessageReply r : replies) {
                                    if (r.isSuccess()) {
                                        Info info = needBackup.get(replies.indexOf(r));
                                        info.backupSuccess = true;
                                        logger.debug(String.format("successfully backed up volume snapshot[uuid:%s] on backup storage[uuid:%s]",
                                                info.snapshot.getUuid(), info.destBackupStorage.getUuid()));
                                    } else {
                                        err = r.getError();
                                    }
                                }

                                if (err != null) {
                                    trigger.fail(err);
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(final FlowTrigger trigger, Map data) {
                        List<VolumeSnapshotBackupStorageDeletionMsg> dmsgs = CollectionUtils.transformToList(needBackup, new Function<VolumeSnapshotBackupStorageDeletionMsg, Info>() {
                            @Override
                            public VolumeSnapshotBackupStorageDeletionMsg call(Info arg) {
                                if (arg.backupSuccess) {
                                    VolumeSnapshotBackupStorageDeletionMsg dmsg = new VolumeSnapshotBackupStorageDeletionMsg();
                                    dmsg.setSnapshotUuid(arg.snapshot.getUuid());
                                    dmsg.setBackupStorageUuids(Arrays.asList(arg.destBackupStorage.getUuid()));
                                    bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeSnapshotConstant.SERVICE_ID, arg.snapshot.getUuid());
                                    return dmsg;
                                }
                                return null;
                            }
                        });

                        if (dmsgs.isEmpty()) {
                            trigger.rollback();
                            return;
                        }

                        bus.send(dmsgs, VolumeSnapshotGlobalConfig.SNAPSHOT_BACKUP_PARALLELISM_DEGREE.value(Integer.class), new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                trigger.rollback();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        refreshVO();
                        evt.setInventory(getSelfInventory());
                        bus.publish(evt);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setErrorCode(errCode);
                        bus.publish(evt);
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handle(final APIRevertVolumeFromSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                revert(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("revert-volume-%s-from-snapshot-%s", currentRoot.getVolumeUuid(), currentRoot.getUuid());
            }
        });
    }

    private void revert(final APIRevertVolumeFromSnapshotMsg msg, final NoErrorCompletion completion) {
        final APIRevertVolumeFromSnapshotEvent evt = new APIRevertVolumeFromSnapshotEvent(msg.getId());

        refreshVO();
        final ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            evt.setErrorCode(err);
            bus.publish(evt);
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("revert-volume-%s-from-snapshot-%s", currentRoot.getVolumeUuid(), currentRoot.getUuid()));
        chain.then(new ShareFlow() {
            String newVolumeInstallPath;
            VolumeVO volume = dbf.findByUuid(currentRoot.getVolumeUuid(), VolumeVO.class);
            VolumeInventory volumeInventory = VolumeInventory.valueOf(volume);

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "revert-volume-from-volume-snapshot-on-primary-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        RevertVolumeFromSnapshotOnPrimaryStorageMsg rmsg = new RevertVolumeFromSnapshotOnPrimaryStorageMsg();
                        rmsg.setSnapshot(getSelfInventory());
                        rmsg.setVolume(volumeInventory);
                        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, volumeInventory.getPrimaryStorageUuid());
                        bus.send(rmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    RevertVolumeFromSnapshotOnPrimaryStorageReply re = (RevertVolumeFromSnapshotOnPrimaryStorageReply) reply;
                                    newVolumeInstallPath = re.getNewVolumeInstallPath();
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg, completion) {
                    @Transactional
                    private void updateLatest() {
                        String sql = "update VolumeSnapshotVO s set s.latest = false where s.latest = true and s.treeUuid = :treeUuid";
                        Query q = dbf.getEntityManager().createQuery(sql);
                        q.setParameter("treeUuid", currentRoot.getTreeUuid());
                        q.executeUpdate();

                        currentRoot.setLatest(true);
                        dbf.getEntityManager().merge(currentRoot);

                        sql = "update VolumeSnapshotTreeVO tree set tree.current = false where tree.current = true and tree.volumeUuid = :volUuid";
                        q = dbf.getEntityManager().createQuery(sql);
                        q.setParameter("volUuid", currentRoot.getVolumeUuid());
                        q.executeUpdate();

                        sql = "update VolumeSnapshotTreeVO tree set tree.current = true where tree.uuid = :treeUuid";
                        q = dbf.getEntityManager().createQuery(sql);
                        q.setParameter("treeUuid", currentRoot.getTreeUuid());
                        q.executeUpdate();
                    }

                    @Override
                    public void handle(Map data) {
                        volume.setInstallPath(newVolumeInstallPath);
                        dbf.update(volume);
                        updateLatest();
                        bus.publish(evt);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        logger.warn(String.format("failed to restore volume[uuid:%s] to snapshot[uuid:%s, name:%s], %s",
                                volumeInventory.getUuid(), currentRoot.getUuid(), currentRoot.getName(), errCode));
                        evt.setErrorCode(errCode);
                        bus.publish(evt);
                        completion.done();
                    }
                });
            }
        }).start();

    }

    private void handle(final APIDeleteVolumeSnapshotMsg msg) {
        final APIDeleteVolumeSnapshotEvent evt = new APIDeleteVolumeSnapshotEvent(msg.getId());
        final String issuer = VolumeSnapshotVO.class.getSimpleName();
        final List<VolumeSnapshotInventory> ctx = Arrays.asList(getSelfInventory());
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-snapshot-%s", msg.getUuid()));
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
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setErrorCode(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errCode));
                bus.publish(evt);
            }
        }).start();
    }
}
