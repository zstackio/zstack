package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.validation.Validation;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg.SnapshotDownloadInfo;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotReply.CreateTemplateFromVolumeSnapshotResult;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.MergeVolumeSnapshotOnKvmMsg;
import org.zstack.storage.primary.PrimaryStoragePathMaker;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.util.*;

/**
 * Created by frank on 6/30/2015.
 */
public class LocalStorageKvmBackend extends LocalStorageHypervisorBackend {
    private final static CLogger logger = Utils.getLogger(LocalStorageKvmBackend.class);

    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private LocalStorageFactory localStorageFactory;

    public static class AgentCommand {
    }

    public static class AgentResponse {
        private Long totalCapacity;
        private Long availableCapacity;

        private boolean success = true;
        private String error;
        public boolean isSuccess() {
            return success;
        }
        public void setSuccess(boolean success) {
            this.success = success;
        }
        public String getError() {
            return error;
        }
        public void setError(String error) {
            this.error = error;
        }
        public Long getTotalCapacity() {
            return totalCapacity;
        }

        public void setTotalCapacity(Long totalCapacity) {
            this.totalCapacity = totalCapacity;
        }

        public Long getAvailableCapacity() {
            return availableCapacity;
        }

        public void setAvailableCapacity(Long availableCapacity) {
            this.availableCapacity = availableCapacity;
        }
    }

    public static class InitCmd extends AgentCommand {
        private String path;
        private String hostUuid;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class CreateEmptyVolumeCmd extends AgentCommand {
        private String installUrl;
        private long size;
        private String accountUuid;
        private String name;
        private String volumeUuid;

        public String getInstallUrl() {
            return installUrl;
        }

        public void setInstallUrl(String installUrl) {
            this.installUrl = installUrl;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }
    }

    public static class CreateEmptyVolumeRsp extends AgentResponse {
    }

    public static class GetPhysicalCapacityCmd extends AgentCommand {
        private String hostUuid;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }
    }

    public static class CreateVolumeFromCacheCmd extends AgentCommand {
        private String templatePathInCache;
        private String installUrl;
        private String volumeUuid;

        public String getTemplatePathInCache() {
            return templatePathInCache;
        }

        public void setTemplatePathInCache(String templatePathInCache) {
            this.templatePathInCache = templatePathInCache;
        }

        public String getInstallUrl() {
            return installUrl;
        }

