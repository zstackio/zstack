package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.notification.N;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.progress.TaskProgressRange;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMConstant;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.primary.PrimaryStoragePathMaker;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;
import static org.zstack.core.progress.ProgressReportService.*;

public class NfsPrimaryStorage extends PrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(NfsPrimaryStorage.class);

    @Autowired
    private NfsPrimaryStorageFactory factory;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private NfsPrimaryStorageManager nfsMgr;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private NfsPrimaryStorageImageCacheCleaner imageCacheCleaner;
    @Autowired
    private PluginRegistry pluginRgty;

    public NfsPrimaryStorage() {
    }

    public NfsPrimaryStorage(PrimaryStorageVO vo) {
        super(vo);
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof PrimaryStorageRemoveCachedImageMsg) {
            handle((PrimaryStorageRemoveCachedImageMsg) msg);
        } else if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) {
            handle((BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateTemporaryVolumeFromSnapshotMsg) {
            handle((CreateTemporaryVolumeFromSnapshotMsg) msg);
        } else if (msg instanceof UploadBitsToBackupStorageMsg) {
            handle((UploadBitsToBackupStorageMsg) msg);
        } else if (msg instanceof GetVolumeRootImageUuidFromPrimaryStorageMsg) {
            handle((GetVolumeRootImageUuidFromPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteImageCacheOnPrimaryStorageMsg) {
            handle((DeleteImageCacheOnPrimaryStorageMsg) msg);
        } else if (msg instanceof NfsRecalculatePrimaryStorageCapacityMsg) {
            handle((NfsRecalculatePrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof NfsToNfsMigrateVolumeMsg) {
            handle((NfsToNfsMigrateVolumeMsg) msg);
        } else if (msg instanceof NfsRebaseVolumeBackingFileMsg) {
            handle((NfsRebaseVolumeBackingFileMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    protected void updateMountPoint(String newUrl, Completion completion) {
        String oldUrl = self.getUrl();

        SimpleQuery<PrimaryStorageClusterRefVO> q = dbf.createQuery(PrimaryStorageClusterRefVO.class);
        q.select(PrimaryStorageClusterRefVO_.clusterUuid);
        q.add(PrimaryStorageClusterRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
        List<String> cuuids = q.listValue();
        if (cuuids.isEmpty()) {
            completion.success();
        }

        for (UpdatePrimaryStorageExtensionPoint ext : pluginRgty.getExtensionList(UpdatePrimaryStorageExtensionPoint.class)){
            ext.beforeUpdatePrimaryStorage(PrimaryStorageInventory.valueOf(self));
        }

        PrimaryStorageInventory psinv = getSelfInventory();
        new LoopAsyncBatch<String>(completion) {
            @Override
            protected Collection<String> collect() {
                return cuuids;
            }

           @Override
           protected AsyncBatchRunner forEach(String item) {
               return new AsyncBatchRunner() {
                   @Override
                   public void run(NoErrorCompletion completion) {
                       NfsPrimaryStorageBackend bkd = getBackendByClusterUuid(item);
                       bkd.updateMountPoint(psinv, item, oldUrl, newUrl, new Completion(completion) {
                           @Override
                           public void success() {
                               completion.done();
                           }

                           @Override
                           public void fail(ErrorCode errorCode) {
                               logger.warn(String.format("failed to update the nfs[uuid:%s, name:%s] mount point" +
                                               " from %s to %s in the cluster[uuid:%s], %s", self.getUuid(), self.getName(),
                                       oldUrl, newUrl, item, errorCode));
                               errors.add(errorCode);
                               completion.done();
                           }
                       });
                   }
               };
           }

            @Override
            protected void done() {
                if (errors.size() == cuuids.size()){
                    completion.fail(errors.get(0));
                }else {
                    completion.success();
                }
            }
        }.start();
    }

    @Override
    protected void updatePrimaryStorage(APIUpdatePrimaryStorageMsg msg, ReturnValueCompletion<PrimaryStorageVO> completion) {
        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }

        if (msg.getUrl() != null && !self.getUrl().equals(msg.getUrl())){
            updateMountPoint(msg.getUrl(), new Completion(completion) {
                @Override
                public void success() {
                    self.setUrl(msg.getUrl());
                    self = dbf.updateAndRefresh(self);
                    for (UpdatePrimaryStorageExtensionPoint ext : pluginRgty.getExtensionList(UpdatePrimaryStorageExtensionPoint.class)){
                        ext.afterUpdatePrimaryStorage(PrimaryStorageInventory.valueOf(self));
                    }
                    completion.success(self);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
            return;
        }
        completion.success(update? self: null);
    }

    @Override
    protected void handle(APICleanUpImageCacheOnPrimaryStorageMsg msg) {
        APICleanUpImageCacheOnPrimaryStorageEvent evt = new APICleanUpImageCacheOnPrimaryStorageEvent(msg.getId());
        imageCacheCleaner.cleanup(msg.getUuid());
        bus.publish(evt);
    }

    private void handle(final DeleteImageCacheOnPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend bkd = getUsableBackend();
        if (bkd == null) {
            throw new OperationFailureException(operr("cannot find usable backend"));
        }
        DeleteImageCacheOnPrimaryStorageReply sreply = new DeleteImageCacheOnPrimaryStorageReply();
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-image-cache-on-nfs-primary-storage-%s", msg.getPrimaryStorageUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "delete-volume-cache";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                DeleteVolumeBitsOnPrimaryStorageMsg dmsg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                dmsg.setFolder(true);
                dmsg.setHypervisorType(bkd.getHypervisorType().toString());
                dmsg.setInstallPath(new File(msg.getInstallPath()).getParent());
                dmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
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
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, sreply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                sreply.setError(errCode);
                bus.reply(msg, sreply);
            }
        }).start();
    }

    private String selectRandomHostFromPS(PrimaryStorageInventory ps) {
        List<String> cuuids = ps.getAttachedClusterUuids();
        if (cuuids.isEmpty()) {
            return null;
        }

        List<String> hosts = SQL.New("select host.uuid from ClusterVO cluster, HostVO host " +
                        "where cluster.uuid = host.clusterUuid and host.status = :hostStatus and cluster.uuid in (:cuuids) order by rand()").
                param("hostStatus", HostStatus.Connected).param("cuuids", cuuids).list();
        if (hosts == null || hosts.size() == 0) {
            return null;
        }
        return hosts.get(0);
    }

    private void handle(final GetVolumeRootImageUuidFromPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend bkd = getUsableBackend();
        if (bkd == null) {
            throw new OperationFailureException(operr("no usable backend found"));
        }

        bkd.handle(getSelfInventory(), msg, new ReturnValueCompletion<GetVolumeRootImageUuidFromPrimaryStorageReply>(msg) {
            @Override
            public void success(GetVolumeRootImageUuidFromPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                GetVolumeRootImageUuidFromPrimaryStorageReply reply = new GetVolumeRootImageUuidFromPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final UploadBitsToBackupStorageMsg msg) {
        NfsPrimaryStorageBackend bkd = getBackend(HypervisorType.valueOf(msg.getHypervisorType()));
        bkd.handle(getSelfInventory(), msg, new ReturnValueCompletion<UploadBitsToBackupStorageReply>(msg) {
            @Override
            public void success(UploadBitsToBackupStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                UploadBitsToBackupStorageReply reply = new UploadBitsToBackupStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final CreateTemporaryVolumeFromSnapshotMsg msg) {
        NfsPrimaryStorageBackend bkd = getBackend(HypervisorType.valueOf(msg.getHypervisorType()));
        bkd.handle(getSelfInventory(), msg, new ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply>(msg) {
            @Override
            public void success(CreateTemporaryVolumeFromSnapshotReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateTemporaryVolumeFromSnapshotReply reply = new CreateTemporaryVolumeFromSnapshotReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public void deleteHook() {
        List<String> cuuids = CollectionUtils.transformToList(self.getAttachedClusterRefs(), new Function<String, PrimaryStorageClusterRefVO>() {
            @Override
            public String call(PrimaryStorageClusterRefVO arg) {
                return arg.getClusterUuid();
            }
        });

        for (String cuuid : cuuids) {
            NfsPrimaryStorageBackend backend = getBackendByClusterUuid(cuuid);
            try {
                backend.detachFromCluster(getSelfInventory(), cuuid);
            } catch (NfsPrimaryStorageException e) {
                logger.warn(String.format("failed to detach the nfs primary storage[uuid: %s] from the cluster[uuid: %s]",
                        self.getUuid(), cuuid));
            }
        }
    }

    protected void handle(final MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        final MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();

        VolumeSnapshotInventory snapshot = msg.getFrom();
        VolumeInventory volume = msg.getTo();
        final NfsPrimaryStorageBackend backend = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(snapshot.getFormat(), self.getUuid()));
        backend.mergeSnapshotToVolume(getSelfInventory(), snapshot, volume, msg.isFullRebase(), new Completion(msg) {
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

    private void handle(final CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        final NfsPrimaryStorageBackend backend = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getSnapshot().getFormat(), self.getUuid()));
        backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg) {
        BackupStorageInventory bs = msg.getBackupStorage();
        VolumeSnapshotInventory sinv = msg.getSnapshot();
        NfsPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(
                BackupStorageType.valueOf(bs.getType()),
                nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(sinv.getFormat(), self.getUuid())
        );

        final BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
        final String installPath = mediator.makeVolumeSnapshotInstallPath(bs.getUuid(), sinv.getUuid());
        mediator.uploadBits(null, getSelfInventory(), bs, installPath, sinv.getPrimaryStorageInstallPath(), new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String installPath) {
                reply.setBackupStorageInstallPath(installPath);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(final RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        final RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
        HostInventory destHost;
        try {
            destHost = factory.getConnectedHostForOperation(PrimaryStorageInventory.valueOf(self)).get(0);
        }catch (OperationFailureException e){
            reply.setError(operr("no host in Connected status to which nfs primary storage[uuid:%s, name:%s] attached" +
                            " found to revert volume[uuid:%s] to snapshot[uuid:%s, name:%s]",
                    self.getUuid(), self.getName(), msg.getVolume().getUuid(),
                    msg.getSnapshot().getUuid(), msg.getSnapshot().getName()));

            bus.reply(msg, reply);
            return;
        }

        NfsPrimaryStorageBackend bkd = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getSnapshot().getFormat(), self.getUuid()));
        bkd.revertVolumeFromSnapshot(msg.getSnapshot(), msg.getVolume(), destHost, new ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(RevertVolumeFromSnapshotOnPrimaryStorageReply returnValue) {
                reply.setNewVolumeInstallPath(returnValue.getNewVolumeInstallPath());
                reply.setSize(returnValue.getSize());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected  void handle(final ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();

        HostInventory destHost = factory.getConnectedHostForOperation(PrimaryStorageInventory.valueOf(self)).get(0);
        if (destHost == null) {
            reply.setError(operr("no host in Connected status to which nfs primary storage[uuid:%s, name:%s] attached" +
                            " found to revert volume[uuid:%s] to image[uuid:%s]",
                    self.getUuid(), self.getName(),
                    msg.getVolume().getUuid(), msg.getVolume().getRootImageUuid()));

            bus.reply(msg, reply);
            return;
        }

        NfsPrimaryStorageBackend bkd = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(
                VolumeConstant.VOLUME_FORMAT_QCOW2, self.getUuid())
        );
        bkd.resetRootVolumeFromImage(msg.getVolume(), destHost, new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String returnValue) {
                reply.setNewVolumeInstallPath(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteSnapshotOnPrimaryStorageMsg msg) {
        final DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
        final VolumeSnapshotInventory sinv = msg.getSnapshot();
        final NfsPrimaryStorageBackend bkd = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(sinv.getFormat(), self.getUuid()));

        bkd.delete(getSelfInventory(), sinv.getPrimaryStorageInstallPath(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                NfsDeleteVolumeSnapshotGC gc = new NfsDeleteVolumeSnapshotGC();
                gc.NAME = String.format("gc-nfs-%s-snapshot-%s", self.getUuid(), sinv.getUuid());
                gc.snapshot = sinv;
                gc.primaryStorageUuid = self.getUuid();
                gc.hypervisorType = bkd.getHypervisorType().toString();
                gc.submit(NfsPrimaryStorageGlobalConfig.GC_INTERVAL.value(Long.class), TimeUnit.SECONDS);

                N.New(PrimaryStorageVO.class, self.getUuid()).warn_("NFS primary storage[uuid:%s] failed to delete a volume snapshot[uuid:%s], %s. A GC" +
                        " job[uuid:%s] is scheduled to cleanup it in the interval of %s seconds",
                        self.getUuid(), sinv.getUuid(), errorCode, NfsPrimaryStorageGlobalConfig.GC_INTERVAL.value(Long.class));
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final TakeSnapshotMsg msg) {
        final TakeSnapshotReply reply = new TakeSnapshotReply();

        String volumeUuid = msg.getStruct().getCurrent().getVolumeUuid();
        VolumeVO vol = dbf.findByUuid(volumeUuid, VolumeVO.class);

        String huuid;
        String connectedHostUuid = factory.getConnectedHostForOperation(getSelfInventory()).get(0).getUuid();
        if (vol.getVmInstanceUuid() != null){
            Tuple t = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.state, VmInstanceVO_.hostUuid)
                    .eq(VmInstanceVO_.uuid, vol.getVmInstanceUuid())
                    .findTuple();
            VmInstanceState state = t.get(0, VmInstanceState.class);
            String vmHostUuid = t.get(1, String.class);

            if (state == VmInstanceState.Running || state == VmInstanceState.Paused){
                DebugUtils.Assert(vmHostUuid != null,
                        String.format("vm[uuid:%s] is Running or Paused, but has no hostUuid", vol.getVmInstanceUuid()));
                huuid = vmHostUuid;
            } else if (state == VmInstanceState.Stopped){
                huuid = connectedHostUuid;
            } else {
                reply.setError(operr("vm[uuid:%s] is not Running, Paused or Stopped, current state is %s",
                        vol.getVmInstanceUuid(), state));
                bus.reply(msg, reply);
                return;
            }
        } else {
            huuid = connectedHostUuid;
        }

        VolumeInventory volInv = VolumeInventory.valueOf(vol);
        TakeSnapshotOnHypervisorMsg hmsg = new TakeSnapshotOnHypervisorMsg();
        hmsg.setHostUuid(huuid);
        hmsg.setVmUuid(vol.getVmInstanceUuid());
        hmsg.setVolume(volInv);
        hmsg.setSnapshotName(msg.getStruct().getCurrent().getUuid());
        hmsg.setFullSnapshot(msg.getStruct().isFullSnapshot());
        final String installPath = NfsPrimaryStorageKvmHelper.makeKvmSnapshotInstallPath(getSelfInventory(), volInv, msg.getStruct().getCurrent());
        hmsg.setInstallPath(installPath);
        bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, huuid);
        bus.send(hmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply ret) {
                if (ret.isSuccess()) {
                    TakeSnapshotOnHypervisorReply treply = (TakeSnapshotOnHypervisorReply) ret;
                    VolumeSnapshotInventory inv = msg.getStruct().getCurrent();
                    inv.setSize(treply.getSize());
                    inv.setPrimaryStorageUuid(self.getUuid());
                    inv.setPrimaryStorageInstallPath(treply.getSnapshotInstallPath());
                    inv.setType(VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());
                    reply.setNewVolumeInstallPath(treply.getNewVolumeInstallPath());
                    reply.setInventory(inv);
                } else {
                    reply.setError(ret.getError());
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void handle(PrimaryStorageRemoveCachedImageMsg msg) {
        if (self.getAttachedClusterRefs().isEmpty()) {
            PrimaryStorageRemoveCachedImageReply reply = new PrimaryStorageRemoveCachedImageReply();
            reply.setError(operr("primary storage[uuid:%s] doesn't attach to any cluster", self.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        PrimaryStorageClusterRefVO ref = self.getAttachedClusterRefs().iterator().next();
        ClusterVO cluster = dbf.findByUuid(ref.getClusterUuid(), ClusterVO.class);
        getBackend(HypervisorType.valueOf(cluster.getHypervisorType())).deleteImageCache(msg.getInventory());
    }


    @Transactional(readOnly = true)
    private NfsPrimaryStorageBackend getUsableBackend() {
        List<String> cuuids = CollectionUtils.transformToList(self.getAttachedClusterRefs(), new Function<String, PrimaryStorageClusterRefVO>() {
            @Override
            public String call(PrimaryStorageClusterRefVO arg) {
                return arg.getClusterUuid();
            }
        });

        if (cuuids.isEmpty()) {
            return null;
        }

        String sql = "select cluster.uuid from ClusterVO cluster, HostVO host where cluster.uuid = host.clusterUuid and host.status = :hostStatus and cluster.uuid in (:cuuids)";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("hostStatus", HostStatus.Connected);
        q.setParameter("cuuids", cuuids);
        cuuids = q.getResultList();

        if (cuuids.isEmpty()) {
            return null;
        }

        return getBackendByClusterUuid(cuuids.get(0));
    }

    private NfsPrimaryStorageBackend getBackendByClusterUuid(String clusterUuid) {
        SimpleQuery<ClusterVO> query = dbf.createQuery(ClusterVO.class);
        query.select(ClusterVO_.hypervisorType);
        query.add(ClusterVO_.uuid, Op.EQ, clusterUuid);
        String hvType = query.findValue();
        return getBackend(HypervisorType.valueOf(hvType));
    }

    private NfsPrimaryStorageBackend getBackend(HypervisorType hvType) {
        return factory.getHypervisorBackend(hvType);
    }

    @Override
    public void attachHook(String clusterUuid, Completion completion) {
        NfsPrimaryStorageBackend backend = getBackendByClusterUuid(clusterUuid);
        backend.attachToCluster(PrimaryStorageInventory.valueOf(self), clusterUuid, new ReturnValueCompletion<Boolean>(completion) {
            @Override
            public void success(Boolean ret) {
                if(ret){
                    changeStatus(PrimaryStorageStatus.Connected);
                }
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }


    @Override
    public void detachHook(String clusterUuid, Completion completion) {
        NfsPrimaryStorageBackend backend = getBackendByClusterUuid(clusterUuid);
        try {
            backend.detachFromCluster(PrimaryStorageInventory.valueOf(self), clusterUuid);
            completion.success();
        } catch (NfsPrimaryStorageException e) {
            completion.fail(errf.throwableToOperationError(e));
        }
    }

    private void handle(final InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg msg) throws PrimaryStorageException {
        final InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
        final ImageSpec ispec = msg.getTemplateSpec();

        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.select(BackupStorageVO_.type);
        q.add(BackupStorageVO_.uuid, Op.EQ, ispec.getSelectedBackupStorage().getBackupStorageUuid());
        final String bsType = q.findValue();

        final VolumeInventory volume = msg.getVolume();

        final ImageInventory image = ispec.getInventory();
        if (ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType())) {
            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("create-root-volume-from-image-%s", image.getUuid()));
            chain.then(new ShareFlow() {
                PrimaryStorageInventory primaryStorage = getSelfInventory();
                ImageCacheInventory imageCache;
                String volumeInstallPath;

                @Override
                public void setup() {
                    flow(new NoRollbackFlow() {
                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            NfsDownloadImageToCacheJob job = new NfsDownloadImageToCacheJob();
                            job.setImage(ispec);
                            job.setPrimaryStorage(primaryStorage);

                            jobf.execute(NfsPrimaryStorageKvmHelper.makeDownloadImageJobName(image, primaryStorage),
                                    NfsPrimaryStorageKvmHelper.makeJobOwnerName(primaryStorage), job,
                                    new ReturnValueCompletion<ImageCacheInventory>(trigger) {
                                        @Override
                                        public void success(ImageCacheInventory returnValue) {
                                            imageCache = returnValue;
                                            trigger.next();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            trigger.fail(errorCode);
                                        }
                                    }, ImageCacheInventory.class);
                        }
                    });

                    flow(new NoRollbackFlow() {
                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            NfsPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(
                                    BackupStorageType.valueOf(bsType),
                                    nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(image.getFormat(), self.getUuid())
                            );

                            mediator.createVolumeFromImageCache(primaryStorage, imageCache, volume, new ReturnValueCompletion<String>(trigger) {
                                @Override
                                public void success(String returnValue) {
                                    volumeInstallPath = returnValue;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    done(new FlowDoneHandler(msg) {
                        @Override
                        public void handle(Map data) {
                            volume.setInstallPath(volumeInstallPath);
                            volume.setFormat(image.getFormat());
                            reply.setVolume(volume);
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
        } else {
            createEmptyVolume(msg);
        }
    }

    private void createEmptyVolume(final InstantiateVolumeOnPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend backend;
        if (msg.getDestHost() != null) {
            backend = getBackend(HypervisorType.valueOf(msg.getDestHost().getHypervisorType()));
        } else {
            backend = getUsableBackend();
            if (backend == null) {
                throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find any usable host to" +
                                        " create the data volume[uuid:%s, name:%s]", self.getUuid(), self.getName(),
                                msg.getVolume().getUuid(), msg.getVolume().getName()));
            }
        }

        VolumeInventory vol = msg.getVolume();
        final InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
        backend.instantiateVolume(PrimaryStorageInventory.valueOf(self), vol, new ReturnValueCompletion<VolumeInventory>(msg) {
            @Override
            public void success(VolumeInventory returnValue) {
                reply.setVolume(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                reply.setSuccess(false);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(GetPrimaryStorageFolderListMsg msg) {
        GetPrimaryStorageFolderListReply reply = new GetPrimaryStorageFolderListReply();
        String hostUuid = getAvailableHostUuidForOperation();
        if (hostUuid == null) {
            bus.reply(msg, reply);
            return;
        }
        String type = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).select(HostVO_.hypervisorType).findValue();
        NfsPrimaryStorageBackend bkd = getBackend(HypervisorType.valueOf(type));

        bkd.list(getSelfInventory(), msg.getPath(), new ReturnValueCompletion<List<String>>(msg) {
            @Override
            public void success(List<String> paths) {
                reply.setFolders(paths);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(InstantiateVolumeOnPrimaryStorageMsg msg) {
        try {
            if (msg.getClass() == InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg.class) {
                handle((InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg);
            } else {
                createEmptyVolume(msg);
            }
        } catch (PrimaryStorageException e) {
            logger.warn(e.getMessage(), e);
            InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
            reply.setError(errf.throwableToOperationError(e));
            bus.reply(msg, reply);
        }
    }

    @Override
    protected void handle(final DeleteVolumeOnPrimaryStorageMsg msg) {
        final DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
        final VolumeInventory vol = msg.getVolume();
        final NfsPrimaryStorageBackend backend = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(vol.getFormat(), self.getUuid()));
        backend.delete(getSelfInventory(), vol.getInstallPath(), new Completion(msg) {
            @Override
            public void success() {
                logger.debug(String.format("successfully delete volume[uuid:%s, installPath:%s] on nfs primary storage[uuid:%s]", vol.getUuid(),
                        vol.getInstallPath(), self.getUuid()));
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                NfsDeleteVolumeGC gc = new NfsDeleteVolumeGC();
                gc.NAME = String.format("gc-nfs-%s-volume-%s", self.getUuid(), vol.getUuid());
                gc.primaryStorageUuid = self.getUuid();
                gc.hypervisorType = backend.getHypervisorType().toString();
                gc.volume = vol;
                gc.submit(NfsPrimaryStorageGlobalConfig.GC_INTERVAL.value(Long.class), TimeUnit.SECONDS);

                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        final CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();
        final VolumeInventory volume = msg.getVolumeInventory();
        BackupStorageVO bsvo = dbf.findByUuid(msg.getBackupStorageUuid(), BackupStorageVO.class);
        final BackupStorageInventory bsinv = BackupStorageInventory.valueOf(bsvo);
        final PrimaryStorageInventory pinv = getSelfInventory();
        final ImageInventory image = msg.getImageInventory();

        final TaskProgressRange parentStage = getTaskStage();
        final TaskProgressRange CREATE_TEMPORARY_TEMPLATE_STAGE = new TaskProgressRange(0, 30);
        final TaskProgressRange TEMPLATE_UPLOAD_STAGE = new TaskProgressRange(30, 100);


        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-template-from-volume-%s-on-nfs-primary-storage-%s", volume.getUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            String templatePrimaryStorageInstallPath;
            String templateBackupStorageInstallPath;

            @Override
            public void setup() {
                flow(new Flow() {
                    NfsPrimaryStorageBackend bkd = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(volume.getFormat(), self.getUuid()));

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        TaskProgressRange stage = markTaskStage(parentStage, CREATE_TEMPORARY_TEMPLATE_STAGE);

                        bkd.createTemplateFromVolume(pinv, volume, image, new ReturnValueCompletion<String>(trigger) {
                            @Override
                            public void success(String returnValue) {
                                templatePrimaryStorageInstallPath = returnValue;
                                reportProgress(stage.getEnd().toString());
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
                        if (templatePrimaryStorageInstallPath != null) {
                            bkd.delete(pinv, templatePrimaryStorageInstallPath, new NopeCompletion());
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        TaskProgressRange stage = markTaskStage(parentStage, TEMPLATE_UPLOAD_STAGE);

                        NfsPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(
                                BackupStorageType.valueOf(bsinv.getType()),
                                nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(volume.getFormat(), self.getUuid())
                        );

                        DebugUtils.Assert(!ImageMediaType.ISO.toString().equals(image.getMediaType()), String.format("how can this happen? creating an template from an ISO????"));
                        templateBackupStorageInstallPath = ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType()) ?
                                mediator.makeRootVolumeTemplateInstallPath(bsinv.getUuid(), image.getUuid()) : mediator.makeDataVolumeTemplateInstallPath(bsinv.getUuid(), image.getUuid());
                        mediator.uploadBits(msg.getImageInventory().getUuid(), pinv, bsinv, templateBackupStorageInstallPath, templatePrimaryStorageInstallPath, new ReturnValueCompletion<String>(trigger) {
                            @Override
                            public void success(String installPath) {
                                reportProgress(stage.getEnd().toString());
                                templateBackupStorageInstallPath = installPath;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        reply.setTemplateBackupStorageInstallPath(templateBackupStorageInstallPath);
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

    @Override
    protected void handle(final DownloadDataVolumeToPrimaryStorageMsg msg) {
        final DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
        final String installPath = PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeDataVolumeInstallPath(msg.getVolumeUuid()));
        BackupStorageVO bsvo = dbf.findByUuid(msg.getBackupStorageRef().getBackupStorageUuid(), BackupStorageVO.class);
        NfsPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(
                BackupStorageType.valueOf(bsvo.getType()),
                nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getImage().getFormat(), self.getUuid())
        );

        mediator.downloadBits(getSelfInventory(), BackupStorageInventory.valueOf(bsvo),
                msg.getBackupStorageRef().getInstallPath(), installPath, new Completion(msg) {
                    @Override
                    public void success() {
                        reply.setInstallPath(installPath);
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
    }

    @Override
    protected void handle(GetInstallPathForDataVolumeDownloadMsg msg) {
        final GetInstallPathForDataVolumeDownloadReply reply = new GetInstallPathForDataVolumeDownloadReply();
        final String installPath = PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeDataVolumeInstallPath(msg.getVolumeUuid()));
        reply.setInstallPath(installPath);
        bus.reply(msg, reply);

    }

    @Override
    protected void handle(final DeleteVolumeBitsOnPrimaryStorageMsg msg) {
        final DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
        NfsPrimaryStorageBackend bkd = getBackend(HypervisorType.valueOf(msg.getHypervisorType()));
        if (msg.isFolder()) {
            bkd.deleteFolder(getSelfInventory(), msg.getInstallPath(), new Completion(msg) {
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
        } else {
            bkd.delete(getSelfInventory(), msg.getInstallPath(), new Completion(msg) {
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
    }

    @Override
    protected void handle(final DeleteBitsOnPrimaryStorageMsg msg) {
        final DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
        String hostUuid = getAvailableHostUuidForOperation();
        if (hostUuid == null) {
            bus.reply(msg, reply);
            return;
        }
        String type = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).select(HostVO_.hypervisorType).findValue();
        NfsPrimaryStorageBackend bkd = getBackend(HypervisorType.valueOf(type));
        if (msg.isFolder()) {
            bkd.deleteFolder(getSelfInventory(), msg.getInstallPath(), new Completion(msg) {
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
        } else {
            bkd.delete(getSelfInventory(), msg.getInstallPath(), new Completion(msg) {
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
    }

    @Override
    protected void handle(final DownloadIsoToPrimaryStorageMsg msg) {
        final DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
        final PrimaryStorageInventory pinv = getSelfInventory();
        NfsDownloadImageToCacheJob job = new NfsDownloadImageToCacheJob();
        job.setPrimaryStorage(pinv);
        job.setImage(msg.getIsoSpec());

        final ImageInventory img = msg.getIsoSpec().getInventory();
        jobf.execute(NfsPrimaryStorageKvmHelper.makeDownloadImageJobName(msg.getIsoSpec().getInventory(), pinv),
                NfsPrimaryStorageKvmHelper.makeJobOwnerName(pinv), job,
                new ReturnValueCompletion<ImageCacheInventory>(msg) {

                    @Override
                    public void success(ImageCacheInventory returnValue) {
                        logger.debug(String.format("successfully downloaded iso[uuid:%s, name:%s] from backup storage[uuid:%s] to primary storage[uuid:%s, name:%s], path in cache: %s",
                                img.getUuid(), img.getName(), msg.getIsoSpec().getSelectedBackupStorage().getBackupStorageUuid(),
                                pinv.getUuid(), pinv.getName(), returnValue.getInstallUrl()));

                        reply.setInstallPath(returnValue.getInstallUrl());
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        String err = String.format("failed to downloaded iso[uuid:%s, name:%s] from backup storage[uuid:%s] to primary storage[uuid:%s, name:%s]",
                                img.getUuid(), img.getName(), msg.getIsoSpec().getSelectedBackupStorage().getBackupStorageUuid(),
                                pinv.getUuid(), pinv.getName());
                        logger.warn(err);
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                }, ImageCacheInventory.class);

    }

    @Override
    protected void handle(DeleteIsoFromPrimaryStorageMsg msg) {
        DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        VolumeSnapshotCapability capability = new VolumeSnapshotCapability();
        HypervisorType hvType = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getVolume().getFormat());
        if (hvType.toString().equals(KVMConstant.KVM_HYPERVISOR_TYPE)) {
            capability.setArrangementType(VolumeSnapshotArrangementType.CHAIN);
            capability.setSupport(true);
        } else {
            capability.setSupport(false);
        }

        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        reply.setCapability(capability);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(final SyncVolumeSizeOnPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                            self.getUuid(), self.getName()));
        }

        backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(SyncVolumeSizeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(NfsToNfsMigrateVolumeMsg msg) {
        NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                    self.getUuid(), self.getName()));
        }

        backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<NfsToNfsMigrateVolumeReply>(msg) {
            @Override
            public void success(NfsToNfsMigrateVolumeReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                NfsToNfsMigrateVolumeReply reply = new NfsToNfsMigrateVolumeReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(NfsRebaseVolumeBackingFileMsg msg) {
        NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                    self.getUuid(), self.getName()));
        }
        backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<NfsRebaseVolumeBackingFileReply>(msg) {
            @Override
            public void success(NfsRebaseVolumeBackingFileReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                NfsRebaseVolumeBackingFileReply reply = new NfsRebaseVolumeBackingFileReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(NfsRecalculatePrimaryStorageCapacityMsg msg) {
        if (msg.isRelease()) {
            doReleasePrimaryStorageCapacity();
        } else {
            RecalculatePrimaryStorageCapacityMsg rmsg = new RecalculatePrimaryStorageCapacityMsg();
            rmsg.setPrimaryStorageUuid(self.getUuid());
            bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
            bus.send(rmsg);
        }
    }

    private void doReleasePrimaryStorageCapacity() {
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(self.getUuid());
        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                cap.setAvailableCapacity(0L);
                cap.setAvailablePhysicalCapacity(0L);
                cap.setSystemUsedCapacity(0L);
                cap.setTotalPhysicalCapacity(0L);
                cap.setTotalCapacity(0L);
                return cap;
            }
        });
    }

    @Override
    protected void connectHook(ConnectParam param, final Completion completion) {
        final NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            // the nfs primary storage has not been attached to any clusters, or no connected hosts
            completion.fail(errf.instantiateErrorCode(PrimaryStorageErrors.DISCONNECTED,
                    String.format("the NFS primary storage[uuid:%s, name:%s] has not attached to any clusters, or no hosts in the" +
                            " attached clusters are connected", self.getUuid(), self.getName())
            ));

            return;
        }

        logger.debug(String.format("reconnect-nfs-primary-storage-%s", self.getUuid()));
        SimpleQuery<PrimaryStorageClusterRefVO> q = dbf.createQuery(PrimaryStorageClusterRefVO.class);
        q.select(PrimaryStorageClusterRefVO_.clusterUuid);
        q.add(PrimaryStorageClusterRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
        List<String> cuuids = q.listValue();
        if (cuuids.isEmpty()) {
            completion.success();
            return;
        }

        PrimaryStorageInventory inv = getSelfInventory();
        new LoopAsyncBatch<String>(completion) {
            boolean success;

            @Override
            protected Collection<String> collect() {
                return cuuids;
            }

            @Override
            protected AsyncBatchRunner forEach(String cuuid) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        NfsPrimaryStorageBackend bkd = getBackendByClusterUuid(cuuid);
                        bkd.remount(inv, cuuid, new Completion(completion) {
                            @Override
                            public void success() {
                                success = true;
                                completion.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                errors.add(errorCode);
                                completion.done();
                            }
                        });
                    }
                };
            }

            @Override
            protected void done() {
                if (success) {
                    self = dbf.reload(self);
                    completion.success();
                } else {
                    completion.fail(errf.stringToOperationError(String.format("unable to connect the" +
                            "NFS primary storage[uuid:%s, name:%s]", self.getUuid(), self.getName()), errors));
                }
            }
        }.start();
    }

    @Override
    protected void pingHook(Completion completion) {
        NfsPrimaryStorageBackend bkd = getUsableBackend();
        if (bkd == null) {
            // the nfs primary storage has not been attached to any clusters, or no connected hosts
            completion.fail(operr("the NFS primary storage[uuid:%s, name:%s] has not attached to any clusters, or no hosts in the" +
                            " attached clusters are connected", self.getUuid(), self.getName()));
        } else {
            bkd.ping(getSelfInventory(), completion);
        }
    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            PhysicalCapacityUsage usage = new PhysicalCapacityUsage();
            usage.availablePhysicalSize = 0;
            usage.totalPhysicalSize = 0;
            completion.success(usage);
        } else {
            backend.getPhysicalCapacity(getSelfInventory(), completion);
        }
    }

    private String getAvailableHostUuidForOperation() {
        List<String> hostUuids = Q.New(PrimaryStorageHostRefVO.class).
                eq(PrimaryStorageHostRefVO_.primaryStorageUuid, self.getUuid()).select(PrimaryStorageHostRefVO_.hostUuid).listValues();
        if (hostUuids == null || hostUuids.size() == 0) {
            return null;
        }
        return hostUuids.get(0);
    }

    @Override
    public void handle(AskInstallPathForNewSnapshotMsg msg) {
        NfsPrimaryStorageBackend bkd = getUsableBackend();
        bkd.handle(getSelfInventory(), msg, new ReturnValueCompletion<AskInstallPathForNewSnapshotReply>(msg) {
            @Override
            public void success(AskInstallPathForNewSnapshotReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                AskInstallPathForNewSnapshotReply reply = new AskInstallPathForNewSnapshotReply();
                reply.setSuccess(false);
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }
}
