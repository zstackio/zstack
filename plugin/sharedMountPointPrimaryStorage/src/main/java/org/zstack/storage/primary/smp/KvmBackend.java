package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.ImageBackupStorageSelector;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.*;
import org.zstack.header.core.validation.Validation;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotMsg;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.*;
import org.zstack.kvm.*;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialReply;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.primary.PrimaryStoragePathMaker;
import org.zstack.storage.primary.PrimaryStoragePhysicalCapacityManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.io.File;
import java.util.*;

/**
 * Created by xing5 on 2016/3/26.
 */
public class KvmBackend extends HypervisorBackend {
    private static final CLogger logger = Utils.getLogger(KvmBackend.class);

    @Autowired
    protected ApiTimeoutManager timeoutManager;
    @Autowired
    protected PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    protected PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;
    @Autowired
    protected PluginRegistry pluginRgty;

    public static class AgentCmd {
    }

    public static class AgentRsp {
        public boolean success = true;
        public String error;
        public Long totalCapacity;
        public Long availableCapacity;
    }

    public static class ConnectCmd extends AgentCmd {
        public String uuid;
        public String mountPoint;
    }


    @ApiTimeout(apiClasses = {APICreateVmInstanceMsg.class})
    public static class CreateVolumeFromCacheCmd extends AgentCmd {
        public String templatePathInCache;
        public String installPath;
        public String volumeUuid;
    }

    public static class DeleteBitsCmd extends AgentCmd {
        public String path;
    }

    @ApiTimeout(apiClasses = {
            APICreateRootVolumeTemplateFromRootVolumeMsg.class,
            APICreateDataVolumeTemplateFromVolumeMsg.class
    })
    public static class CreateTemplateFromVolumeCmd extends AgentCmd {
        public String installPath;
        public String volumePath;
    }

    public static class SftpUploadBitsCmd extends AgentCmd {
        public String primaryStorageInstallPath;
        public String backupStorageInstallPath;
        public String hostname;
        public String username;
        public String sshKey;
        public int sshPort;
    }

    @ApiTimeout(apiClasses = {APIAddImageMsg.class})
    public static class SftpDownloadBitsCmd extends AgentCmd {
        public String sshKey;
        public int sshPort;
        public String hostname;
        public String username;
        public String backupStorageInstallPath;
        public String primaryStorageInstallPath;
    }

    public static class RevertVolumeFromSnapshotCmd extends AgentCmd {
        public String snapshotInstallPath;
    }

    public static class RevertVolumeFromSnapshotRsp extends AgentRsp {
        @Validation
        public String newVolumeInstallPath;
    }

    @ApiTimeout(apiClasses = {APICreateDataVolumeFromVolumeSnapshotMsg.class})
    public static class MergeSnapshotCmd extends AgentCmd {
        public String volumeUuid;
        public String snapshotInstallPath;
        public String workspaceInstallPath;
    }

    public static class MergeSnapshotRsp extends AgentRsp {
        public long actualSize;
        public long size;
    }

    @ApiTimeout(apiClasses = {APICreateDataVolumeFromVolumeSnapshotMsg.class, APIDeleteVolumeSnapshotMsg.class})
    public static class OfflineMergeSnapshotCmd extends AgentCmd {
        public String srcPath;
        public String destPath;
        public boolean fullRebase;
    }

    public static class CreateEmptyVolumeCmd extends AgentCmd {
        public String installPath;
        public long size;
        public String name;
        public String volumeUuid;
    }

    public static class CheckBitsCmd extends AgentCmd {
        public String path;
    }

    public static class CheckBitsRsp extends AgentRsp {
        public boolean existing;
    }

    public static class GetVolumeSizeCmd extends AgentCmd {
        public String volumeUuid;
        public String installPath;
    }

    public static class GetVolumeSizeRsp extends AgentRsp {
        public Long actualSize;
        public Long size;
    }

    public static final String CONNECT_PATH = "/sharedmountpointprimarystorage/connect";
    public static final String CREATE_VOLUME_FROM_CACHE_PATH = "/sharedmountpointprimarystorage/createrootvolume";
    public static final String DELETE_BITS_PATH = "/sharedmountpointprimarystorage/bits/delete";
    public static final String CREATE_TEMPLATE_FROM_VOLUME_PATH = "/sharedmountpointprimarystorage/createtemplatefromvolume";
    public static final String UPLOAD_BITS_TO_SFTP_BACKUPSTORAGE_PATH = "/sharedmountpointprimarystorage/sftp/upload";
    public static final String DOWNLOAD_BITS_FROM_SFTP_BACKUPSTORAGE_PATH = "/sharedmountpointprimarystorage/sftp/download";
    public static final String REVERT_VOLUME_FROM_SNAPSHOT_PATH = "/sharedmountpointprimarystorage/volume/revertfromsnapshot";
    public static final String MERGE_SNAPSHOT_PATH = "/sharedmountpointprimarystorage/snapshot/merge";
    public static final String OFFLINE_MERGE_SNAPSHOT_PATH = "/sharedmountpointprimarystorage/snapshot/offlinemerge";
    public static final String CREATE_EMPTY_VOLUME_PATH = "/sharedmountpointprimarystorage/volume/createempty";
    public static final String CHECK_BITS_PATH = "/sharedmountpointprimarystorage/bits/check";
    public static final String GET_VOLUME_SIZE_PATH = "/sharedmountpointprimarystorage/volume/getsize";

