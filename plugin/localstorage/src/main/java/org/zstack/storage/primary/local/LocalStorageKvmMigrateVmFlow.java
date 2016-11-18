package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.ApiTimeout;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.MigrateVmOnHypervisorMsg;
import org.zstack.header.host.MigrateVmOnHypervisorMsg.StorageMigrationPolicy;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMHostVO;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 10/24/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalStorageKvmMigrateVmFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(LocalStorageKvmMigrateVmFlow.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    public static final String VERIFY_SNAPSHOT_CHAIN_PATH = "/localstorage/snapshot/verifychain";
    public static final String REBASE_SNAPSHOT_BACKING_FILES_PATH = "/localstorage/snapshot/rebasebackingfiles";
    public static final String REBASE_ROOT_VOLUME_TO_BACKING_FILE_PATH = "/localstorage/volume/rebaserootvolumetobackingfile";
    public static final String COPY_TO_REMOTE_BITS_PATH = "/localstorage/copytoremote";

    public static class SnapshotTO {
        public String path;
        public String parentPath;
        public String snapshotUuid;
    }

    public static class VerifySnapshotChainCmd extends LocalStorageKvmBackend.AgentCommand {
        public List<SnapshotTO> snapshots;
    }

    public static class RebaseSnapshotBackingFilesCmd extends LocalStorageKvmBackend.AgentCommand {
        public List<SnapshotTO> snapshots;
    }

    @ApiTimeout(apiClasses = {APILocalStorageMigrateVolumeMsg.class})
    public static class CopyBitsFromRemoteCmd extends LocalStorageKvmBackend.AgentCommand {
        public List<String> paths;
        public String dstIp;
        public String dstPassword;
        public String dstUsername;
    }

    class BackingImage {
        String uuid;
        String path;
        Long size;
        String md5;
    }

    @Override
    public void run(final FlowTrigger next, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final String srcHostUuid = spec.getVmInventory().getHostUuid();
        final String dstHostUuid = spec.getDestHost().getUuid();

        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, spec.getVmInventory().getRootVolumeUuid());
        final LocalStorageResourceRefVO ref = q.find();

        final List<VolumeInventory> volumesOnLocalStorage = getVolumeOnLocalStorage(spec);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("migrate-vm-%s-on-localstorage-%s", spec.getVmInventory().getUuid(), ref.getPrimaryStorageUuid()));
        chain.then(new ShareFlow() {
            long requiredSize = 0;
            StorageMigrationPolicy storageMigrationPolicy = StorageMigrationPolicy.FullCopy;
            BackingImage backingImage = new BackingImage();
            List<VolumeSnapshotTree> snapshotTrees = new ArrayList<VolumeSnapshotTree>();
            List<VolumeSnapshotInventory> allSnapshots;

            boolean downloadImage;
            ImageVO image;
            VolumeInventory rootVolume;
            KVMHostVO dstHost;

            {
                for (VolumeInventory vol : volumesOnLocalStorage) {
                    requiredSize += vol.getSize();

                    SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
                    q.add(VolumeSnapshotVO_.volumeUuid, Op.EQ, vol.getUuid());
                    List<VolumeSnapshotVO> vos = q.list();
                    if (!vos.isEmpty()) {
                        Map<String, List<VolumeSnapshotVO>> m = new HashMap<String, List<VolumeSnapshotVO>>();

                        for (VolumeSnapshotVO vo : vos) {
                            requiredSize += vo.getSize();

                            List<VolumeSnapshotVO> lst = m.get(vo.getTreeUuid());
                            if (lst == null) {
                                lst = new ArrayList<VolumeSnapshotVO>();
                                m.put(vo.getTreeUuid(), lst);
                            }

                            lst.add(vo);
                        }

                        for (List<VolumeSnapshotVO> l : m.values()) {
                            VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(l);
                            snapshotTrees.add(tree);
                        }

                        allSnapshots =  VolumeSnapshotInventory.valueOf(vos);
                    }
                }

                rootVolume = spec.getVmInventory().getRootVolume();
                String imageUuid = rootVolume.getRootImageUuid();
                if (imageUuid == null) {
                    downloadImage = false;
                } else {
                    image = dbf.findByUuid(imageUuid, ImageVO.class);
                    downloadImage = !(image == null || image.getMediaType() == ImageMediaType.ISO
                            || image.getStatus() == ImageStatus.Deleted);
                }

                dstHost = dbf.findByUuid(dstHostUuid, KVMHostVO.class);
            }

            @Override
            public void setup() {
                if (downloadImage) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "download-image-to-cache";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            DownloadImageToPrimaryStorageCacheMsg msg = new DownloadImageToPrimaryStorageCacheMsg();
                            msg.setImage(ImageInventory.valueOf(image));
                            msg.setHostUuid(dstHostUuid);
                            msg.setPrimaryStorageUuid(ref.getPrimaryStorageUuid());
                            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ref.getPrimaryStorageUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                        return;
                                    }

                                    DownloadImageToPrimaryStorageCacheReply r = reply.castReply();
                                    storageMigrationPolicy = StorageMigrationPolicy.IncCopy;
                                    backingImage.uuid = image.getUuid();
                                    backingImage.path = r.getInstallPath();
                                    trigger.next();
                                }
                            });
                        }
                    });
                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = "get-backing-file-of-root-volume";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            GetBackingFileCmd cmd = new GetBackingFileCmd();
                            cmd.path = rootVolume.getInstallPath();
                            cmd.volumeUuid = rootVolume.getUuid();
                            callKvmHost(srcHostUuid, ref.getPrimaryStorageUuid(), LocalStorageKvmBackend.GET_BACKING_FILE_PATH, cmd, GetBackingFileRsp.class, new ReturnValueCompletion<GetBackingFileRsp>(trigger) {
                                @Override
                                public void success(GetBackingFileRsp rsp) {
                                    backingImage.path = rsp.backingFilePath;
                                    backingImage.size = rsp.size;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    flow(new Flow() {
                        String __name__ = "reserve-capacity-for-backing-file-on-dst-host";

                        boolean s = false;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            if (backingImage.path == null) {
                                logger.debug("no backing file, skip this flow");
                                trigger.next();
                                return;
                            }

                            reserveStorageCapacityOnHost(dstHostUuid, ref.getPrimaryStorageUuid(), backingImage.size, new Completion(trigger) {
                                @Override
                                public void success() {
                                    s = true;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void rollback(final FlowRollback trigger, Map data) {
                            if (s) {
                                returnStorageCapacityToHost(dstHostUuid, ref.getPrimaryStorageUuid(), backingImage.size);
                            }

                            trigger.rollback();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "get-md5-of-backing-file";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            if (backingImage.path == null) {
                                logger.debug("no backing file, skip this flow");
                                trigger.next();
                                return;
                            }

                            GetMd5Cmd cmd = new GetMd5Cmd();
                            GetMd5TO to = new GetMd5TO();
                            to.resourceUuid = "backing-file";
                            to.path = backingImage.path;
                            cmd.md5s = list(to);

                            callKvmHost(srcHostUuid, ref.getPrimaryStorageUuid(), LocalStorageKvmBackend.GET_MD5_PATH, cmd, GetMd5Rsp.class, new ReturnValueCompletion<GetMd5Rsp>(trigger) {
                                @Override
                                public void success(GetMd5Rsp rsp) {
                                    backingImage.md5 = rsp.md5s.get(0).md5;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    flow(new Flow() {
                        String __name__ = "migrate-backing-file";

                        boolean s = false;

                        private void migrate(final FlowTrigger trigger) {
                            // sync here for migrating multiple volumes having the same backing file
                            thdf.chainSubmit(new ChainTask(trigger) {
                                @Override
                                public String getSyncSignature() {
                                    return String.format("migrate-backing-file-%s-to-host-%s", backingImage.path, dstHostUuid);
                                }

                                @Override
                                public void run(final SyncTaskChain chain) {
                                    final CopyBitsFromRemoteCmd cmd = new CopyBitsFromRemoteCmd();
                                    cmd.dstIp = dstHost.getManagementIp();
                                    cmd.dstUsername = dstHost.getUsername();
                                    cmd.dstPassword = dstHost.getPassword();
                                    cmd.paths = list(backingImage.path);

                                    callKvmHost(srcHostUuid, ref.getPrimaryStorageUuid(), COPY_TO_REMOTE_BITS_PATH, cmd, AgentResponse.class,
                                            new ReturnValueCompletion<AgentResponse>(trigger, chain) {
                                        @Override
                                        public void success(AgentResponse rsp) {
                                            s = true;
                                            trigger.next();
                                            chain.next();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            trigger.fail(errorCode);
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

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            if (backingImage.path == null) {
                                logger.debug("no backing file, skip this flow");
                                trigger.next();
                                return;
                            }

                            checkIfExistOnDst(new ReturnValueCompletion<Boolean>(trigger) {
                                @Override
                                public void success(Boolean existing) {
                                    if (existing) {
                                        // DO NOT set success = true here, otherwise the rollback
                                        // will delete the backing file which belongs to others on the dst host
                                        logger.debug(String.format("found %s on the dst host[uuid:%s], don't copy it",
                                                backingImage.path, dstHostUuid));
                                        trigger.next();
                                    } else {
                                        migrate(trigger);
                                    }

                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        private void checkIfExistOnDst(final ReturnValueCompletion<Boolean> completion) {
                            CheckBitsCmd cmd = new CheckBitsCmd();
                            cmd.path = backingImage.path;

                            callKvmHost(dstHostUuid, ref.getPrimaryStorageUuid(), LocalStorageKvmBackend.CHECK_BITS_PATH,
                                    cmd, CheckBitsRsp.class, new ReturnValueCompletion<CheckBitsRsp>(completion) {
                                @Override
                                public void success(CheckBitsRsp rsp) {
                                    completion.success(rsp.existing);
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    completion.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            if (s) {
                                LocalStorageDirectlyDeleteBitsMsg msg = new LocalStorageDirectlyDeleteBitsMsg();
                                msg.setPath(backingImage.path);
                                msg.setHostUuid(dstHostUuid);
                                msg.setPrimaryStorageUuid(ref.getPrimaryStorageUuid());
                                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ref.getPrimaryStorageUuid());
                                bus.send(msg, new CloudBusCallBack() {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            //TODO
                                            logger.warn(String.format("failed to delete %s on the host[uuid:%s] of local storage[uuid:%s], %s",
                                                    backingImage.path, dstHostUuid, ref.getPrimaryStorageUuid(), reply.getError()));
                                        }
                                    }
                                });
                            }

                            trigger.rollback();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "check-md5-of-backing-file-on-dst-host";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            if (backingImage.path == null) {
                                logger.debug("no backing file, skip this flow");
                                trigger.next();
                                return;
                            }

                            Md5TO to = new Md5TO();
                            to.resourceUuid = "backing-file";
                            to.path = backingImage.path;
                            to.md5 = backingImage.md5;

                            CheckMd5sumCmd cmd = new CheckMd5sumCmd();
                            cmd.md5s = list(to);

                            callKvmHost(dstHostUuid, ref.getPrimaryStorageUuid(), LocalStorageKvmBackend.CHECK_MD5_PATH,
                                    cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(trigger) {
                                @Override
                                public void success(AgentResponse returnValue) {
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

                flow(new Flow() {
                    String __name__ = "reserve-capacity-on-host";

                    boolean s = false;

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        reserveStorageCapacityOnHost(dstHostUuid, ref.getPrimaryStorageUuid(), requiredSize, new Completion(trigger) {
                            @Override
                            public void success() {
                                s = true;
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
                        if (s) {
                            returnStorageCapacityToHost(dstHostUuid, ref.getPrimaryStorageUuid(), requiredSize);
                        }

                        trigger.rollback();
                    }
                });

                if (!snapshotTrees.isEmpty()) {
                    List<Flow> flows = createFlowsForSnapshot(volumesOnLocalStorage, snapshotTrees, srcHostUuid, dstHostUuid, backingImage);
                    for (Flow f : flows) {
                        flow(f);
                    }
                } else {
                    flow(new Flow() {
                        String __name__ = "create-volumes-on-dst-host";

                        List<VolumeInventory> successVolumes = new ArrayList<VolumeInventory>();

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            List<LocalStorageCreateEmptyVolumeMsg> msgs = CollectionUtils.transformToList(volumesOnLocalStorage, new Function<LocalStorageCreateEmptyVolumeMsg, VolumeInventory>() {
                                @Override
                                public LocalStorageCreateEmptyVolumeMsg call(VolumeInventory arg) {
                                    LocalStorageCreateEmptyVolumeMsg msg = new LocalStorageCreateEmptyVolumeMsg();
                                    msg.setHostUuid(dstHostUuid);
                                    msg.setVolume(arg);

                                    if (VolumeType.Root.toString().equals(arg.getType())) {
                                        msg.setBackingFile(backingImage.path);
                                    }

                                    msg.setPrimaryStorageUuid(ref.getPrimaryStorageUuid());
                                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ref.getPrimaryStorageUuid());
                                    return msg;
                                }
                            });

                            bus.send(msgs, 1, new CloudBusListCallBack(trigger) {
                                @Override
                                public void run(List<MessageReply> replies) {
                                    for (MessageReply r : replies) {
                                        if (!r.isSuccess()) {
                                            trigger.fail(r.getError());
                                            return;
                                        }

                                        successVolumes.add(volumesOnLocalStorage.get(replies.indexOf(r)));
                                    }

                                    trigger.next();
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            if (!successVolumes.isEmpty()) {
                                List<LocalStorageDirectlyDeleteBitsMsg> msgs = CollectionUtils.transformToList(successVolumes, new Function<LocalStorageDirectlyDeleteBitsMsg, VolumeInventory>() {
                                    @Override
                                    public LocalStorageDirectlyDeleteBitsMsg call(VolumeInventory arg) {
                                        LocalStorageDirectlyDeleteBitsMsg msg = new LocalStorageDirectlyDeleteBitsMsg();
                                        msg.setHostUuid(dstHostUuid);
                                        msg.setPrimaryStorageUuid(ref.getPrimaryStorageUuid());
                                        msg.setPath(arg.getInstallPath());
                                        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ref.getPrimaryStorageUuid());
                                        return msg;
                                    }
                                });

                                bus.send(msgs);
                            }

                            trigger.rollback();
                        }
                    });
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "migrate-vm";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        MigrateVmOnHypervisorMsg msg = new MigrateVmOnHypervisorMsg();
                        msg.setVmInventory(spec.getVmInventory());
                        msg.setDestHostInventory(spec.getDestHost());
                        msg.setSrcHostUuid(spec.getVmInventory().getHostUuid());
                        msg.setStorageMigrationPolicy(storageMigrationPolicy);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
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
                    String __name__ = "update-volumes-info-in-db-to-dst-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> volUuids = CollectionUtils.transformToList(volumesOnLocalStorage, new Function<String, VolumeInventory>() {
                            @Override
                            public String call(VolumeInventory arg) {
                                return arg.getUuid();
                            }
                        });

                        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
                        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.IN, volUuids);
                        q.add(LocalStorageResourceRefVO_.resourceType, Op.EQ, VolumeVO.class.getSimpleName());
                        List<LocalStorageResourceRefVO> refs = q.list();
                        for (LocalStorageResourceRefVO ref : refs) {
                            ref.setHostUuid(dstHostUuid);
                        }

                        dbf.updateCollection(refs);
                        trigger.next();
                    }
                });

                if (!snapshotTrees.isEmpty()) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "update-snapshots-info-in-db-to-dst-host";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            List<String> spUuids = CollectionUtils.transformToList(allSnapshots, new Function<String, VolumeSnapshotInventory>() {
                                @Override
                                public String call(VolumeSnapshotInventory arg) {
                                    return arg.getUuid();
                                }
                            });

                            SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
                            q.add(LocalStorageResourceRefVO_.resourceUuid, Op.IN, spUuids);
                            q.add(LocalStorageResourceRefVO_.resourceType, Op.EQ, VolumeSnapshotVO.class.getSimpleName());
                            List<LocalStorageResourceRefVO> refs = q.list();
                            for (LocalStorageResourceRefVO ref : refs) {
                                ref.setHostUuid(dstHostUuid);
                            }

                            dbf.updateCollection(refs);
                            trigger.next();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-snapshots-on-src-host";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            List<LocalStorageDirectlyDeleteBitsMsg> msgs = CollectionUtils.transformToList(allSnapshots, new Function<LocalStorageDirectlyDeleteBitsMsg, VolumeSnapshotInventory>() {
                                @Override
                                public LocalStorageDirectlyDeleteBitsMsg call(VolumeSnapshotInventory sp) {
                                    LocalStorageDirectlyDeleteBitsMsg msg = new LocalStorageDirectlyDeleteBitsMsg();
                                    msg.setHostUuid(srcHostUuid);
                                    msg.setPath(sp.getPrimaryStorageInstallPath());
                                    msg.setPrimaryStorageUuid(ref.getPrimaryStorageUuid());
                                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ref.getPrimaryStorageUuid());
                                    return msg;
                                }
                            });

                            bus.send(msgs, new CloudBusListCallBack() {
                                @Override
                                public void run(List<MessageReply> replies) {
                                    for (MessageReply r : replies) {
                                        VolumeSnapshotInventory sp = allSnapshots.get(replies.indexOf(r));
                                        if (!r.isSuccess()) {
                                            //TODO
                                            logger.warn(String.format("failed to delete the snapshot[%s] on the local primary storage[uuid:%s], %s",
                                                    sp.getPrimaryStorageInstallPath(), ref.getPrimaryStorageUuid(), r.getError()));
                                        }
                                    }
                                }
                            });

                            trigger.next();
                        }
                    });
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-volumes-on-src-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<LocalStorageDirectlyDeleteBitsMsg> msgs = CollectionUtils.transformToList(volumesOnLocalStorage, new Function<LocalStorageDirectlyDeleteBitsMsg, VolumeInventory>() {
                            @Override
                            public LocalStorageDirectlyDeleteBitsMsg call(VolumeInventory arg) {
                                LocalStorageDirectlyDeleteBitsMsg msg = new LocalStorageDirectlyDeleteBitsMsg();
                                msg.setHostUuid(srcHostUuid);
                                msg.setPath(arg.getInstallPath());
                                msg.setPrimaryStorageUuid(ref.getPrimaryStorageUuid());
                                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ref.getPrimaryStorageUuid());
                                return msg;
                            }
                        });

                        bus.send(msgs, new CloudBusListCallBack() {
                            @Override
                            public void run(List<MessageReply> replies) {
                                for (MessageReply r : replies) {
                                    if (!r.isSuccess()) {
                                        //TODO:
                                        VolumeInventory vol = volumesOnLocalStorage.get(replies.indexOf(r));
                                        logger.warn(String.format("failed to delete the volume[%s] in the host[uuid:%s] for the local" +
                                                        " primary storage[uuid:%s] during after the vm[uuid:%s] migrated to the host[uuid:%s, ip:%s], %s",
                                                vol.getUuid(), srcHostUuid, ref.getPrimaryStorageUuid(), spec.getVmInventory().getUuid(), dstHostUuid,
                                                spec.getDestHost().getManagementIp(), r.getError()));
                                    }
                                }
                            }
                        });

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "return-capacity-to-src-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        LocalStorageReturnHostCapacityMsg msg = new LocalStorageReturnHostCapacityMsg();
                        msg.setHostUuid(srcHostUuid);
                        msg.setPrimaryStorageUuid(ref.getPrimaryStorageUuid());
                        msg.setSize(requiredSize);
                        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ref.getPrimaryStorageUuid());
                        bus.send(msg, new CloudBusCallBack() {
                            @Override
                            public void run(MessageReply reply) {
                                //TODO
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to return capacity[%s] to the host[uuid:%s] of the local" +
                                            " primary storage[uuid:%s], %s", requiredSize, srcHostUuid, ref.getPrimaryStorageUuid(), reply.getError()));
                                }
                            }
                        });

                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(next) {
                    @Override
                    public void handle(Map data) {
                        next.next();
                    }
                });

                error(new FlowErrorHandler(next) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        next.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void returnStorageCapacityToHost(final String dstHostUuid, final String primaryStorageUuid, final Long size) {
        LocalStorageReturnHostCapacityMsg msg = new LocalStorageReturnHostCapacityMsg();
        msg.setHostUuid(dstHostUuid);
        msg.setSize(size);
        msg.setPrimaryStorageUuid(primaryStorageUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(msg, new CloudBusCallBack() {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    //TODO
                    logger.warn(String.format("failed to return capacity[%s] to the host[uuid:%s] of local storage[uuid:%s], %s",
                            size, dstHostUuid, primaryStorageUuid, reply.getError()));
                }
            }
        });
    }

    private void reserveStorageCapacityOnHost(String dstHostUuid, String psUuid, Long size, final Completion completion) {
        LocalStorageReserveHostCapacityMsg msg = new LocalStorageReserveHostCapacityMsg();
        msg.setHostUuid(dstHostUuid);
        msg.setSize(size);
        msg.setPrimaryStorageUuid(psUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, psUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success();
                }
            }
        });
    }

    private <T extends AgentResponse> void callKvmHost(final String hostUuid, final String psUuid, String path, AgentCommand cmd, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setPath(path);
        msg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                T rsp = r.toResponse(rspType);
                if (!rsp.isSuccess()) {
                    completion.fail(errf.stringToOperationError(rsp.getError()));
                    return;
                }

                if (rsp.getTotalCapacity() != null && rsp.getAvailableCapacity() != null) {
                    new LocalStorageCapacityUpdater().updatePhysicalCapacityByKvmAgentResponse(psUuid, hostUuid, rsp);
                }

                completion.success(rsp);
            }
        });
    }

    private List<Flow> createFlowsForSnapshot(List<VolumeInventory> volumesOnLocalStorage,
                                              List<VolumeSnapshotTree> snapshotTrees, final String srcHostUuid,
                                              final String dstHostUuid, final BackingImage image) {
        List<Flow> flows = new ArrayList<Flow>();

        class VSPair {
            VolumeInventory volume;
            List<VolumeSnapshotTree> snapshotTrees;
            VolumeSnapshotInventory latest;
        }

        List<VSPair> volumeHasSnapshots = new ArrayList<VSPair>();
        List<VolumeInventory> volumeNoSnapshots = new ArrayList<VolumeInventory>();

        for (final VolumeInventory vol : volumesOnLocalStorage) {
            final List<VolumeSnapshotTree> trees = CollectionUtils.transformToList(snapshotTrees, new Function<VolumeSnapshotTree, VolumeSnapshotTree>() {
                @Override
                public VolumeSnapshotTree call(VolumeSnapshotTree arg) {
                    return arg.getVolumeUuid().equals(vol.getUuid()) ? arg : null;
                }
            });

            if (!trees.isEmpty()) {
                VSPair p = new VSPair();
                p.volume = vol;
                p.snapshotTrees = trees;
                p.latest = new Callable<VolumeSnapshotInventory>() {
                    @Override
                    @Transactional(readOnly = true)
                    public VolumeSnapshotInventory call() {
                        String sql = "select sp from VolumeSnapshotVO sp, VolumeSnapshotTreeVO tree where sp.treeUuid = tree.uuid" +
                                " and tree.current = :c and sp.latest = :l and tree.volumeUuid = :volUuid";
                        TypedQuery<VolumeSnapshotVO> q = dbf.getEntityManager().createQuery(sql, VolumeSnapshotVO.class);
                        q.setParameter("c", true);
                        q.setParameter("l", true);
                        q.setParameter("volUuid", vol.getUuid());
                        VolumeSnapshotVO vo = q.getSingleResult();
                        return VolumeSnapshotInventory.valueOf(vo);
                    }
                }.call();

                volumeHasSnapshots.add(p);
            } else {
                volumeNoSnapshots.add(vol);
            }
        }

        for (final VSPair p : volumeHasSnapshots) {
            final List<VolumeSnapshotInventory> children = new ArrayList<VolumeSnapshotInventory>();
            for (VolumeSnapshotTree t : p.snapshotTrees) {
                children.addAll(t.getRoot().getDescendants());
            }

            final List<SnapshotTO> snapshotTOs = CollectionUtils.transformToList(children, new Function<SnapshotTO, VolumeSnapshotInventory>() {
                @Override
                public SnapshotTO call(final VolumeSnapshotInventory s) {
                    SnapshotTO to = new SnapshotTO();
                    to.path = s.getPrimaryStorageInstallPath();
                    to.snapshotUuid = s.getUuid();
                    if (s.getParentUuid() != null) {
                        to.parentPath = CollectionUtils.find(children, new Function<String, VolumeSnapshotInventory>() {
                            @Override
                            public String call(VolumeSnapshotInventory arg) {
                                return arg.getUuid().equals(s.getParentUuid()) ? arg.getPrimaryStorageInstallPath() : null;
                            }
                        });
                    }
                    return to;
                }
            });


            class Context {
                List<Md5TO> md5s;
            }

            final Context context = new Context();

            flows.add(new NoRollbackFlow() {
                String __name__ = String.format("verify-snapshot-integrity-of-volume-%s-on-src-host", p.volume.getUuid());

                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    VerifySnapshotChainCmd cmd = new VerifySnapshotChainCmd();
                    cmd.snapshots = snapshotTOs;
                    callKvmHost(srcHostUuid, p.volume.getPrimaryStorageUuid(), VERIFY_SNAPSHOT_CHAIN_PATH, cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(trigger) {
                        @Override
                        public void success(AgentResponse returnValue) {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });

            flows.add(new NoRollbackFlow() {
                String __name__ = "get-snapshot-md5";

                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    GetMd5Cmd cmd = new GetMd5Cmd();
                    cmd.md5s = CollectionUtils.transformToList(children, new Function<GetMd5TO, VolumeSnapshotInventory>() {
                        @Override
                        public GetMd5TO call(VolumeSnapshotInventory arg) {
                            GetMd5TO to = new GetMd5TO();
                            to.path = arg.getPrimaryStorageInstallPath();
                            to.resourceUuid = arg.getUuid();
                            return to;
                        }
                    });

                    callKvmHost(srcHostUuid, p.volume.getPrimaryStorageUuid(), LocalStorageKvmBackend.GET_MD5_PATH, cmd,
                            GetMd5Rsp.class, new ReturnValueCompletion<GetMd5Rsp>(trigger) {
                        @Override
                        public void success(GetMd5Rsp rsp) {
                            context.md5s = rsp.md5s;
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });

            flows.add(new Flow() {
                String __name__ = String.format("copy-snapshots-for-volume-%s-on-dst-host", p.volume.getUuid());

                List<VolumeSnapshotInventory> success = new ArrayList<VolumeSnapshotInventory>();
                KVMHostVO dstHost = dbf.findByUuid(dstHostUuid, KVMHostVO.class);

                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    CopyBitsFromRemoteCmd cmd = new CopyBitsFromRemoteCmd();
                    cmd.paths = CollectionUtils.transformToList(children, new Function<String, VolumeSnapshotInventory>() {
                        @Override
                        public String call(VolumeSnapshotInventory arg) {
                            return arg.getPrimaryStorageInstallPath();
                        }
                    });
                    cmd.dstIp = dstHost.getManagementIp();
                    cmd.dstPassword = dstHost.getPassword();
                    cmd.dstUsername = dstHost.getUsername();
                    callKvmHost(srcHostUuid, p.volume.getPrimaryStorageUuid(), COPY_TO_REMOTE_BITS_PATH, cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(trigger) {
                        @Override
                        public void success(AgentResponse returnValue) {
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
                    if (!success.isEmpty()) {
                        final List<LocalStorageDirectlyDeleteBitsMsg> msgs = CollectionUtils.transformToList(success, new Function<LocalStorageDirectlyDeleteBitsMsg, VolumeSnapshotInventory>() {
                            @Override
                            public LocalStorageDirectlyDeleteBitsMsg call(VolumeSnapshotInventory arg) {
                                LocalStorageDirectlyDeleteBitsMsg msg = new LocalStorageDirectlyDeleteBitsMsg();
                                msg.setHostUuid(dstHostUuid);
                                msg.setPrimaryStorageUuid(arg.getPrimaryStorageUuid());
                                msg.setPath(arg.getPrimaryStorageInstallPath());
                                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, arg.getPrimaryStorageUuid());
                                return msg;
                            }
                        });

                        bus.send(msgs, new CloudBusListCallBack() {
                            @Override
                            public void run(List<MessageReply> replies) {
                                for (MessageReply r : replies) {
                                    if (!r.isSuccess()) {
                                        LocalStorageDirectlyDeleteBitsMsg msg = msgs.get(replies.indexOf(r));
                                        //TODO
                                        logger.warn(String.format("failed to delete %s on the local primary storage[uuid:%s], host[uuid:%s], %s",
                                                msg.getPath(), msg.getPrimaryStorageUuid(), msg.getHostUuid(), r.getError()));
                                    }
                                }
                            }
                        });
                    }

                    trigger.rollback();
                }
            });

            flows.add(new NoRollbackFlow() {
                String __name__ = "check-snapshots-md5-on-dst-host";

                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    CheckMd5sumCmd cmd = new CheckMd5sumCmd();
                    cmd.md5s = context.md5s;

                    callKvmHost(dstHostUuid, p.volume.getPrimaryStorageUuid(), LocalStorageKvmBackend.CHECK_MD5_PATH,
                            cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(trigger) {
                                @Override
                                public void success(AgentResponse returnValue) {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                }
            });

            flows.add(new Flow() {
                String __name__ = "create-volume-on-dst-host";

                boolean s = false;

                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    final CreateEmptyVolumeCmd cmd = new CreateEmptyVolumeCmd();
                    cmd.setInstallUrl(p.volume.getInstallPath());
                    cmd.setSize(p.volume.getSize());
                    cmd.setVolumeUuid(p.volume.getUuid());
                    cmd.setBackingFile(p.latest.getPrimaryStorageInstallPath());

                    callKvmHost(dstHostUuid, p.volume.getPrimaryStorageUuid(), LocalStorageKvmBackend.CREATE_EMPTY_VOLUME_PATH, cmd, CreateEmptyVolumeRsp.class,
                            new ReturnValueCompletion<CreateEmptyVolumeRsp>(trigger) {
                                @Override
                                public void success(CreateEmptyVolumeRsp returnValue) {
                                    s = true;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                                            String.format("unable to create an empty volume[uuid:%s, name:%s] on the kvm host[uuid:%s]",
                                                    p.volume.getUuid(), p.volume.getName(), dstHostUuid), errorCode));
                                }
                            });
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    if (s) {
                        LocalStorageDirectlyDeleteBitsMsg msg = new LocalStorageDirectlyDeleteBitsMsg();
                        msg.setHostUuid(dstHostUuid);
                        msg.setPrimaryStorageUuid(p.volume.getPrimaryStorageUuid());
                        msg.setPath(p.volume.getInstallPath());
                        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, p.volume.getPrimaryStorageUuid());

                        bus.send(msg, new CloudBusCallBack() {
                            @Override
                            public void run(MessageReply reply) {
                                //TODO
                                logger.warn(String.format("failed to delete %s on the local primary storage[uuid:%s], host[uuid:%s], %s",
                                        p.volume.getInstallPath(), p.volume.getPrimaryStorageUuid(), dstHostUuid, reply.getError()));
                            }
                        });
                    }

                    trigger.rollback();
                }
            });

            flows.add(new NoRollbackFlow() {
                String __name__ = String.format("rebase-backing-files-of-snapshots-of-volume-%s-on-dst-host", p.volume.getUuid());

                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    RebaseSnapshotBackingFilesCmd cmd = new RebaseSnapshotBackingFilesCmd();
                    cmd.snapshots = snapshotTOs;
                    callKvmHost(dstHostUuid, p.volume.getPrimaryStorageUuid(), REBASE_SNAPSHOT_BACKING_FILES_PATH, cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(trigger) {
                        @Override
                        public void success(AgentResponse returnValue) {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });

            flows.add(new NoRollbackFlow() {
                String __name__ = String.format("verify-backingfile-integrity-of-volume-%s-on-dst-host", p.volume.getUuid());

                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    List<SnapshotTO> s = new ArrayList<SnapshotTO>();
                    s.addAll(snapshotTOs);

                    // the volume links to the latest snapshot
                    SnapshotTO to = new SnapshotTO();
                    to.parentPath = p.latest.getPrimaryStorageInstallPath();
                    to.path = p.volume.getInstallPath();
                    to.snapshotUuid = p.volume.getUuid();
                    s.add(to);

                    VerifySnapshotChainCmd cmd = new VerifySnapshotChainCmd();
                    cmd.snapshots = s;
                    callKvmHost(dstHostUuid, p.volume.getPrimaryStorageUuid(), VERIFY_SNAPSHOT_CHAIN_PATH, cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(trigger) {
                        @Override
                        public void success(AgentResponse returnValue) {
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

        for (final VolumeInventory vol : volumeNoSnapshots) {
            flows.add(new Flow() {
                String __name__ = String.format("create-no-snapshot-volume-%s-on-dst-host", vol.getUuid());

                boolean s = false;

                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    LocalStorageCreateEmptyVolumeMsg msg = new LocalStorageCreateEmptyVolumeMsg();
                    msg.setHostUuid(dstHostUuid);
                    msg.setVolume(vol);

                    if (VolumeType.Root.toString().equals(vol.getType())) {
                        msg.setBackingFile(image.path);
                    }

                    msg.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, vol.getPrimaryStorageUuid());
                    bus.send(msg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                trigger.fail(reply.getError());
                            } else {
                                s = true;
                                trigger.next();
                            }
                        }
                    });
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    if (s) {
                        LocalStorageDirectlyDeleteBitsMsg msg = new LocalStorageDirectlyDeleteBitsMsg();
                        msg.setHostUuid(dstHostUuid);
                        msg.setPath(vol.getInstallPath());
                        msg.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, vol.getPrimaryStorageUuid());
                        bus.send(msg, new CloudBusCallBack() {
                            @Override
                            public void run(MessageReply r) {
                                if (!r.isSuccess()) {
                                    //TODO
                                    logger.warn(String.format("failed to delete %s on the local primary storage[uuid:%s], host[uuid:%s], %s",
                                            vol.getInstallPath(), vol.getPrimaryStorageUuid(), dstHostUuid, r.getError()));
                                }
                            }
                        });
                    }

                    trigger.rollback();
                }
            });
        }

        return flows;
    }

    @Transactional(readOnly = true)
    private List<VolumeInventory> getVolumeOnLocalStorage(VmInstanceSpec spec) {
        String sql = "select v from VolumeVO v, PrimaryStorageVO ps where v.primaryStorageUuid = ps.uuid" +
                " and ps.type = :type and v.vmInstanceUuid = :vmUuid";
        TypedQuery<VolumeVO> q = dbf.getEntityManager().createQuery(sql, VolumeVO.class);
        q.setParameter("type", LocalStorageConstants.LOCAL_STORAGE_TYPE);
        q.setParameter("vmUuid", spec.getVmInventory().getUuid());
        return VolumeInventory.valueOf(q.getResultList());
    }
}