        public void setInstallUrl(String installUrl) {
            this.installUrl = installUrl;
        }

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }
    }

    public static class CreateVolumeFromCacheRsp extends AgentResponse {

    }

    public static class DeleteBitsCmd extends AgentCommand {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class DeleteBitsRsp extends AgentResponse {
    }

    public static class CreateTemplateFromVolumeCmd extends AgentCommand {
        private String installPath;
        private String volumePath;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
        public String getVolumePath() {
            return volumePath;
        }
        public void setVolumePath(String rootVolumePath) {
            this.volumePath = rootVolumePath;
        }
    }

    public static class CreateTemplateFromVolumeRsp extends AgentResponse {
    }

    public static class RevertVolumeFromSnapshotCmd extends AgentCommand {
        private String snapshotInstallPath;

        public String getSnapshotInstallPath() {
            return snapshotInstallPath;
        }

        public void setSnapshotInstallPath(String snapshotInstallPath) {
            this.snapshotInstallPath = snapshotInstallPath;
        }
    }

    public static class RevertVolumeFromSnapshotRsp extends AgentResponse {
        @Validation
        private String newVolumeInstallPath;

        public String getNewVolumeInstallPath() {
            return newVolumeInstallPath;
        }

        public void setNewVolumeInstallPath(String newVolumeInstallPath) {
            this.newVolumeInstallPath = newVolumeInstallPath;
        }
    }

    public static class MergeSnapshotCmd extends AgentCommand {
        private String snapshotInstallPath;
        private String workspaceInstallPath;

        public String getSnapshotInstallPath() {
            return snapshotInstallPath;
        }

        public void setSnapshotInstallPath(String snapshotInstallPath) {
            this.snapshotInstallPath = snapshotInstallPath;
        }

        public String getWorkspaceInstallPath() {
            return workspaceInstallPath;
        }

        public void setWorkspaceInstallPath(String workspaceInstallPath) {
            this.workspaceInstallPath = workspaceInstallPath;
        }
    }

    public static class MergeSnapshotRsp extends AgentResponse {
        private long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class RebaseAndMergeSnapshotsCmd extends AgentCommand {
        private List<String> snapshotInstallPaths;
        private String workspaceInstallPath;

        public List<String> getSnapshotInstallPaths() {
            return snapshotInstallPaths;
        }

        public void setSnapshotInstallPaths(List<String> snapshotInstallPaths) {
            this.snapshotInstallPaths = snapshotInstallPaths;
        }

        public String getWorkspaceInstallPath() {
            return workspaceInstallPath;
        }

        public void setWorkspaceInstallPath(String workspaceInstallPath) {
            this.workspaceInstallPath = workspaceInstallPath;
        }
    }

    public static class RebaseAndMergeSnapshotsRsp extends AgentResponse {
        private long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class OfflineMergeSnapshotCmd extends AgentCommand {
        private String srcPath;
        private String destPath;
        private boolean fullRebase;

        public boolean isFullRebase() {
            return fullRebase;
        }

        public void setFullRebase(boolean fullRebase) {
            this.fullRebase = fullRebase;
        }

        public String getSrcPath() {
            return srcPath;
        }

        public void setSrcPath(String srcPath) {
            this.srcPath = srcPath;
        }

        public String getDestPath() {
            return destPath;
        }

        public void setDestPath(String destPath) {
            this.destPath = destPath;
        }
    }

    public static class OfflineMergeSnapshotRsp extends AgentResponse {
    }

    public static final String INIT_PATH = "/localstorage/init";
    public static final String GET_PHYSICAL_CAPACITY_PATH = "/localstorage/getphysicalcapacity";
    public static final String CREATE_EMPTY_VOLUME_PATH = "/localstorage/volume/createempty";
    public static final String CREATE_VOLUME_FROM_CACHE_PATH = "/localstorage/volume/createvolumefromcache";
    public static final String DELETE_BITS_PATH = "/localstorage/delete";
    public static final String CREATE_TEMPLATE_FROM_VOLUME = "/localstorage/volume/createtemplate";
    public static final String REVERT_SNAPSHOT_PATH = "/localstorage/snapshot/revert";
    public static final String MERGE_SNAPSHOT_PATH = "/localstorage/snapshot/merge";
    public static final String MERGE_AND_REBASE_SNAPSHOT_PATH = "/localstorage/snapshot/mergeandrebase";
    public static final String OFFLINE_MERGE_PATH = "/localstorage/snapshot/offlinemerge";

    public LocalStorageKvmBackend(PrimaryStorageVO self) {
        super(self);
    }

    public String makeRootVolumeInstallUrl(VolumeInventory vol) {
        return PathUtil.join(self.getUrl(), PrimaryStoragePathMaker.makeRootVolumeInstallPath(vol));
    }

    public String makeDataVolumeInstallUrl(String volUuid) {
        return PathUtil.join(self.getUrl(), PrimaryStoragePathMaker.makeDataVolumeInstallPath(volUuid));
    }

    public String makeCachedImageInstallUrl(ImageInventory iminv) {
        return PathUtil.join(self.getUrl(), PrimaryStoragePathMaker.makeCachedImageInstallPath(iminv));
    }

    public String makeTemplateFromVolumeInWorkspacePath(String imageUuid) {
        return PathUtil.join(self.getUrl(), "templateWorkspace", String.format("image-%s", imageUuid), String.format("%s.qcow2", imageUuid));
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
                self.getUrl(),
                PrimaryStoragePathMaker.makeImageFromSnapshotWorkspacePath(imageUuid),
                String.format("%s.qcow2", imageUuid)
        );
    }

    @Override
    void syncPhysicalCapacityInCluster(List<ClusterInventory> clusters, final ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        List<String> clusterUuids = CollectionUtils.transformToList(clusters, new Function<String, ClusterInventory>() {
            @Override
            public String call(ClusterInventory arg) {
                return arg.getUuid();
            }
        });

        final PhysicalCapacityUsage ret = new PhysicalCapacityUsage();

        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.IN, clusterUuids);
        final List<String> hostUuids = q.listValue();

        if (hostUuids.isEmpty()) {
            completion.success(ret);
            return;
        }

        List<KVMHostAsyncHttpCallMsg> msgs = CollectionUtils.transformToList(hostUuids, new Function<KVMHostAsyncHttpCallMsg, String>() {
            @Override
            public KVMHostAsyncHttpCallMsg call(String arg) {
                GetPhysicalCapacityCmd cmd = new GetPhysicalCapacityCmd();
                cmd.setHostUuid(arg);

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(arg);
                msg.setCommand(cmd);
                msg.setPath(GET_PHYSICAL_CAPACITY_PATH);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, arg);
                return msg;
            }
        });

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply reply : replies) {
                    String hostUuid = hostUuids.get(replies.indexOf(reply));

                    if (!reply.isSuccess()) {
                        logger.warn(String.format("cannot get the physical capacity of local storage on the host[uuid:%s], %s", hostUuid, reply.getError()));
                        continue;
                    }

                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    AgentResponse rsp = r.toResponse(AgentResponse.class);

                    if (!rsp.isSuccess()) {
                        logger.warn(String.format("cannot get the physical capacity of local storage on the host[uuid:%s], %s", hostUuid, rsp.getError()));
                        continue;
                    }

                    ret.totalPhysicalSize += rsp.getTotalCapacity();
                    ret.availablePhysicalSize += rsp.getAvailableCapacity();
                }

                completion.success(ret);
            }
        });
    }

    @Override
    void handle(InstantiateVolumeMsg msg, ReturnValueCompletion<InstantiateVolumeReply> completion) {
        if (msg instanceof  InstantiateRootVolumeFromTemplateMsg) {
            createRootVolume((InstantiateRootVolumeFromTemplateMsg) msg, completion);
        } else {
            createEmptyVolume(msg, completion);
        }
    }

    private void createEmptyVolume(final InstantiateVolumeMsg msg, final ReturnValueCompletion<InstantiateVolumeReply> completion) {
        String hostUuid = msg.getDestHost().getUuid();
        final CreateEmptyVolumeCmd cmd = new CreateEmptyVolumeCmd();
        cmd.setAccountUuid(acntMgr.getOwnerAccountUuidOfResource(msg.getVolume().getUuid()));
        if (VolumeType.Root.toString().equals(msg.getVolume().getType())) {
            cmd.setInstallUrl(makeRootVolumeInstallUrl(msg.getVolume()));
        } else {
            cmd.setInstallUrl(makeDataVolumeInstallUrl(msg.getVolume().getUuid()));
        }
        cmd.setName(msg.getVolume().getName());
        cmd.setSize(msg.getVolume().getSize());
        cmd.setVolumeUuid(msg.getVolume().getUuid());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(hostUuid);
        kmsg.setPath(CREATE_EMPTY_VOLUME_PATH);
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        final String finalHostUuid = hostUuid;
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                InstantiateVolumeReply r = new InstantiateVolumeReply();
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply kr = reply.castReply();
                CreateEmptyVolumeRsp rsp = kr.toResponse(CreateEmptyVolumeRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(errf.stringToOperationError(
                            String.format("unable to create an empty volume[uuid:%s, name:%s] on the kvm host[uuid:%s], %s",
                                    msg.getVolume().getUuid(), msg.getVolume().getName(), finalHostUuid, rsp.getError())
                    ));
                    return;
                }

                VolumeInventory vol = msg.getVolume();
                vol.setInstallPath(cmd.getInstallUrl());
                r.setVolume(vol);

                completion.success(r);
            }
        });
    }

    private String getHostUuidByResourceUuid(String resUuid, String resType) {
        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.select(LocalStorageResourceRefVO_.hostUuid);
        q.add(LocalStorageResourceRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, resUuid);
        String hostUuid = q.findValue();

        if (hostUuid == null) {
            throw new CloudRuntimeException(String.format("resource[uuid:%s, type:%s] is not any on any host of local primary storage[uuid:%s]",
                    resUuid, resType, self.getUuid()));
        }

        return hostUuid;
    }

    class CacheInstallPath {
        String fullPath;
        String hostUuid;
        String installPath;

        CacheInstallPath disassemble() {
            DebugUtils.Assert(fullPath != null, "fullPath cannot be null");
            String[] pair = fullPath.split(";");
            installPath = pair[0].replaceFirst("file://", "");
            hostUuid = pair[1].replaceFirst("hostUuid://", "");
            return this;
        }

        String makeFullPath() {
            DebugUtils.Assert(installPath != null, "installPath cannot be null");
            DebugUtils.Assert(hostUuid != null, "hostUuid cannot be null");
            fullPath = String.format("file://%s;hostUuid://%s", installPath, hostUuid);
            return fullPath;
        }
    }

    class ImageCache {
        ImageInventory image;
        BackupStorageInventory backupStorage;
        String hostUuid;
        String primaryStorageInstallPath;
        String backupStorageInstallPath;

        void download(final ReturnValueCompletion<String> completion) {
            DebugUtils.Assert(image != null, "image cannot be null");
            DebugUtils.Assert(backupStorage != null, "backup storage cannot be null");
            DebugUtils.Assert(hostUuid != null, "host uuid cannot be null");
            DebugUtils.Assert(primaryStorageInstallPath != null, "primaryStorageInstallPath cannot be null");
            DebugUtils.Assert(backupStorageInstallPath != null, "backupStorageInstallPath cannot be null");

            thdf.chainSubmit(new ChainTask(completion) {
                @Override
                public String getSyncSignature() {
                    return String.format("download-image-%s-to-localstorage-%s-cache", image.getUuid(), self.getUuid());
                }

                @Override
                public void run(final SyncTaskChain chain) {
                    SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
                    q.select(ImageCacheVO_.installUrl);
                    q.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, self.getUuid());
                    q.add(ImageCacheVO_.imageUuid, Op.EQ, image.getUuid());
                    q.add(ImageCacheVO_.installUrl, Op.LIKE, String.format("%%hostUuid://%s%%", hostUuid));
                    String fullPath = q.findValue();
                    if (fullPath != null) {
                        CacheInstallPath path = new CacheInstallPath();
                        path.fullPath = fullPath;
                        String installPath = path.disassemble().installPath;
                        logger.debug(String.format("found image[uuid: %s, name: %s] in the image cache of local primary storage[uuid:%s, installPath: %s]",
                                image.getUuid(), image.getName(), self.getUuid(), installPath));
                        completion.success(installPath);
                        chain.next();
                        return;
                    }

                    LocalStorageBackupStorageMediator m = localStorageFactory.getBackupStorageMediator(KVMConstant.KVM_HYPERVISOR_TYPE, backupStorage.getType());
                    m.downloadBits(getSelfInventory(), backupStorage,
                            backupStorageInstallPath, primaryStorageInstallPath,
                            hostUuid, new Completion(completion, chain) {
                                @Override
                                public void success() {
                                    ImageCacheVO vo = new ImageCacheVO();
                                    vo.setState(ImageCacheState.ready);
                                    vo.setMediaType(ImageMediaType.valueOf(image.getMediaType()));
                                    vo.setImageUuid(image.getUuid());
                                    vo.setPrimaryStorageUuid(self.getUuid());
                                    vo.setSize(image.getSize());
                                    vo.setMd5sum("not calculated");

                                    CacheInstallPath path = new CacheInstallPath();
                                    path.installPath = primaryStorageInstallPath;
                                    path.hostUuid = hostUuid;
                                    vo.setInstallUrl(path.makeFullPath());
                                    dbf.persist(vo);

                                    logger.debug(String.format("downloaded image[uuid:%s, name:%s] to the image cache of local primary storage[uuid: %s, installPath: %s] on host[uuid: %s]",
                                            image.getUuid(), image.getName(), self.getUuid(), primaryStorageInstallPath, hostUuid));
                                    completion.success(primaryStorageInstallPath);
                                    chain.next();
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

    private void createRootVolume(InstantiateRootVolumeFromTemplateMsg msg, final ReturnValueCompletion<InstantiateVolumeReply> completion) {
        final ImageSpec ispec = msg.getTemplateSpec();
        final ImageInventory image = ispec.getInventory();

        if (!ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType())) {
            createEmptyVolume(msg, completion);
            return;
        }

        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.add(BackupStorageVO_.uuid, Op.EQ, ispec.getSelectedBackupStorage().getBackupStorageUuid());
        BackupStorageVO bs = q.find();

        final BackupStorageInventory bsInv = BackupStorageInventory.valueOf(bs);
        final VolumeInventory volume = msg.getVolume();
        final String hostUuid = msg.getDestHost().getUuid();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("kvm-localstorage-create-root-volume-from-image-%s", image.getUuid()));
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
                        cache.backupStorage = bsInv;
                        cache.backupStorageInstallPath = ispec.getSelectedBackupStorage().getInstallPath();
                        cache.primaryStorageInstallPath = pathInCache;
                        cache.hostUuid = hostUuid;
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
                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        installPath = makeRootVolumeInstallUrl(volume);

                        CreateVolumeFromCacheCmd cmd = new CreateVolumeFromCacheCmd();
                        cmd.setInstallUrl(installPath);
                        cmd.setTemplatePathInCache(pathInCache);
                        cmd.setVolumeUuid(volume.getUuid());

                        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                        kmsg.setCommand(cmd);
                        kmsg.setHostUuid(hostUuid);
                        kmsg.setPath(CREATE_VOLUME_FROM_CACHE_PATH);
                        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
                        bus.send(kmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                KVMHostAsyncHttpCallReply kr = reply.castReply();
                                CreateVolumeFromCacheRsp rsp = kr.toResponse(CreateVolumeFromCacheRsp.class);
                                if (!rsp.isSuccess()) {
                                    trigger.fail(errf.stringToOperationError(rsp.getError()));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        InstantiateVolumeReply reply = new InstantiateVolumeReply();
                        volume.setInstallPath(installPath);
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

    private void deleteBits(String path, String hostUuid, final Completion completion) {
        DeleteBitsCmd cmd = new DeleteBitsCmd();
        cmd.setPath(path);

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(hostUuid);
        kmsg.setCommand(cmd);
        kmsg.setPath(DELETE_BITS_PATH);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply kr = reply.castReply();
                DeleteBitsRsp rsp = kr.toResponse(DeleteBitsRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(errf.stringToOperationError(rsp.getError()));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    void handle(DeleteVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply> completion) {
        String hostUuid = getHostUuidByResourceUuid(msg.getVolume().getUuid(), VolumeVO.class.getSimpleName());
        deleteBits(msg.getVolume().getInstallPath(), hostUuid, new Completion(completion) {
            @Override
            public void success() {
                DeleteVolumeOnPrimaryStorageReply dreply = new DeleteVolumeOnPrimaryStorageReply();
                completion.success(dreply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(final DownloadDataVolumeToPrimaryStorageMsg msg, final ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply> completion) {
        BackupStorageVO bsvo = dbf.findByUuid(msg.getBackupStorageRef().getBackupStorageUuid(), BackupStorageVO.class);
        LocalStorageBackupStorageMediator m = localStorageFactory.getBackupStorageMediator(KVMConstant.KVM_HYPERVISOR_TYPE, bsvo.getType());
        final String installPath = makeDataVolumeInstallUrl(msg.getVolumeUuid());
        m.downloadBits(getSelfInventory(), BackupStorageInventory.valueOf(bsvo), msg.getBackupStorageRef().getInstallPath(), installPath, msg.getHostUuid(), new Completion(completion) {
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
        String hostUuid = getHostUuidByResourceUuid(msg.getBitsUuid(), msg.getBitsType());
        deleteBits(msg.getInstallPath(), hostUuid, new Completion(completion) {
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
        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.add(BackupStorageVO_.uuid, Op.EQ, ispec.getSelectedBackupStorage().getBackupStorageUuid());
        BackupStorageVO bsvo = q.find();
        BackupStorageInventory bsinv = BackupStorageInventory.valueOf(bsvo);

        ImageCache cache = new ImageCache();
        cache.image = ispec.getInventory();
        cache.hostUuid = msg.getDestHostUuid();
        cache.primaryStorageInstallPath = makeCachedImageInstallUrl(ispec.getInventory());
        cache.backupStorage = bsinv;
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
    void handle(InitPrimaryStorageOnHostConnectedMsg msg, final ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        InitCmd cmd = new InitCmd();
        cmd.setHostUuid(msg.getHostUuid());
        cmd.setPath(self.getUrl());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setCommand(cmd);
        kmsg.setHostUuid(msg.getHostUuid());
        kmsg.setPath(INIT_PATH);
        kmsg.setCommand(cmd);
        kmsg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply kr = reply.castReply();
                AgentResponse rsp = kr.toResponse(AgentResponse.class);
                if (!rsp.isSuccess()) {
                    completion.fail(errf.stringToOperationError(rsp.getError()));
                    return;
                }

                PhysicalCapacityUsage usage = new PhysicalCapacityUsage();
                usage.totalPhysicalSize = rsp.getTotalCapacity();
                usage.availablePhysicalSize = rsp.getAvailableCapacity();
                completion.success(usage);
            }
        });
    }

    @Override
    void handle(final TakeSnapshotMsg msg, String hostUuid, final ReturnValueCompletion<TakeSnapshotReply> completion) {
        final VolumeSnapshotInventory sp = msg.getStruct().getCurrent();
        VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(sp.getVolumeUuid(), VolumeVO.class));

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

                TakeSnapshotOnHypervisorReply treply = (TakeSnapshotOnHypervisorReply)reply;
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
    void handle(DeleteSnapshotOnPrimaryStorageMsg msg, String hostUuid, final ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply> completion) {
        deleteBits(msg.getSnapshot().getPrimaryStorageInstallPath(), hostUuid, new Completion(completion) {
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
    void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg, String hostUuid, final ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply> completion) {
        VolumeSnapshotInventory sp = msg.getSnapshot();
        RevertVolumeFromSnapshotCmd cmd = new RevertVolumeFromSnapshotCmd();
        cmd.setSnapshotInstallPath(sp.getPrimaryStorageInstallPath());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(hostUuid);
        kmsg.setPath(REVERT_SNAPSHOT_PATH);
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply kr = reply.castReply();
                RevertVolumeFromSnapshotRsp rsp = kr.toResponse(RevertVolumeFromSnapshotRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(errf.stringToOperationError(rsp.getError()));
                    return;
                }

                RevertVolumeFromSnapshotOnPrimaryStorageReply ret = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
                ret.setNewVolumeInstallPath(rsp.getNewVolumeInstallPath());
                completion.success(ret);
            }
        });
    }

    @Override
    void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg, String hostUuid, final ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply> completion) {
        VolumeSnapshotInventory sp = msg.getSnapshot();
        LocalStorageBackupStorageMediator m = localStorageFactory.getBackupStorageMediator(KVMConstant.KVM_HYPERVISOR_TYPE, msg.getBackupStorage().getType());
        BackupStorageAskInstallPathMsg bmsg = new BackupStorageAskInstallPathMsg();
        bmsg.setImageMediaType(VolumeSnapshotVO.class.getSimpleName());
        bmsg.setBackupStorageUuid(msg.getBackupStorage().getUuid());
        bmsg.setImageUuid(sp.getUuid());
        bus.makeTargetServiceIdByResourceUuid(bmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorage().getUuid());
        MessageReply br = bus.call(bmsg);
        if (!br.isSuccess()) {
            completion.fail(br.getError());
            return;
        }

        final String installPath = ((BackupStorageAskInstallPathReply)br).getInstallPath();

        m.uploadBits(getSelfInventory(), msg.getBackupStorage(), installPath, sp.getPrimaryStorageInstallPath(), hostUuid, new Completion(completion) {
            @Override
            public void success() {
                BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
                reply.setBackupStorageInstallPath(installPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    class CreateTemplateOrVolumeFromSnapshots {
        List<SnapshotDownloadInfo> infos;
        String hostUuid;
        boolean needDownload;
        String primaryStorageInstallPath;

        private void createTemplateWithDownload(final ReturnValueCompletion<Long> completion) {
            FlowChain c = FlowChainBuilder.newShareFlowChain();
            c.setName("download-snapshots-and-create-template");
            c.then(new ShareFlow() {
                long totalSnapshotSize;
                List<String> snapshotInstallPaths;

                long templateSize;

                @Override
                public void setup() {
                    for (SnapshotDownloadInfo i : infos) {
                        totalSnapshotSize += i.getSnapshot().getSize();
                    }

                    flow(new Flow() {
                        String __name__ = "reserve-capacity-for-downloading-snapshots";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            reserveCapacityOnHost(hostUuid, totalSnapshotSize);
                            trigger.next();
                        }

                        @Override
                        public void rollback(FlowTrigger trigger, Map data) {
                            returnCapacityToHost(hostUuid, totalSnapshotSize);
                            trigger.rollback();
                        }
                    });

                    flow(new Flow() {
                        String __name__ = "download-snapshots";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            download(infos.iterator(), new Completion(trigger) {
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

                        private void download(final Iterator<SnapshotDownloadInfo> it, final Completion completion) {
                            if (!it.hasNext()) {
                                Collections.reverse(snapshotInstallPaths);
                                completion.success();
                                return;
                            }

                            SnapshotDownloadInfo i = it.next();
                            BackupStorageVO bsvo = dbf.findByUuid(i.getBackupStorageUuid(), BackupStorageVO.class);
                            LocalStorageBackupStorageMediator m = localStorageFactory.getBackupStorageMediator(KVMConstant.KVM_HYPERVISOR_TYPE, bsvo.getType());
                            final String pinstallPath = makeSnapshotWorkspacePath(i.getSnapshot().getUuid());
                            m.downloadBits(getSelfInventory(), BackupStorageInventory.valueOf(bsvo), i.getBackupStorageInstallPath(), pinstallPath, hostUuid, new Completion(completion) {
                                @Override
                                public void success() {
                                    snapshotInstallPaths.add(pinstallPath);
                                    download(it, completion);
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    completion.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowTrigger trigger, Map data) {
                            for (String path : snapshotInstallPaths) {
                                //TODO
                                deleteBits(path, hostUuid, new NopeCompletion());
                            }

                            trigger.rollback();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "rebase-and-merge-snapshots-on-host";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            RebaseAndMergeSnapshotsCmd cmd = new RebaseAndMergeSnapshotsCmd();
                            cmd.setSnapshotInstallPaths(snapshotInstallPaths);
                            cmd.setWorkspaceInstallPath(primaryStorageInstallPath);

                            KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                            kmsg.setCommand(cmd);
                            kmsg.setPath(MERGE_AND_REBASE_SNAPSHOT_PATH);
                            kmsg.setHostUuid(hostUuid);
                            bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
                            bus.send(kmsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                        return;
                                    }

                                    KVMHostAsyncHttpCallReply kr = reply.castReply();
                                    RebaseAndMergeSnapshotsRsp rsp = kr.toResponse(RebaseAndMergeSnapshotsRsp.class);
                                    if (!rsp.isSuccess()) {
                                        trigger.fail(errf.stringToOperationError(rsp.getError()));
                                        return;
                                    }

                                    templateSize = rsp.getSize();
                                    trigger.next();
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-temporary-snapshot-in-workspace";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            for (String installPath : snapshotInstallPaths) {
                                deleteBits(installPath, hostUuid, new NopeCompletion());
                            }

                            trigger.next();
                        }
                    });

                    done(new FlowDoneHandler(completion) {
                        @Override
                        public void handle(Map data) {
                            completion.success(templateSize);
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


        private void createTemplateWithoutDownload(final ReturnValueCompletion<Long> completion) {
            VolumeSnapshotInventory latest = infos.get(infos.size()-1).getSnapshot();
            MergeSnapshotCmd cmd = new MergeSnapshotCmd();
            cmd.setSnapshotInstallPath(latest.getPrimaryStorageInstallPath());
            cmd.setWorkspaceInstallPath(primaryStorageInstallPath);

            KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
            kmsg.setCommand(cmd);
            kmsg.setPath(MERGE_SNAPSHOT_PATH);
            kmsg.setHostUuid(hostUuid);
            bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
            bus.send(kmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }

                    MergeSnapshotRsp rsp = ((KVMHostAsyncHttpCallReply) reply).toResponse(MergeSnapshotRsp.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(errf.stringToOperationError(rsp.getError()));
                        return;
                    }

                    completion.success(rsp.getSize());
                }
            });
        }

        void create(ReturnValueCompletion<Long> completion) {
            DebugUtils.Assert(infos != null, "infos cannot be null");
            DebugUtils.Assert(hostUuid != null, "hostUuid cannot be null");
            DebugUtils.Assert(primaryStorageInstallPath != null, "workSpaceInstallPath cannot be null");

            if (needDownload) {
                createTemplateWithDownload(completion);
            } else {
                createTemplateWithoutDownload(completion);
            }
        }
    }


    @Override
    void handle(final CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg msg, final String hostUuid, final ReturnValueCompletion<CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        final List<SnapshotDownloadInfo> infos = msg.getSnapshotsDownloadInfo();

        SimpleQuery<ImageVO> q = dbf.createQuery(ImageVO.class);
        q.select(ImageVO_.mediaType);
        q.add(ImageVO_.uuid, Op.EQ, msg.getImageUuid());
        final String mediaType = q.findValue().toString();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-template-%s-from-snapshots", msg.getImageUuid()));
        chain.then(new ShareFlow() {
            String workSpaceInstallPath = makeSnapshotWorkspacePath(msg.getImageUuid());
            long templateSize;

            class Result {
                BackupStorageInventory backupStorageInventory;
                String installPath;
            }

            List<Result> successBackupStorage = new ArrayList<Result>();

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-template-on-primary-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CreateTemplateOrVolumeFromSnapshots c = new CreateTemplateOrVolumeFromSnapshots();
                        c.infos = infos;
                        c.primaryStorageInstallPath = workSpaceInstallPath;
                        c.needDownload = msg.isNeedDownload();
                        c.hostUuid = hostUuid;
                        c.create(new ReturnValueCompletion<Long>(trigger) {
                            @Override
                            public void success(Long returnValue) {
                                templateSize = returnValue;
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
                        deleteBits(workSpaceInstallPath, hostUuid, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO
                                logger.warn(String.format("failed to delete %s on local primary storage[uuid: %s], %s; continue to rollback", workSpaceInstallPath, self.getUuid(), errorCode));
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "upload-template-to-backup-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        upload(msg.getBackupStorage().iterator(), new Completion() {
                            @Override
                            public void success() {
                                if (successBackupStorage.isEmpty()) {
                                    trigger.fail(errf.stringToInternalError("failed to upload the template to all backup storage"));
                                } else {
                                    trigger.next();
                                }
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });

                    }

                    private void upload(final Iterator<BackupStorageInventory> it, final Completion completion) {
                        if (!it.hasNext()) {
                            completion.success();
                            return;
                        }

                        final BackupStorageInventory bs = it.next();
                        BackupStorageAskInstallPathMsg bmsg = new BackupStorageAskInstallPathMsg();
                        bmsg.setImageMediaType(mediaType);
                        bmsg.setImageUuid(msg.getImageUuid());
                        bmsg.setBackupStorageUuid(bs.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(bmsg, BackupStorageConstant.SERVICE_ID, bs.getUuid());
                        MessageReply br = bus.call(bmsg);
                        if (!br.isSuccess()) {
                            logger.warn(String.format("failed to get install path on backup storage[uuid: %s] for image[uuid:%s]", bs.getUuid(), msg.getImageUuid()));
                            upload(it, completion);
                            return;
                        }

                        final String backupStorageInstallPath = ((BackupStorageAskInstallPathReply) br).getInstallPath();
                        LocalStorageBackupStorageMediator m = localStorageFactory.getBackupStorageMediator(KVMConstant.KVM_HYPERVISOR_TYPE, bs.getType());
                        m.uploadBits(getSelfInventory(), bs, backupStorageInstallPath, workSpaceInstallPath, hostUuid, new Completion(completion) {
                            @Override
                            public void success() {
                                Result ret = new Result();
                                ret.backupStorageInventory = bs;
                                ret.installPath = backupStorageInstallPath;
                                successBackupStorage.add(ret);
                                upload(it, completion);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO
                                logger.warn(String.format("failed to upload template[%s] from local primary storage[uuid: %s] to the backup storage[uuid: %s, path: %s]",
                                        workSpaceInstallPath, self.getUuid(), bs.getUuid(), backupStorageInstallPath));
                                upload(it, completion);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-temporary-template-from-primary-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        deleteBits(workSpaceInstallPath, hostUuid, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO
                                logger.warn(String.format("failed to delete temporary template[%s] from primary storage[uuid:%s], %s; need a cleanup", workSpaceInstallPath, self.getUuid(), errorCode));
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply();
                        List<CreateTemplateFromVolumeSnapshotResult> ret = CollectionUtils.transformToList(successBackupStorage, new Function<CreateTemplateFromVolumeSnapshotResult, Result>() {
                            @Override
                            public CreateTemplateFromVolumeSnapshotResult call(Result arg) {
                                CreateTemplateFromVolumeSnapshotResult r = new CreateTemplateFromVolumeSnapshotResult();
                                r.setBackupStorageUuid(arg.backupStorageInventory.getUuid());
                                r.setInstallPath(arg.installPath);
                                return r;
                            }
                        });
                        reply.setResults(ret);
                        reply.setSize(templateSize);
                        completion.success(reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, String hostUuid, final ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        final CreateTemplateOrVolumeFromSnapshots c = new CreateTemplateOrVolumeFromSnapshots();
        c.hostUuid = hostUuid;
        c.needDownload = msg.isNeedDownload();
        c.primaryStorageInstallPath = makeDataVolumeInstallUrl(msg.getVolumeUuid());
        c.infos = msg.getSnapshots();
        c.create(new ReturnValueCompletion<Long>(completion) {
            @Override
            public void success(Long returnValue) {
                CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
                reply.setInstallPath(c.primaryStorageInstallPath);
                reply.setSize(returnValue);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg, String hostUuid, final ReturnValueCompletion<MergeVolumeSnapshotOnPrimaryStorageReply> completion) {
        boolean offline = true;
        VolumeInventory volume = msg.getTo();
        VolumeSnapshotInventory sp = msg.getFrom();
        if (volume.getVmInstanceUuid() != null) {
            SimpleQuery<VmInstanceVO> q  = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state);
            q.add(VmInstanceVO_.uuid, Op.EQ, volume.getVmInstanceUuid());
            VmInstanceState state = q.findValue();
            offline = (state == VmInstanceState.Stopped);
        }

        final MergeVolumeSnapshotOnPrimaryStorageReply ret = new MergeVolumeSnapshotOnPrimaryStorageReply();

        if (offline) {
            OfflineMergeSnapshotCmd cmd = new OfflineMergeSnapshotCmd();
            cmd.setFullRebase(msg.isFullRebase());
            cmd.setSrcPath(sp.getPrimaryStorageInstallPath());
            cmd.setDestPath(volume.getInstallPath());

            KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
            kmsg.setCommand(cmd);
            kmsg.setPath(OFFLINE_MERGE_PATH);
            kmsg.setHostUuid(hostUuid);
            bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
            bus.send(kmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }

                    OfflineMergeSnapshotRsp rsp = ((KVMHostAsyncHttpCallReply)reply).toResponse(OfflineMergeSnapshotRsp.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(errf.stringToOperationError(rsp.getError()));
                        return;
                    }

                    completion.success(ret);
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
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        completion.success(ret);
                    } else {
                        completion.fail(reply.getError());
                    }
                }
            });
        }
    }

    @Override
    public void detachHook(String clusterUuid, final Completion completion) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        final List<String> hostUuids = q.listValue();

        if (hostUuids.isEmpty()) {
            completion.success();
            return;
        }

        SimpleQuery<LocalStorageHostRefVO> refq = dbf.createQuery(LocalStorageHostRefVO.class);
        refq.add(LocalStorageHostRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
        refq.add(LocalStorageHostRefVO_.hostUuid, Op.IN, hostUuids);
        List<LocalStorageHostRefVO> refs = refq.list();
        if (!refs.isEmpty()) {
            dbf.removeCollection(refs, LocalStorageHostRefVO.class);

            long total = 0;
            for (LocalStorageHostRefVO ref : refs) {
                total += ref.getTotalCapacity();
            }

            // after detaching, total capacity on those hosts should be deducted
            // from both total and available capacity of the primary storage
            decreaseCapacity(total, total, null, null);
        }

        syncPhysicalCapacity(new ReturnValueCompletion<PhysicalCapacityUsage>(completion) {
            @Override
            public void success(PhysicalCapacityUsage returnValue) {
                setCapacity(null, null, returnValue.totalPhysicalSize, returnValue.availablePhysicalSize);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to sync the physical capacity on the local primary storage[uuid:%s], %s", self.getUuid(), errorCode));
                completion.success();
            }
        });

    }

    @Override
    public void attachHook(String clusterUuid, final Completion completion) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        final List<String> hostUuids = q.listValue();

        if (hostUuids.isEmpty()) {
            completion.success();
            return;
        }

        List<KVMHostAsyncHttpCallMsg> msgs = CollectionUtils.transformToList(hostUuids, new Function<KVMHostAsyncHttpCallMsg, String>() {
            @Override
            public KVMHostAsyncHttpCallMsg call(String arg) {
                InitCmd cmd = new InitCmd();
                cmd.path = self.getUrl();
                cmd.hostUuid = arg;

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setCommand(cmd);
                msg.setPath(INIT_PATH);
                msg.setHostUuid(arg);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, arg);
                return msg;
            }
        });

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                long total = 0;
                long avail = 0;
                List<LocalStorageHostRefVO> refs = new ArrayList<LocalStorageHostRefVO>();

                for (MessageReply reply : replies) {
                    String hostUuid = hostUuids.get(replies.indexOf(reply));
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("cannot get the physical capacity of local storage on the host[uuid:%s], %s", hostUuid, reply.getError()));
                        continue;
                    }

                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    AgentResponse rsp = r.toResponse(AgentResponse.class);
                    if (!rsp.isSuccess()) {
                        logger.warn(String.format("cannot get the physical capacity of local storage on the host[uuid:%s], %s", hostUuid, rsp.getError()));
                        continue;
                    }

                    if (dbf.isExist(hostUuid, LocalStorageHostRefVO.class)) {
                        logger.debug(String.format("host[uuid :%s] is already in the local primary storage[uuid: %s]", hostUuid, self.getUuid()));
                        continue;
                    }

                    total += rsp.getTotalCapacity();
                    avail += rsp.getAvailableCapacity();

                    LocalStorageHostRefVO ref = new LocalStorageHostRefVO();
                    ref.setPrimaryStorageUuid(self.getUuid());
                    ref.setHostUuid(hostUuid);
                    ref.setAvailablePhysicalCapacity(rsp.getAvailableCapacity());
                    ref.setAvailableCapacity(rsp.getAvailableCapacity());
                    ref.setTotalCapacity(rsp.getTotalCapacity());
                    ref.setTotalPhysicalCapacity(rsp.getTotalCapacity());
                    refs.add(ref);

                }

                dbf.persistCollection(refs);

                increaseCapacity(total, avail, total, avail);

                completion.success();
            }
        });
    }

    @Override
    protected  void handle(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        final LocalStorageResourceRefVO ref = dbf.findByUuid(msg.getVolumeInventory().getUuid(), LocalStorageResourceRefVO.class);
        final CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-image-%s-from-volume-%s", msg.getImageInventory().getUuid(), msg.getVolumeInventory().getUuid()));
        chain.then(new ShareFlow() {
            String temporaryTemplatePath = makeTemplateFromVolumeInWorkspacePath(msg.getImageInventory().getUuid());
            String backupStorageInstallPath;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "reserve-capacity-on-the-host-for-template";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        reserveCapacityOnHost(ref.getHostUuid(), msg.getVolumeInventory().getSize());
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        returnCapacityToHost(ref.getHostUuid(), msg.getVolumeInventory().getSize());
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-temporary-template";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CreateTemplateFromVolumeCmd cmd = new CreateTemplateFromVolumeCmd();
                        cmd.setInstallPath(temporaryTemplatePath);
                        cmd.setVolumePath(msg.getVolumeInventory().getInstallPath());

                        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                        kmsg.setHostUuid(ref.getHostUuid());
                        kmsg.setPath(CREATE_TEMPLATE_FROM_VOLUME);
                        kmsg.setCommand(cmd);
                        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, ref.getHostUuid());
                        bus.send(kmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                KVMHostAsyncHttpCallReply kr = reply.castReply();
                                CreateTemplateFromVolumeRsp rsp = kr.toResponse(CreateTemplateFromVolumeRsp.class);
                                if (!rsp.isSuccess()) {
                                    trigger.fail(errf.stringToOperationError(rsp.getError()));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(final FlowTrigger trigger, Map data) {
                        deleteBits(temporaryTemplatePath, ref.getHostUuid(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.warn(String.format("failed to delete %s on primary storage[uuid: %s], %s; continue to rollback", temporaryTemplatePath, self.getUuid(), errorCode));
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "upload-template-to-backup-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        BackupStorageAskInstallPathMsg bmsg = new BackupStorageAskInstallPathMsg();
                        bmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        bmsg.setImageMediaType(msg.getImageInventory().getMediaType());
                        bmsg.setImageUuid(msg.getImageInventory().getUuid());
                        bus.makeTargetServiceIdByResourceUuid(bmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                        MessageReply br = bus.call(bmsg);
                        if (!br.isSuccess()) {
                            trigger.fail(br.getError());
                            return;
                        }

                        backupStorageInstallPath = ((BackupStorageAskInstallPathReply) br).getInstallPath();

                        BackupStorageVO bsvo = dbf.findByUuid(msg.getBackupStorageUuid(), BackupStorageVO.class);
                        LocalStorageBackupStorageMediator m = localStorageFactory.getBackupStorageMediator(KVMConstant.KVM_HYPERVISOR_TYPE, bsvo.getType());
                        m.uploadBits(getSelfInventory(), BackupStorageInventory.valueOf(bsvo), backupStorageInstallPath, temporaryTemplatePath, ref.getHostUuid(), new Completion(trigger) {
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
                    String __name__ = "delete-temporary-template-on-primary-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        deleteBits(temporaryTemplatePath, ref.getHostUuid(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO: cleanup
                                logger.warn(String.format("failed to delete %s on local primary storage[uuid: %s], %s; need a cleanup", temporaryTemplatePath, self.getUuid(), errorCode));
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "return-capacity-of-temporary-template-to-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        returnCapacityToHost(ref.getHostUuid(), msg.getVolumeInventory().getSize());
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        reply.setFormat(msg.getVolumeInventory().getFormat());
                        reply.setTemplateBackupStorageInstallPath(backupStorageInstallPath);
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
}
