package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.*;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg.SnapshotDownloadInfo;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotReply.CreateTemplateFromVolumeSnapshotResult;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMIsoTO;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.storage.primary.PrimaryStoragePathMaker;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageBackend.CreateBitsFromSnapshotResult;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NfsPrimaryStorage extends PrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(NfsPrimaryStorage.class);

    @Autowired
    private NfsPrimaryStorageFactory factory;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private NfsPrimaryStorageManager nfsMgr;

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
        } else if (msg instanceof CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof MergeVolumeSnapshotOnPrimaryStorageMsg) {
            handle((MergeVolumeSnapshotOnPrimaryStorageMsg)msg);
        } else {
            super.handleLocalMessage(msg);
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
        final List<SnapshotDownloadInfo> infos = msg.getSnapshots();
        final VolumeSnapshotInventory sinv = infos.get(0).getSnapshot();
        final NfsPrimaryStorageBackend backend = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(sinv.getFormat(), self.getUuid()));
        final PrimaryStorageInventory pinv = getSelfInventory();

        final CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-data-volume-from-snapshot-on-nfs-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            String volumeInWorkSpacePath;
            long volumeSize;
            final String volumeInstallPath = NfsPrimaryStorageKvmHelper.makeDataVolumeInstallUrl(pinv, msg.getVolumeUuid());

            @Override
            public void setup() {
                flow(new Flow() {
                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        backend.createDataVolumeFromVolumeSnapshot(pinv, infos,
                                msg.getVolumeUuid(),
                                msg.isNeedDownload(),
                                new ReturnValueCompletion<CreateBitsFromSnapshotResult>(msg) {
                                    @Override
                                    public void success(CreateBitsFromSnapshotResult returnValue) {
                                        volumeInWorkSpacePath = returnValue.getInstallPath();
                                        volumeSize = returnValue.getSize();
                                        trigger.next();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.fail(errorCode);
                                    }
                                });
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        backend.delete(pinv, volumeInWorkSpacePath, new NopeCompletion());
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        backend.moveBits(pinv, volumeInWorkSpacePath, volumeInstallPath, new Completion(trigger) {
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

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        reply.setInstallPath(volumeInstallPath);
                        reply.setSize(volumeSize);
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

    private void handle(final CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        final CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply();
        createTemplateFromSnapshot(msg, new ReturnValueCompletion<CreateTemplateFromSnapshotResultStruct>(msg) {
            @Override
            public void success(CreateTemplateFromSnapshotResultStruct returnValue) {
                reply.setResults(returnValue.results);
                reply.setSize(returnValue.size);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private class CreateTemplateFromSnapshotResultStruct {
        List<CreateTemplateFromVolumeSnapshotResult> results;
        long size;
    }

    private void createTemplateFromSnapshot(final CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<CreateTemplateFromSnapshotResultStruct> completion) {
        final List<SnapshotDownloadInfo> infos = msg.getSnapshotsDownloadInfo();
        final VolumeSnapshotInventory sinv = infos.get(0).getSnapshot();
        final PrimaryStorageInventory primaryStorage = getSelfInventory();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-template-from-snapshot-on-nfs-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            NfsPrimaryStorageBackend backend;
            String templateInstallPathOnPrimaryStorage;
            List<BackupStorageInventory> backupStorage;
            List<CreateTemplateFromVolumeSnapshotResult> results = new ArrayList<CreateTemplateFromVolumeSnapshotResult>();
            long templateSize;

            {
                backupStorage = msg.getBackupStorage();
                backend = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(sinv.getFormat(), self.getUuid()));
            }

            @Override
            public void setup() {
                flow(new Flow() {
                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        backend.createTemplateFromVolumeSnapshot(getSelfInventory(), infos,
                                msg.getImageUuid(),
                                msg.isNeedDownload(),
                                new ReturnValueCompletion<CreateBitsFromSnapshotResult>(trigger) {
                                    @Override
                                    public void success(CreateBitsFromSnapshotResult returnValue) {
                                        templateInstallPathOnPrimaryStorage = returnValue.getInstallPath();
                                        templateSize = returnValue.getSize();
                                        trigger.next();

                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.fail(errorCode);
                                    }
                                });
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        if (templateInstallPathOnPrimaryStorage != null) {
                            backend.delete(getSelfInventory(), templateInstallPathOnPrimaryStorage, new NopeCompletion());
                        }

                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    List<ErrorCode> errs;

                    private void upload(final Iterator<BackupStorageInventory> it, final FlowTrigger trigger) {
                        if (!it.hasNext()) {
                            if (results.isEmpty()) {
                                trigger.fail(errf.stringToOperationError(String.format("failed to upload image to all backup storage, a list of error: %s", JSONObjectUtil.toJsonString(errs))));
                            } else {
                                trigger.next();
                            }

                            return;
                        }


                        final BackupStorageInventory bs = it.next();
                        NfsPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(
                                BackupStorageType.valueOf(bs.getType()),
                                nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(sinv.getFormat(), self.getUuid())
                        );

                        final String templateInstallPathOnBackupStorage = mediator.makeRootVolumeTemplateInstallPath(bs.getUuid(), msg.getImageUuid());
                        mediator.uploadBits(primaryStorage, bs, templateInstallPathOnBackupStorage, templateInstallPathOnPrimaryStorage, new Completion(trigger) {
                            @Override
                            public void success() {
                                CreateTemplateFromVolumeSnapshotResult res = new CreateTemplateFromVolumeSnapshotResult();
                                res.setBackupStorageUuid(bs.getUuid());
                                res.setInstallPath(templateInstallPathOnBackupStorage);
                                results.add(res);
                                upload(it, trigger);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                errs.add(errorCode);
                                upload(it, trigger);
                            }
                        });
                    }

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        upload(backupStorage.iterator(), trigger);
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        if (!results.isEmpty()) {
                            List<DeleteBitsOnBackupStorageMsg> dmsgs = CollectionUtils.transformToList(results, new Function<DeleteBitsOnBackupStorageMsg, CreateTemplateFromVolumeSnapshotResult>() {
                                @Override
                                public DeleteBitsOnBackupStorageMsg call(CreateTemplateFromVolumeSnapshotResult arg) {
                                    DeleteBitsOnBackupStorageMsg dmsg = new DeleteBitsOnBackupStorageMsg();
                                    dmsg.setInstallPath(arg.getInstallPath());
                                    dmsg.setBackupStorageUuid(arg.getBackupStorageUuid());
                                    bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, arg.getBackupStorageUuid());
                                    return dmsg;
                                }
                            });

                            bus.send(dmsgs, new CloudBusListCallBack() {
                                @Override
                                public void run(List<MessageReply> replies) {
                                    for (MessageReply r : replies) {
                                        if (!r.isSuccess()) {
                                            CreateTemplateFromVolumeSnapshotResult res = results.get(replies.indexOf(r));
                                            logger.warn(String.format("failed to delete image[%s] from backup storage[uuid:%s]", res.getInstallPath(), res.getBackupStorageUuid()));
                                        }
                                    }
                                }
                            });
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        backend.delete(getSelfInventory(), templateInstallPathOnPrimaryStorage, new NopeCompletion());
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        CreateTemplateFromSnapshotResultStruct struct = new CreateTemplateFromSnapshotResultStruct();
                        struct.results = results;
                        struct.size = templateSize;
                        completion.success(struct);
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

    private void handle(final BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg) {
        BackupStorageInventory bs = msg.getBackupStorage();
        VolumeSnapshotInventory sinv = msg.getSnapshot();
        NfsPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(
                BackupStorageType.valueOf(bs.getType()),
                nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(sinv.getFormat(), self.getUuid())
        );

        final BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
        final String installPath = mediator.makeVolumeSnapshotInstallPath(bs.getUuid(), sinv.getUuid());
        mediator.uploadBits(getSelfInventory(), bs, installPath, sinv.getPrimaryStorageInstallPath(), new Completion(msg) {
            @Override
            public void success() {
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

        if (msg.getVolume().getVmInstanceUuid() != null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state);
            q.add(VmInstanceVO_.uuid, Op.EQ, msg.getVolume().getVmInstanceUuid());
            VmInstanceState state = q.findValue();
            if (state != VmInstanceState.Stopped) {
                reply.setError(errf.stringToOperationError(
                        String.format("unable to revert volume[uuid:%s] to snapshot[uuid:%s], the vm[uuid:%s] volume attached to is not in Stopped state, current state is %s",
                                msg.getVolume().getUuid(), msg.getSnapshot().getUuid(), msg.getVolume().getVmInstanceUuid(), state)
                ));

                bus.reply(msg, reply);
                return;
            }
        }

        HostInventory destHost = factory.getConnectedHostForOperation(PrimaryStorageInventory.valueOf(self));
        if (destHost == null) {
            reply.setError(errf.stringToOperationError(
                    String.format("no host in Connected status nfs primary storage[uuid:%s, name:%s] attached found to revert volume[uuid:%s] to snapshot[uuid:%s, name:%s]",
                            self.getUuid(), self.getName(), msg.getVolume().getUuid(), msg.getSnapshot().getUuid(), msg.getSnapshot().getName())
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

    private void handle(final DeleteSnapshotOnPrimaryStorageMsg msg) {
        final DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
        VolumeSnapshotInventory sinv = msg.getSnapshot();
        NfsPrimaryStorageBackend bkd = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(sinv.getFormat(), self.getUuid()));

        bkd.delete(getSelfInventory(), sinv.getPrimaryStorageInstallPath(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                //TODO: clean up
                reply.setError(errorCode);
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
                    TakeSnapshotOnHypervisorReply treply = (TakeSnapshotOnHypervisorReply)ret;
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

    private NfsPrimaryStorageBackend getBackendByClusterUuid(String clusterUuid) {
        SimpleQuery<ClusterVO> query = dbf.createQuery(ClusterVO.class);
        query.select(ClusterVO_.hypervisorType);
        query.add(ClusterVO_.uuid, Op.EQ, clusterUuid);
        String hvType = query.findValue();
        return getBackend(HypervisorType.valueOf(hvType));
    }

    private NfsPrimaryStorageBackend getBackend(HypervisorType hvType) {
        NfsPrimaryStorageBackend backend = factory.getHypervisorBackend(hvType);
        return backend;
    }

    @Override
    public void attachHook(String clusterUuid, Completion completion) {
        NfsPrimaryStorageBackend backend = getBackendByClusterUuid(clusterUuid);
        try {
            backend.attachToCluster(PrimaryStorageInventory.valueOf(self), clusterUuid);
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

    private void handle(final InstantiateRootVolumeFromTemplateMsg msg) throws PrimaryStorageException {
        final InstantiateVolumeReply reply = new InstantiateVolumeReply();
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

    private void createEmptyVolume(final InstantiateVolumeMsg msg) {
        NfsPrimaryStorageBackend backend = getBackend(HypervisorType.valueOf(msg.getDestHost().getHypervisorType()));
        VolumeInventory vol = msg.getVolume();
        final InstantiateVolumeReply reply = new InstantiateVolumeReply();
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
    protected void handle(InstantiateVolumeMsg msg) {
        try {
            if (msg.getClass() == InstantiateRootVolumeFromTemplateMsg.class) {
                handle((InstantiateRootVolumeFromTemplateMsg) msg);
            } else {
                createEmptyVolume(msg);
            }
        } catch (PrimaryStorageException e) {
            logger.warn(e.getMessage(), e);
            InstantiateVolumeReply reply = new InstantiateVolumeReply();
            reply.setError(errf.throwableToOperationError(e));
            bus.reply(msg, reply);
        }
    }

    @Override
    protected void handle(final DeleteVolumeOnPrimaryStorageMsg msg) {
        final DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
        final VolumeInventory vol = msg.getVolume();
        NfsPrimaryStorageBackend backend = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(vol.getFormat(), self.getUuid()));
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
                //TODO: cleanup
                logger.warn(errorCode.toString());
                reply.setError(errorCode);
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
                    public void rollback(FlowTrigger trigger, Map data) {
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
                        mediator.uploadBits(pinv, bsinv, templateBackupStorageInstallPath, templatePrimaryStorageInstallPath, new Completion(trigger) {
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
    protected void connectHook(ConnectPrimaryStorageMsg msg, Completion completion) {
        completion.success();
    }
}
