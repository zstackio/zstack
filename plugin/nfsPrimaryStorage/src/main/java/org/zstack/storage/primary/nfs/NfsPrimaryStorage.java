package org.zstack.storage.primary.nfs;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.cloudbus.AutoOffEventCallback;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.gc.GCFacade;
import org.zstack.core.gc.TimeBasedGCPersistentContext;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.host.HostCanonicalEvents.HostStatusChangedData;
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
import org.zstack.storage.primary.PrimaryStoragePathMaker;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class NfsPrimaryStorage extends PrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(NfsPrimaryStorage.class);

    @Autowired
    private NfsPrimaryStorageFactory factory;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private NfsPrimaryStorageManager nfsMgr;
    @Autowired
    private GCFacade gcf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private NfsPrimaryStorageImageCacheCleaner imageCacheCleaner;

    public NfsPrimaryStorage(PrimaryStorageVO vo) {
        super(vo);
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof PrimaryStorageRemoveCachedImageMsg) {
            handle((PrimaryStorageRemoveCachedImageMsg) msg);
        } else if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof DeleteSnapshotOnPrimaryStorageMsg) {
            handle((DeleteSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof RevertVolumeFromSnapshotOnPrimaryStorageMsg) {
            handle((RevertVolumeFromSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) {
            handle((BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof MergeVolumeSnapshotOnPrimaryStorageMsg) {
            handle((MergeVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateTemporaryVolumeFromSnapshotMsg) {
            handle((CreateTemporaryVolumeFromSnapshotMsg) msg);
        } else if (msg instanceof UploadBitsToBackupStorageMsg) {
            handle((UploadBitsToBackupStorageMsg) msg);
        } else if (msg instanceof GetVolumeRootImageUuidFromPrimaryStorageMsg) {
            handle((GetVolumeRootImageUuidFromPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteImageCacheOnPrimaryStorageMsg) {
            handle((DeleteImageCacheOnPrimaryStorageMsg) msg);
        } else if (msg instanceof ReInitRootVolumeFromTemplateOnPrimaryStorageMsg) {
            handle((ReInitRootVolumeFromTemplateOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    protected void updateMountPoint(PrimaryStorageVO vo, String newUrl) {
        Future<ErrorCode> future = thdf.syncSubmit(new SyncTask<ErrorCode>() {
            @Override
            public ErrorCode call() throws Exception {
                ErrorCode err = new NfsApiParamChecker().checkUrl(self.getZoneUuid(), newUrl);
                if (err != null) {
                    return err;
                }

                String oldUrl = self.getUrl();

                checkRunningVmForUpdateUrl();
                vo.setUrl(newUrl);
                dbf.update(vo);

                SimpleQuery<PrimaryStorageClusterRefVO> q = dbf.createQuery(PrimaryStorageClusterRefVO.class);
                q.select(PrimaryStorageClusterRefVO_.clusterUuid);
                q.add(PrimaryStorageClusterRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
                List<String> cuuids = q.listValue();
                if (cuuids.isEmpty()) {
                    return null;
                }

                FutureCompletion completion = new FutureCompletion();

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
                                        //TODO: bring the host to an error state
                                        logger.warn(String.format("failed to update the nfs[uuid:%s, name:%s] mount point" +
                                                        " from %s to %s in the cluster[uuid:%s], %s", self.getUuid(), self.getName(),
                                                oldUrl, newUrl, item, errorCode));
                                        completion.done();
                                    }
                                });
                            }
                        };
                    }

                    @Override
                    protected void done() {
                        completion.success();
                    }
                }.start();

                completion.await();

                if (completion.isSuccess()) {
                    return null;
                } else {
                    return completion.getErrorCode();
                }
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public String getSyncSignature() {
                return String.format("nfs-update-url-%s", self.getUuid());
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }
        });

        try {
            ErrorCode err = future.get();
            if (err != null) {
                throw new OperationFailureException(err);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    protected PrimaryStorageVO updatePrimaryStorage(APIUpdatePrimaryStorageMsg msg) {
        PrimaryStorageVO vo = super.updatePrimaryStorage(msg);
        vo = vo == null ? self : vo;

        if (msg.getUrl() != null && !self.getUrl().equals(msg.getUrl())) {
            updateMountPoint(vo, msg.getUrl());
        }

        return vo;
    }

    @Transactional(readOnly = true)
    private void checkRunningVmForUpdateUrl() {
        String sql = "select vm.name, vm.uuid from VmInstanceVO vm, VolumeVO vol where vm.uuid = vol.vmInstanceUuid and" +
                " vol.primaryStorageUuid = :psUuid and vm.state = :vmState";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("psUuid", self.getUuid());
        q.setParameter("vmState", VmInstanceState.Running);
        List<Tuple> ts = q.getResultList();

        if (!ts.isEmpty()) {
            List<String> vms = ts.stream().map(v -> String.format("VM[name:%s, uuid:%s]", v.get(0, String.class), v.get(1, String.class))).collect(Collectors.toList());
            throw new OperationFailureException(errf.stringToOperationError(String.format("there are %s running VMs on the NFS primary storage, please" +
                    " stop them and try again:\n%s\n", vms.size(), StringUtils.join(vms, "\n"))));
        }
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
            throw new OperationFailureException(errf.stringToOperationError("cannot find usable backend"));
        }

        DeleteBitsOnPrimaryStorageMsg dmsg = new DeleteBitsOnPrimaryStorageMsg();
        dmsg.setHypervisorType(bkd.getHypervisorType().toString());
        dmsg.setInstallPath(msg.getInstallPath());
        dmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
        bus.send(dmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                DeleteImageCacheOnPrimaryStorageReply r = new DeleteImageCacheOnPrimaryStorageReply();
                r.setSuccess(reply.isSuccess());
                if (reply.getError() != null) {
                    r.setError(reply.getError());
                }
                bus.reply(msg, r);
            }
        });
    }

    private void handle(final GetVolumeRootImageUuidFromPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend bkd = getUsableBackend();
        if (bkd == null) {
            throw new OperationFailureException(errf.stringToOperationError("no usable backend found"));
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

    private void handle(final MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
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
        mediator.uploadBits(getSelfInventory(), bs, installPath, sinv.getPrimaryStorageInstallPath(), new ReturnValueCompletion<String>(msg) {
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

    private void handle(final RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        final RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();

        HostInventory destHost = factory.getConnectedHostForOperation(PrimaryStorageInventory.valueOf(self));
        if (destHost == null) {
            reply.setError(errf.stringToOperationError(
                    String.format("no host in Connected status to which nfs primary storage[uuid:%s, name:%s] attached" +
                                    " found to revert volume[uuid:%s] to snapshot[uuid:%s, name:%s]",
                            self.getUuid(), self.getName(), msg.getVolume().getUuid(),
                            msg.getSnapshot().getUuid(), msg.getSnapshot().getName())
            ));

            bus.reply(msg, reply);
            return;
        }

        NfsPrimaryStorageBackend bkd = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getSnapshot().getFormat(), self.getUuid()));
        bkd.revertVolumeFromSnapshot(msg.getSnapshot(), msg.getVolume(), destHost, new ReturnValueCompletion<String>(msg) {
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

    private void handle(final ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();

        HostInventory destHost = factory.getConnectedHostForOperation(PrimaryStorageInventory.valueOf(self));
        if (destHost == null) {
            reply.setError(errf.stringToOperationError(
                    String.format("no host in Connected status to which nfs primary storage[uuid:%s, name:%s] attached" +
                                    " found to revert volume[uuid:%s] to image[uuid:%s]",
                            self.getUuid(), self.getName(),
                            msg.getVolume().getUuid(), msg.getVolume().getRootImageUuid()
                    )
            ));

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

    private void handle(final DeleteSnapshotOnPrimaryStorageMsg msg) {
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
                GCBitsDeletionContext c = new GCBitsDeletionContext();
                c.setPrimaryStorageUuid(self.getUuid());
                c.setSnapshot(sinv);
                c.setHypervisorType(bkd.getHypervisorType().toString());

                TimeBasedGCPersistentContext<GCBitsDeletionContext> ctx = new TimeBasedGCPersistentContext();
                ctx.setContextClass(GCBitsDeletionContext.class);
                ctx.setRunnerClass(GCBitsDeletionRunner.class);
                ctx.setContext(c);
                ctx.setInterval(NfsPrimaryStorageGlobalProperty.BITS_DELETION_GC_INTERVAL);
                ctx.setName(String.format("nfs-gc-volume-snapshot-%s-%s", self.getUuid(), sinv.getUuid()));
                gcf.schedule(ctx);

                //TODO: alarm
                logger.warn(String.format("failed to delete the volume snapshot[uuid:%s], %s. GC job is submitted",
                        sinv.getUuid(), errorCode));
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final TakeSnapshotMsg msg) {
        final TakeSnapshotReply reply = new TakeSnapshotReply();

        String volumeUuid = msg.getStruct().getCurrent().getVolumeUuid();
        VolumeVO vol = dbf.findByUuid(volumeUuid, VolumeVO.class);

        String huuid = null;
        if (vol.getVmInstanceUuid() != null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state, VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid);
            q.add(VmInstanceVO_.uuid, Op.EQ, vol.getVmInstanceUuid());
            Tuple t = q.findTuple();
            VmInstanceState vmState = t.get(0, VmInstanceState.class);
            String hostUuid = t.get(1, String.class);
            String lastHostUuid = t.get(2, String.class);
            if (vmState != VmInstanceState.Running && vmState != VmInstanceState.Stopped) {
                ErrorCode err = errf.stringToOperationError(String.format("vm[uuid:%s] is not Running or Stopped, current state is %s",
                        vol.getVmInstanceUuid(), vmState));
                reply.setError(err);
                bus.reply(msg, reply);
                return;
            }

            huuid = VmInstanceState.Running == vmState ? hostUuid : lastHostUuid;
        } else {
            HostInventory host = factory.getConnectedHostForOperation(getSelfInventory());
            huuid = host.getUuid();
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
            errf.stringToOperationError(String.format("primary storage[uuid:%s] doesn't attach to any cluster", self.getUuid()));
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
        try {
            boolean ret = backend.attachToCluster(PrimaryStorageInventory.valueOf(self), clusterUuid);
            if (ret) {
                changeStatus(PrimaryStorageStatus.Connected);
            }

            completion.success();
        } catch (NfsPrimaryStorageException e) {
            completion.fail(errf.throwableToOperationError(e));
        }
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

                            mediator.createVolumeFromImageCache(primaryStorage, imageCache, volume, new ReturnValueCompletion<String>() {
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
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("the NFS primary storage[uuid:%s, name:%s] cannot find any usable host to" +
                                        " create the data volume[uuid:%s, name:%s]", self.getUuid(), self.getName(),
                                msg.getVolume().getUuid(), msg.getVolume().getName())
                ));
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
        String volumeFolder = PathUtil.parentFolder(vol.getInstallPath());
        backend.deleteFolder(getSelfInventory(), volumeFolder, new Completion(msg) {
            @Override
            public void success() {
                logger.debug(String.format("successfully delete volume[uuid:%s, installPath:%s] on nfs primary storage[uuid:%s]", vol.getUuid(),
                        vol.getInstallPath(), self.getUuid()));
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                GCBitsDeletionContext c = new GCBitsDeletionContext();
                c.setPrimaryStorageUuid(self.getUuid());
                c.setVolume(vol);
                c.setHypervisorType(backend.getHypervisorType().toString());

                TimeBasedGCPersistentContext<GCBitsDeletionContext> ctx = new TimeBasedGCPersistentContext<GCBitsDeletionContext>();
                ctx.setContext(c);
                ctx.setRunnerClass(GCBitsDeletionRunner.class);
                ctx.setContextClass(GCBitsDeletionContext.class);
                ctx.setName(String.format("nfs-gc-volume-%s-%s", self.getUuid(), vol.getUuid()));
                ctx.setInterval(NfsPrimaryStorageGlobalProperty.BITS_DELETION_GC_INTERVAL);
                gcf.schedule(ctx);

                //TODO: send alarm
                logger.warn(String.format("failed to delete the volume[uuid:%s] on the nfs primary storage[uuid:%s], a GC job is submitted",
                        vol.getUuid(), self.getUuid()));

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
                        bkd.createTemplateFromVolume(pinv, volume, image, new ReturnValueCompletion<String>(trigger) {
                            @Override
                            public void success(String returnValue) {
                                templatePrimaryStorageInstallPath = returnValue;
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
                        NfsPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(
                                BackupStorageType.valueOf(bsinv.getType()),
                                nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(volume.getFormat(), self.getUuid())
                        );

                        DebugUtils.Assert(!ImageMediaType.ISO.toString().equals(image.getMediaType()), String.format("how can this happen? creating an template from an ISO????"));
                        templateBackupStorageInstallPath = ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType()) ?
                                mediator.makeRootVolumeTemplateInstallPath(bsinv.getUuid(), image.getUuid()) : mediator.makeDataVolumeTemplateInstallPath(bsinv.getUuid(), image.getUuid());
                        mediator.uploadBits(pinv, bsinv, templateBackupStorageInstallPath, templatePrimaryStorageInstallPath, new ReturnValueCompletion<String>(trigger) {
                            @Override
                            public void success(String installPath) {
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
    protected void handle(final DeleteBitsOnPrimaryStorageMsg msg) {
        final DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
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
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                            self.getUuid(), self.getName())
            ));
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

    protected void hookToKVMHostConnectedEventToChangeStatusToConnected() {
        // hook on host connected event to reconnect the primary storage once there is
        // a host connected in attached clusters
        evtf.on(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, new AutoOffEventCallback() {
            {
                uniqueIdentity = String.format("connect-nfs-%s-when-host-connected", self.getUuid());
            }

            @Override
            protected boolean run(Map tokens, Object data) {
                HostStatusChangedData d = (HostStatusChangedData) data;
                if (!HostStatus.Connected.toString().equals(d.getNewStatus())) {
                    return false;
                }

                if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(d.getInventory().getHypervisorType())) {
                    return false;
                }

                self = dbf.reload(self);
                if (self.getStatus() == PrimaryStorageStatus.Connected) {
                    return true;
                }

                if (!self.getAttachedClusterRefs().stream()
                        .anyMatch(ref -> ref.getClusterUuid().equals(d.getInventory().getClusterUuid()))) {
                    return false;
                }

                FutureCompletion future = new FutureCompletion();

                ConnectParam p = new ConnectParam();
                p.setNewAdded(false);
                connectHook(p, future);

                future.await();

                if (!future.isSuccess()) {
                    //TODO
                    logger.warn(String.format("%s", future.getErrorCode()));
                }

                return future.isSuccess();
            }
        });
    }

    @Override
    protected void connectHook(ConnectParam param, final Completion completion) {
        final NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            if (!param.isNewAdded()) {
                hookToKVMHostConnectedEventToChangeStatusToConnected();
            }

            // the nfs primary storage has not been attached to any clusters, or no connected hosts
            completion.fail(errf.instantiateErrorCode(PrimaryStorageErrors.DISCONNECTED,
                    String.format("the NFS primary storage[uuid:%s, name:%s] has not attached to any clusters, or no hosts in the" +
                            " attached clusters are connected", self.getUuid(), self.getName())
            ));

            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("reconnect-nfs-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "ping";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        backend.ping(getSelfInventory(), new Completion(trigger) {
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
                    String __name__ = "remount";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        SimpleQuery<PrimaryStorageClusterRefVO> q = dbf.createQuery(PrimaryStorageClusterRefVO.class);
                        q.select(PrimaryStorageClusterRefVO_.clusterUuid);
                        q.add(PrimaryStorageClusterRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
                        List<String> cuuids = q.listValue();

                        if (cuuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        PrimaryStorageInventory inv = getSelfInventory();

                        new LoopAsyncBatch<String>(trigger) {
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
                                    trigger.next();
                                } else {
                                    trigger.fail(errf.stringToOperationError(String.format("unable to connect the" +
                                            "NFS primary storage[uuid:%s, name:%s]", self.getUuid(), self.getName()), errors));
                                }
                            }
                        }.start();
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

    @Override
    protected void pingHook(Completion completion) {
        NfsPrimaryStorageBackend bkd = getUsableBackend();
        if (bkd == null) {
            hookToKVMHostConnectedEventToChangeStatusToConnected();

            // the nfs primary storage has not been attached to any clusters, or no connected hosts
            completion.fail(errf.stringToOperationError(
                    String.format("the NFS primary storage[uuid:%s, name:%s] has not attached to any clusters, or no hosts in the" +
                            " attached clusters are connected", self.getUuid(), self.getName())
            ));
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
}