    public KvmBackend(PrimaryStorageVO self) {
        super(self);
    }

    private List<String> findConnectedHostByClusterUuid(String clusterUuid, boolean exceptionOnNotFound) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        q.add(HostVO_.status, Op.EQ, HostStatus.Connected);
        List<String> hostUuids = q.listValue();
        if (hostUuids.isEmpty() && exceptionOnNotFound) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("no connected host found in the cluster[uuid:%s]", clusterUuid)
            ));
        }

        return hostUuids;
    }

    protected <T extends AgentRsp> void httpCall(String path, final String hostUuid, AgentCmd cmd, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        httpCall(path, hostUuid, cmd, false, rspType, completion);
    }

    private <T extends AgentRsp> void httpCall(String path, final String hostUuid, AgentCmd cmd, boolean noCheckStatus, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setPath(path);
        msg.setNoStatusCheck(noCheckStatus);
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutManager.getTimeout(cmd.getClass(), "5m"));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                final T rsp = r.toResponse(rspType);
                if (!rsp.success) {
                    completion.fail(errf.stringToOperationError(rsp.error));
                    return;
                }

                if (rsp.totalCapacity != null && rsp.availableCapacity != null) {
                    new PrimaryStorageCapacityUpdater(self.getUuid()).run(new PrimaryStorageCapacityUpdaterRunnable() {
                        @Override
                        public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                            if (cap.getAvailableCapacity() == 0) {
                                cap.setAvailableCapacity(rsp.availableCapacity);
                            }

                            cap.setTotalCapacity(rsp.totalCapacity);
                            cap.setTotalPhysicalCapacity(rsp.totalCapacity);
                            cap.setAvailablePhysicalCapacity(rsp.availableCapacity);

                            return cap;
                        }
                    });
                }

                completion.success(rsp);
            }
        });
    }

    private void connect(String hostUuid, final Completion completion) {
        ConnectCmd cmd = new ConnectCmd();
        cmd.uuid = self.getUuid();
        cmd.mountPoint = self.getMountPath();

        httpCall(CONNECT_PATH, hostUuid, cmd, true, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp rsp) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void attachHook(final String clusterUuid, final Completion completion) {
        connectByClusterUuid(clusterUuid, completion);
    }

    @Override
    void handle(InstantiateVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion) {
        if (msg instanceof InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) {
            createRootVolume((InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg, completion);
        } else {
            createEmptyVolume(msg.getVolume(), msg.getDestHost().getUuid(), completion);
        }
    }

    public String makeRootVolumeInstallUrl(VolumeInventory vol) {
        return PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeRootVolumeInstallPath(vol));
    }

    public String makeDataVolumeInstallUrl(String volUuid) {
        return PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeDataVolumeInstallPath(volUuid));
    }

    public String makeCachedImageInstallUrl(ImageInventory iminv) {
        return PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeCachedImageInstallPath(iminv));
    }

    public String makeCachedImageInstallUrlFromImageUuidForTemplate(String imageUuid) {
        return PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeCachedImageInstallPathFromImageUuidForTemplate(imageUuid));
    }

    public String makeTemplateFromVolumeInWorkspacePath(String imageUuid) {
        return PathUtil.join(self.getMountPath(), "templateWorkspace", String.format("image-%s", imageUuid), String.format("%s.qcow2", imageUuid));
    }

    public String makeSnapshotInstallPath(VolumeInventory vol, VolumeSnapshotInventory snapshot) {
        String volPath;
        if (VolumeType.Data.toString().equals(vol.getType())) {
            volPath = makeDataVolumeInstallUrl(vol.getUuid());
        } else {
            volPath = makeRootVolumeInstallUrl(vol);
        }
        File volDir = new File(volPath).getParentFile();
        return PathUtil.join(volDir.getAbsolutePath(), "snapshots", String.format("%s.qcow2", snapshot.getUuid()));
    }

    public String makeSnapshotWorkspacePath(String imageUuid) {
        return PathUtil.join(
                self.getMountPath(),
                PrimaryStoragePathMaker.makeImageFromSnapshotWorkspacePath(imageUuid),
                String.format("%s.qcow2", imageUuid)
        );
    }

    class ImageCache {
        ImageInventory image;
        String backupStorageUuid;
        String primaryStorageInstallPath;
        String backupStorageInstallPath;

        void download(final ReturnValueCompletion<String> completion) {
            DebugUtils.Assert(image != null, "image cannot be null");
            DebugUtils.Assert(backupStorageUuid != null, "backup storage UUID cannot be null");
            DebugUtils.Assert(primaryStorageInstallPath != null, "primaryStorageInstallPath cannot be null");
            DebugUtils.Assert(backupStorageInstallPath != null, "backupStorageInstallPath cannot be null");

            thdf.chainSubmit(new ChainTask() {
                @Override
                public String getSyncSignature() {
                    return String.format("download-image-%s-to-shared-mountpoint-storage-%s-cache", image.getUuid(), self.getUuid());
                }

                private void doDownload(final SyncTaskChain chain) {
                    FlowChain fchain = FlowChainBuilder.newShareFlowChain();
                    fchain.setName(String.format("download-image-%s-to-shared-mountpoint-storage-%s-cache",
                            image.getUuid(), self.getUuid()));
                    fchain.then(new ShareFlow() {
                        @Override
                        public void setup() {
                            flow(new Flow() {
                                String __name__ = "allocate-primary-storage";

                                boolean s = false;

                                @Override
                                public void run(final FlowTrigger trigger, Map data) {
                                    AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                                    amsg.setRequiredPrimaryStorageUuid(self.getUuid());
                                    amsg.setSize(image.getActualSize());
                                    amsg.setPurpose(PrimaryStorageAllocationPurpose.DownloadImage.toString());
                                    amsg.setNoOverProvisioning(true);
                                    bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                                    bus.send(amsg, new CloudBusCallBack(trigger) {
                                        @Override
                                        public void run(MessageReply reply) {
                                            if (reply.isSuccess()) {
                                                s = true;
                                                trigger.next();
                                            } else {
                                                trigger.fail(reply.getError());
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void rollback(FlowRollback trigger, Map data) {
                                    if (s) {
                                        ReturnPrimaryStorageCapacityMsg rmsg = new ReturnPrimaryStorageCapacityMsg();
                                        rmsg.setDiskSize(image.getActualSize());
                                        rmsg.setNoOverProvisioning(true);
                                        rmsg.setPrimaryStorageUuid(self.getUuid());
                                        bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
                                        bus.send(rmsg);
                                    }

                                    trigger.rollback();
                                }
                            });

                            flow(new NoRollbackFlow() {
                                String __name__ = "download";

                                @Override
                                public void run(final FlowTrigger trigger, Map data) {
                                    BackupStorageKvmDownloader downloader = getBackupStorageKvmDownloader(backupStorageUuid);
                                    downloader.downloadBits(backupStorageInstallPath, primaryStorageInstallPath, new Completion(trigger) {
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

                            done(new FlowDoneHandler(completion, chain) {
                                @Override
                                public void handle(Map data) {
                                    ImageCacheVO vo = new ImageCacheVO();
                                    vo.setState(ImageCacheState.ready);
                                    vo.setMediaType(ImageMediaType.valueOf(image.getMediaType()));
                                    vo.setImageUuid(image.getUuid());
                                    vo.setPrimaryStorageUuid(self.getUuid());
                                    vo.setSize(image.getActualSize());
                                    vo.setMd5sum("not calculated");
                                    vo.setInstallUrl(primaryStorageInstallPath);
                                    dbf.persist(vo);

                                    logger.debug(String.format("downloaded image[uuid:%s, name:%s] to the image cache of local shared mount point storage[uuid: %s, installPath: %s]",
                                            image.getUuid(), image.getName(), self.getUuid(), primaryStorageInstallPath));

                                    completion.success(primaryStorageInstallPath);
                                    chain.next();
                                }
                            });

                            error(new FlowErrorHandler(completion, chain) {
                                @Override
                                public void handle(ErrorCode errCode, Map data) {
                                    completion.fail(errCode);
                                    chain.next();
                                }
                            });
                        }
                    }).start();
                }

                @Override
                public void run(final SyncTaskChain chain) {
                    SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
                    q.select(ImageCacheVO_.installUrl);
                    q.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, self.getUuid());
                    q.add(ImageCacheVO_.imageUuid, Op.EQ, image.getUuid());
                    String fullPath = q.findValue();
                    if (fullPath == null) {
                        doDownload(chain);
                        return;
                    }

                    CheckBitsCmd cmd = new CheckBitsCmd();
                    cmd.path = primaryStorageInstallPath;

                    new Do().go(CHECK_BITS_PATH, cmd, CheckBitsRsp.class, new ReturnValueCompletion<AgentRsp>(completion, chain) {
                        @Override
                        public void success(AgentRsp returnValue) {
                            CheckBitsRsp rsp = (CheckBitsRsp) returnValue;
                            if (rsp.existing) {
                                completion.success(primaryStorageInstallPath);
                                chain.next();
                                return;
                            }

                            // the image is removed on the host
                            // delete the cache object and re-download it
                            SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
                            q.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, self.getUuid());
                            q.add(ImageCacheVO_.imageUuid, Op.EQ, image.getUuid());
                            ImageCacheVO vo = q.find();

                            ReturnPrimaryStorageCapacityMsg rmsg = new ReturnPrimaryStorageCapacityMsg();
                            rmsg.setDiskSize(vo.getSize());
                            rmsg.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, vo.getPrimaryStorageUuid());
                            bus.send(rmsg);

                            dbf.remove(vo);
                            doDownload(chain);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
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
    }

    private void createEmptyVolume(final VolumeInventory volume, String hostUuid, final ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion) {
        final CreateEmptyVolumeCmd cmd = new CreateEmptyVolumeCmd();
        cmd.installPath = VolumeType.Root.toString().equals(volume.getType()) ? makeRootVolumeInstallUrl(volume) : makeDataVolumeInstallUrl(volume.getUuid());
        cmd.name = volume.getName();
        cmd.size = volume.getSize();
        cmd.volumeUuid = volume.getUuid();

        new Do(hostUuid).go(CREATE_EMPTY_VOLUME_PATH, cmd, new ReturnValueCompletion<AgentRsp>() {
            @Override
            public void success(AgentRsp returnValue) {
                InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
                volume.setInstallPath(cmd.installPath);
                volume.setFormat(VolumeConstant.VOLUME_FORMAT_QCOW2);
                reply.setVolume(volume);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void createRootVolume(InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg msg, final ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion) {
        final ImageSpec ispec = msg.getTemplateSpec();
        final ImageInventory image = ispec.getInventory();

        if (!ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType())) {
            createEmptyVolume(msg.getVolume(), msg.getDestHost().getUuid(), completion);
            return;
        }

        final VolumeInventory volume = msg.getVolume();
        final String hostUuid = msg.getDestHost().getUuid();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("kvm-smp-storage-create-root-volume-from-image-%s", image.getUuid()));
        chain.then(new ShareFlow() {
            String pathInCache = makeCachedImageInstallUrl(image);
            String installPath;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "download-image-to-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ImageCache cache = new ImageCache();
                        cache.backupStorageUuid = ispec.getSelectedBackupStorage().getBackupStorageUuid();
                        cache.backupStorageInstallPath = ispec.getSelectedBackupStorage().getInstallPath();
                        cache.primaryStorageInstallPath = pathInCache;
                        cache.image = image;
                        cache.download(new ReturnValueCompletion<String>(trigger) {
                            @Override
                            public void success(String returnValue) {
                                pathInCache = returnValue;
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
                    String __name__ = "create-template-from-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        installPath = makeRootVolumeInstallUrl(volume);

                        CreateVolumeFromCacheCmd cmd = new CreateVolumeFromCacheCmd();
                        cmd.installPath = installPath;
                        cmd.templatePathInCache = pathInCache;
                        cmd.volumeUuid = volume.getUuid();

                        httpCall(CREATE_VOLUME_FROM_CACHE_PATH, hostUuid, cmd, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(trigger) {
                            @Override
                            public void success(AgentRsp rsp) {
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
                        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
                        volume.setInstallPath(installPath);
                        volume.setFormat(VolumeConstant.VOLUME_FORMAT_QCOW2);
                        reply.setVolume(volume);
                        completion.success(reply);
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

    @Transactional(readOnly = true)
    private List<String> findConnectedHosts(int num) {
        String sql = "select h.uuid from HostVO h, PrimaryStorageClusterRefVO ref where ref.clusterUuid = h.clusterUuid and" +
                " ref.primaryStorageUuid = :psUuid and h.status = :status and h.hypervisorType = :htype";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("psUuid", self.getUuid());
        q.setParameter("status", HostStatus.Connected);
        q.setParameter("htype", KVMConstant.KVM_HYPERVISOR_TYPE);
        q.setMaxResults(num);
        List<String> hostUuids = q.getResultList();
        Collections.shuffle(hostUuids);
        return hostUuids;
    }

    private String findConnectedHost() {
        List<String> huuids = findConnectedHosts(50);
        if (huuids.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError("cannot find any connected host to perform the operation"));
        }
        return huuids.get(0);
    }

    class Do {
        private List<String> hostUuids;
        private List<ErrorCode> errors = new ArrayList<ErrorCode>();

        public Do(String huuid) {
            hostUuids = new ArrayList<String>();
            hostUuids.add(huuid);
        }

        public Do() {
            hostUuids = findConnectedHosts(50);
            if (hostUuids.isEmpty()) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("cannot find any connected host to perform the operation, it seems all KVM hosts" +
                                        " in the clusters attached with the shared mount point storage[uuid:%s] are disconnected",
                                self.getUuid())
                ));
            }
        }

        void go(String path, AgentCmd cmd, ReturnValueCompletion<AgentRsp> completion) {
            go(path, cmd, AgentRsp.class, completion);
        }

        void go(String path, AgentCmd cmd, Class rspType, ReturnValueCompletion<AgentRsp> completion) {
            doCommand(hostUuids.iterator(), path, cmd, rspType, completion);
        }

        private void doCommand(final Iterator<String> it, final String path, final AgentCmd cmd, final Class rspType, final ReturnValueCompletion<AgentRsp> completion) {
            if (!it.hasNext()) {
                completion.fail(errf.stringToOperationError("an operation failed on all hosts", errors));
                return;
            }

            final String hostUuid = it.next();
            httpCall(path, hostUuid, cmd, rspType, new ReturnValueCompletion<AgentRsp>(completion) {
                @Override
                public void success(AgentRsp rsp) {
                    completion.success(rsp);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    if (!errorCode.isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE)) {
                        completion.fail(errorCode);
                        return;
                    }

                    errors.add(errorCode);
                    logger.warn(String.format("failed to do the command[%s] on the kvm host[uuid:%s], %s, try next one",
                            cmd.getClass(), hostUuid, errorCode));
                    doCommand(it, path, cmd, rspType, completion);
                }
            });
        }
    }


    @Override
    void handle(DeleteVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply> completion) {
        deleteBits(msg.getVolume().getInstallPath(), new Completion(completion) {
            @Override
            public void success() {
                DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }


    @Override
    void handle(final DownloadDataVolumeToPrimaryStorageMsg msg, final ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply> completion) {
        final String installPath = makeDataVolumeInstallUrl(msg.getVolumeUuid());
        BackupStorageKvmDownloader downloader = getBackupStorageKvmDownloader(msg.getBackupStorageRef().getBackupStorageUuid());
        downloader.downloadBits(msg.getBackupStorageRef().getInstallPath(), installPath, new Completion(completion) {
            @Override
            public void success() {
                DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
                reply.setFormat(msg.getImage().getFormat());
                reply.setInstallPath(installPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(DeleteBitsOnPrimaryStorageMsg msg, final ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply> completion) {
        deleteBits(msg.getInstallPath(), new Completion(completion) {
            @Override
            public void success() {
                DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(DownloadIsoToPrimaryStorageMsg msg, final ReturnValueCompletion<DownloadIsoToPrimaryStorageReply> completion) {
        ImageSpec ispec = msg.getIsoSpec();
        ImageCache cache = new ImageCache();

        cache.image = ispec.getInventory();
        cache.primaryStorageInstallPath = makeCachedImageInstallUrl(ispec.getInventory());
        cache.backupStorageUuid = ispec.getSelectedBackupStorage().getBackupStorageUuid();
        cache.backupStorageInstallPath = ispec.getSelectedBackupStorage().getInstallPath();
        cache.download(new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String returnValue) {
                DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
                reply.setInstallPath(returnValue);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(DeleteIsoFromPrimaryStorageMsg msg, ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply> completion) {
        // The ISO is in the image cache, no need to delete it
        DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
        completion.success(reply);
    }

    @Override
    void handle(TakeSnapshotMsg msg, final ReturnValueCompletion<TakeSnapshotReply> completion) {
        final VolumeSnapshotInventory sp = msg.getStruct().getCurrent();
        VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(sp.getVolumeUuid(), VolumeVO.class));

        final String hostUuid = findConnectedHost();

        TakeSnapshotOnHypervisorMsg hmsg = new TakeSnapshotOnHypervisorMsg();
        hmsg.setHostUuid(hostUuid);
        hmsg.setVmUuid(vol.getVmInstanceUuid());
        hmsg.setVolume(vol);
        hmsg.setSnapshotName(msg.getStruct().getCurrent().getUuid());
        hmsg.setFullSnapshot(msg.getStruct().isFullSnapshot());
        String installPath = makeSnapshotInstallPath(vol, sp);
        hmsg.setInstallPath(installPath);
        bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(hmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                TakeSnapshotOnHypervisorReply treply = (TakeSnapshotOnHypervisorReply) reply;
                sp.setSize(treply.getSize());
                sp.setPrimaryStorageUuid(self.getUuid());
                sp.setPrimaryStorageInstallPath(treply.getSnapshotInstallPath());
                sp.setType(VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());

                TakeSnapshotReply ret = new TakeSnapshotReply();
                ret.setNewVolumeInstallPath(treply.getNewVolumeInstallPath());
                ret.setInventory(sp);

                completion.success(ret);
            }
        });
    }

    @Override
    void handle(DeleteSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply> completion) {
        deleteBits(msg.getSnapshot().getPrimaryStorageInstallPath(), new Completion(completion) {
            @Override
            public void success() {
                DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply> completion) {
        VolumeSnapshotInventory sp = msg.getSnapshot();
        RevertVolumeFromSnapshotCmd cmd = new RevertVolumeFromSnapshotCmd();
        cmd.snapshotInstallPath = sp.getPrimaryStorageInstallPath();

        new Do().go(REVERT_VOLUME_FROM_SNAPSHOT_PATH, cmd, RevertVolumeFromSnapshotRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                RevertVolumeFromSnapshotRsp rsp = (RevertVolumeFromSnapshotRsp) returnValue;
                RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
                reply.setNewVolumeInstallPath(rsp.newVolumeInstallPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg, final ReturnValueCompletion<ReInitRootVolumeFromTemplateOnPrimaryStorageReply> completion) {
        RevertVolumeFromSnapshotCmd cmd = new RevertVolumeFromSnapshotCmd();
        cmd.snapshotInstallPath = makeCachedImageInstallUrlFromImageUuidForTemplate(msg.getVolume().getRootImageUuid());

        new Do().go(REVERT_VOLUME_FROM_SNAPSHOT_PATH, cmd, RevertVolumeFromSnapshotRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                RevertVolumeFromSnapshotRsp rsp = (RevertVolumeFromSnapshotRsp) returnValue;
                ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();
                reply.setNewVolumeInstallPath(rsp.newVolumeInstallPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        final String installPath = makeDataVolumeInstallUrl(msg.getVolumeUuid());
        VolumeSnapshotInventory latest = msg.getSnapshot();
        MergeSnapshotCmd cmd = new MergeSnapshotCmd();
        cmd.volumeUuid = latest.getVolumeUuid();
        cmd.snapshotInstallPath = latest.getPrimaryStorageInstallPath();
        cmd.workspaceInstallPath = installPath;

        new Do().go(MERGE_SNAPSHOT_PATH, cmd, MergeSnapshotRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                MergeSnapshotRsp rsp = (MergeSnapshotRsp) returnValue;
                CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
                reply.setActualSize(rsp.actualSize);
                reply.setInstallPath(installPath);
                reply.setSize(rsp.size);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<MergeVolumeSnapshotOnPrimaryStorageReply> completion) {
        boolean offline = true;
        VolumeInventory volume = msg.getTo();
        VolumeSnapshotInventory sp = msg.getFrom();
        String hostUuid = null;
        if (volume.getVmInstanceUuid() != null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state, VmInstanceVO_.hostUuid);
            q.add(VmInstanceVO_.uuid, Op.EQ, volume.getVmInstanceUuid());
            Tuple t = q.findTuple();
            VmInstanceState state = t.get(0, VmInstanceState.class);
            hostUuid = t.get(1, String.class);

            if (state != VmInstanceState.Stopped && state != VmInstanceState.Running) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("the volume[uuid;%s] is attached to a VM[uuid:%s] which is in state of %s, cannot do the snapshot merge",
                                volume.getUuid(), volume.getVmInstanceUuid(), state)
                ));
            }

            offline = (state == VmInstanceState.Stopped);
        }

        final MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();

        if (offline) {
            OfflineMergeSnapshotCmd cmd = new OfflineMergeSnapshotCmd();
            cmd.fullRebase = msg.isFullRebase();
            cmd.srcPath = sp.getPrimaryStorageInstallPath();
            cmd.destPath = volume.getInstallPath();

            new Do().go(OFFLINE_MERGE_SNAPSHOT_PATH, cmd, new ReturnValueCompletion<AgentRsp>(completion) {
                @Override
                public void success(AgentRsp returnValue) {
                    completion.success(reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
        } else {
            MergeVolumeSnapshotOnKvmMsg kmsg = new MergeVolumeSnapshotOnKvmMsg();
            kmsg.setFullRebase(msg.isFullRebase());
            kmsg.setHostUuid(hostUuid);
            kmsg.setFrom(sp);
            kmsg.setTo(volume);
            bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
            bus.send(kmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply r) {
                    if (r.isSuccess()) {
                        completion.success(reply);
                    } else {
                        completion.fail(r.getError());
                    }
                }
            });
        }
    }

    @Override
    void downloadImageToCache(ImageInventory img, final ReturnValueCompletion<String> completion) {
        ImageBackupStorageSelector selector = new ImageBackupStorageSelector();
        selector.setZoneUuid(self.getZoneUuid());
        selector.setImageUuid(img.getUuid());
        final String bsUuid = selector.select();
        if (bsUuid == null) {
            throw new OperationFailureException(errf.stringToOperationError(String.format(
                    "the image[uuid:%s, name: %s] is not available to download on any backup storage:\n" +
                            "1. check if image is in status of Deleted\n" +
                            "2. check if the backup storage on which the image is shown as Ready is attached to the zone[uuid:%s]",
                    img.getUuid(), img.getName(), self.getZoneUuid()
            )));
        }

        ImageBackupStorageRefInventory ref = CollectionUtils.find(img.getBackupStorageRefs(), new Function<ImageBackupStorageRefInventory, ImageBackupStorageRefInventory>() {
            @Override
            public ImageBackupStorageRefInventory call(ImageBackupStorageRefInventory arg) {
                return arg.getBackupStorageUuid().equals(bsUuid) ? arg : null;
            }
        });

        final ImageCache cache = new ImageCache();
        cache.image = img;
        cache.primaryStorageInstallPath = makeCachedImageInstallUrl(img);
        cache.backupStorageUuid = bsUuid;
        cache.backupStorageInstallPath = ref.getInstallPath();
        cache.download(new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String returnValue) {
                completion.success(cache.primaryStorageInstallPath);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void deleteBits(String path, final Completion completion) {
        DeleteBitsCmd cmd = new DeleteBitsCmd();
        cmd.path = path;
        new Do().go(DELETE_BITS_PATH, cmd, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp rsp) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<CreateTemplateFromVolumeOnPrimaryStorageReply> completion) {
        final CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();
        final VolumeInventory volume = msg.getVolumeInventory();
        final ImageInventory image = msg.getImageInventory();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-template-%s-from-volume-%s", image.getUuid(), volume.getUuid()));
        chain.then(new ShareFlow() {
            String temporaryTemplatePath = makeTemplateFromVolumeInWorkspacePath(msg.getImageInventory().getUuid());
            String backupStorageInstallPath;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-temporary-template";

                    boolean success = false;

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CreateTemplateFromVolumeCmd cmd = new CreateTemplateFromVolumeCmd();
                        cmd.volumePath = volume.getInstallPath();
                        cmd.installPath = temporaryTemplatePath;
                        new Do().go(CREATE_TEMPLATE_FROM_VOLUME_PATH, cmd, new ReturnValueCompletion<AgentRsp>(trigger) {
                            @Override
                            public void success(AgentRsp returnValue) {
                                success = true;
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
                        if (success) {
                            deleteBits(temporaryTemplatePath, new Completion() {
                                @Override
                                public void success() {
                                    // pass
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    //TODO
                                    logger.warn(String.format("failed to delete %s, %s", temporaryTemplatePath, errorCode));
                                }
                            });
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "upload-to-backup-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        BackupStorageAskInstallPathMsg bmsg = new BackupStorageAskInstallPathMsg();
                        bmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        bmsg.setImageMediaType(image.getMediaType());
                        bmsg.setImageUuid(image.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(bmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                        MessageReply br = bus.call(bmsg);
                        if (!br.isSuccess()) {
                            trigger.fail(br.getError());
                            return;
                        }

                        backupStorageInstallPath = ((BackupStorageAskInstallPathReply) br).getInstallPath();

                        BackupStorageKvmUploader uploader = getBackupStorageKvmUploader(msg.getBackupStorageUuid());
                        uploader.uploadBits(backupStorageInstallPath, temporaryTemplatePath, new ReturnValueCompletion<String>(trigger) {
                            @Override
                            public void success(String bsPath) {
                                backupStorageInstallPath = bsPath;
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
                    String __name__ = "delete-temporary-template-on-primary-storage";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        deleteBits(temporaryTemplatePath, new Completion(trigger) {
                            @Override
                            public void success() {
                                // pass
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO: cleanup
                                logger.warn(String.format("failed to delete %s on shared mount point primary storage[uuid: %s], %s; need a cleanup", temporaryTemplatePath, self.getUuid(), errorCode));
                            }
                        });

                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        reply.setFormat(volume.getFormat());
                        reply.setTemplateBackupStorageInstallPath(backupStorageInstallPath);
                        completion.success(reply);
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
    void handle(UploadBitsToBackupStorageMsg msg, final ReturnValueCompletion<UploadBitsToBackupStorageReply> completion) {
        SftpBackupStorageKvmUploader uploader = new SftpBackupStorageKvmUploader(msg.getBackupStorageUuid());
        uploader.uploadBits(msg.getBackupStorageInstallPath(), msg.getPrimaryStorageInstallPath(), new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String bsPath) {
                UploadBitsToBackupStorageReply reply = new UploadBitsToBackupStorageReply();
                reply.setBackupStorageInstallPath(bsPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    class SftpBackupStorageKvmDownloader extends BackupStorageKvmDownloader {
        private final String bsUuid;

        public SftpBackupStorageKvmDownloader(String backupStorageUuid) {
            bsUuid = backupStorageUuid;
        }

        @Override
        public void downloadBits(final String bsPath, final String psPath, final Completion completion) {
            GetSftpBackupStorageDownloadCredentialMsg gmsg = new GetSftpBackupStorageDownloadCredentialMsg();
            gmsg.setBackupStorageUuid(bsUuid);
            bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, bsUuid);

            bus.send(gmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }

                    final GetSftpBackupStorageDownloadCredentialReply greply = reply.castReply();
                    SftpDownloadBitsCmd cmd = new SftpDownloadBitsCmd();
                    cmd.hostname = greply.getHostname();
                    cmd.username = greply.getUsername();
                    cmd.sshKey = greply.getSshKey();
                    cmd.sshPort = greply.getSshPort();
                    cmd.backupStorageInstallPath = bsPath;
                    cmd.primaryStorageInstallPath = psPath;

                    new Do().go(DOWNLOAD_BITS_FROM_SFTP_BACKUPSTORAGE_PATH, cmd, new ReturnValueCompletion<AgentRsp>(completion) {
                        @Override
                        public void success(AgentRsp returnValue) {
                            completion.success();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                        }
                    });
                }
            });
        }
    }

    class SftpBackupStorageKvmUploader extends BackupStorageKvmUploader {
        private final String bsUuid;

        public SftpBackupStorageKvmUploader(String backupStorageUuid) {
            bsUuid = backupStorageUuid;
        }

        @Override
        public void uploadBits(final String bsPath, final String psPath, final ReturnValueCompletion<String> completion) {
            GetSftpBackupStorageDownloadCredentialMsg gmsg = new GetSftpBackupStorageDownloadCredentialMsg();
            gmsg.setBackupStorageUuid(bsUuid);
            bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, bsUuid);
            bus.send(gmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }

                    final GetSftpBackupStorageDownloadCredentialReply r = reply.castReply();
                    SftpUploadBitsCmd cmd = new SftpUploadBitsCmd();
                    cmd.primaryStorageInstallPath = psPath;
                    cmd.backupStorageInstallPath = bsPath;
                    cmd.hostname = r.getHostname();
                    cmd.username = r.getUsername();
                    cmd.sshKey = r.getSshKey();
                    cmd.sshPort = r.getSshPort();

                    new Do().go(UPLOAD_BITS_TO_SFTP_BACKUPSTORAGE_PATH, cmd, new ReturnValueCompletion<AgentRsp>(completion) {
                        @Override
                        public void success(AgentRsp returnValue) {
                            completion.success(bsPath);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                        }
                    });
                }
            });
        }
    }

    private BackupStorageKvmDownloader getBackupStorageKvmDownloader(String backupStorageUuid) {
        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.select(BackupStorageVO_.type);
        q.add(BackupStorageVO_.uuid, Op.EQ, backupStorageUuid);
        String bsType = q.findValue();

        if (SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE.equals(bsType)) {
            return new SftpBackupStorageKvmDownloader(backupStorageUuid);
        } else {
            for (BackupStorageKvmFactory f : pluginRgty.getExtensionList(BackupStorageKvmFactory.class)) {
                if (bsType.equals(f.getBackupStorageType())) {
                    return f.createDownloader(getSelfInventory(), backupStorageUuid);
                }
            }

            throw new CloudRuntimeException(String.format("cannot find any BackupStorageKvmFactory for the type[%s]", bsType));
        }
    }


    private BackupStorageKvmUploader getBackupStorageKvmUploader(String backupStorageUuid) {
        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.select(BackupStorageVO_.type);
        q.add(BackupStorageVO_.uuid, Op.EQ, backupStorageUuid);
        String bsType = q.findValue();

        if (bsType == null) {
            throw new OperationFailureException(errf.stringToOperationError(String.format("cannot find backup storage[uuid:%s]", backupStorageUuid)));
        }

        if (SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE.equals(bsType)) {
            return new SftpBackupStorageKvmUploader(backupStorageUuid);
        } else {
            for (BackupStorageKvmFactory f : pluginRgty.getExtensionList(BackupStorageKvmFactory.class)) {
                if (bsType.equals(f.getBackupStorageType())) {
                    return f.createUploader(getSelfInventory(), backupStorageUuid);
                }
            }

            throw new CloudRuntimeException(String.format("cannot find any BackupStorageKvmFactory for the type[%s]", bsType));
        }
    }


    @Override
    void handleHypervisorSpecificMessage(SMPPrimaryStorageHypervisorSpecificMessage msg) {
        if (msg instanceof InitKvmHostMsg) {
            handle((InitKvmHostMsg) msg);
        } else {
            bus.dealWithUnknownMessage((Message) msg);
        }
    }

    @Override
    void connectByClusterUuid(final String clusterUuid, final Completion completion) {
        List<String> huuids = findConnectedHostByClusterUuid(clusterUuid, false);
        if (huuids.isEmpty()) {
            // no host in the cluster
            completion.success();
            return;
        }

        class Result {
            List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
            boolean success = false;
        }

        final Result ret = new Result();
        final AsyncLatch latch = new AsyncLatch(huuids.size(), new NoErrorCompletion(completion) {
            @Override
            public void done() {
                if (ret.success) {
                    completion.success();
                } else {
                    completion.fail(errf.stringToOperationError(String.format("unable to connect the shared mount point storage[uuid:%s, name:%s] to" +
                            " the cluster[uuid:%s]", self.getUuid(), self.getName(), clusterUuid), ret.errorCodes));
                }
            }
        });

        for (String huuid : huuids) {
            connect(huuid, new Completion(latch) {
                @Override
                public void success() {
                    ret.success = true;
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    ret.errorCodes.add(errorCode);
                    latch.ack();
                }
            });
        }
    }

    @Override
    void handle(SyncVolumeSizeOnPrimaryStorageMsg msg, final ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply> completion) {
        String hostUuid = findConnectedHost();
        final GetVolumeSizeCmd cmd = new GetVolumeSizeCmd();
        cmd.installPath = msg.getInstallPath();
        cmd.volumeUuid = msg.getVolumeUuid();
        new KvmCommandSender(hostUuid).send(cmd, GET_VOLUME_SIZE_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                GetVolumeSizeRsp rsp = wrapper.getResponse(GetVolumeSizeRsp.class);
                return rsp.success ? null : errf.stringToOperationError(rsp.error);
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>() {
            @Override
            public void success(KvmResponseWrapper returnValue) {
                SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
                GetVolumeSizeRsp rsp = returnValue.getResponse(GetVolumeSizeRsp.class);
                reply.setActualSize(rsp.actualSize);
                reply.setSize(rsp.size);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });

    }

    @Override
    void handle(CreateTemporaryVolumeFromSnapshotMsg msg, final ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply> completion) {
        final String installPath = makeTemplateFromVolumeInWorkspacePath(msg.getTemporaryVolumeUuid());
        VolumeSnapshotInventory latest = msg.getSnapshot();
        MergeSnapshotCmd cmd = new MergeSnapshotCmd();
        cmd.volumeUuid = latest.getVolumeUuid();
        cmd.snapshotInstallPath = latest.getPrimaryStorageInstallPath();
        cmd.workspaceInstallPath = installPath;

        new Do().go(MERGE_SNAPSHOT_PATH, cmd, MergeSnapshotRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp rsp) {
                CreateTemporaryVolumeFromSnapshotReply reply = new CreateTemporaryVolumeFromSnapshotReply();
                reply.setInstallPath(installPath);
                MergeSnapshotRsp mrsp = (MergeSnapshotRsp) rsp;
                reply.setSize(mrsp.size);
                reply.setActualSize(mrsp.actualSize);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg, final ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply> completion) {
        VolumeSnapshotInventory sinv = msg.getSnapshot();
        String bsUuid = msg.getBackupStorage().getUuid();

        // Get the backup storage install path
        BackupStorageAskInstallPathMsg bmsg = new BackupStorageAskInstallPathMsg();
        bmsg.setImageMediaType(VolumeSnapshotVO.class.getSimpleName());
        bmsg.setBackupStorageUuid(msg.getBackupStorage().getUuid());
        bmsg.setImageUuid(sinv.getUuid());
        bus.makeTargetServiceIdByResourceUuid(bmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorage().getUuid());
        MessageReply br = bus.call(bmsg);
        if (!br.isSuccess()) {
            completion.fail(br.getError());
            return;
        }

        final String installPath = ((BackupStorageAskInstallPathReply) br).getInstallPath();
        BackupStorageKvmUploader uploader = getBackupStorageKvmUploader(bsUuid);
        uploader.uploadBits(installPath, sinv.getPrimaryStorageInstallPath(), new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String bsPath) {
                BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
                reply.setBackupStorageInstallPath(bsPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void handle(final InitKvmHostMsg msg) {
        final InitKvmHostReply reply = new InitKvmHostReply();
        connect(msg.getHostUuid(), new Completion(msg) {
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
