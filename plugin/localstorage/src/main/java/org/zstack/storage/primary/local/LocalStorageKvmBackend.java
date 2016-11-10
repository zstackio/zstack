package org.zstack.storage.primary.local;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.ImageBackupStorageSelector;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.gc.EventBasedGCPersistentContext;
import org.zstack.core.gc.GCEventTrigger;
import org.zstack.core.gc.GCFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.core.ApiTimeout;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.validation.Validation;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
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
import org.zstack.identity.AccountManager;
import org.zstack.kvm.*;
import org.zstack.storage.primary.PrimaryStoragePathMaker;
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow.CopyBitsFromRemoteCmd;
import org.zstack.storage.primary.local.MigrateBitsStruct.ResourceInfo;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.Tuple;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.StringDSL.ln;

/**
 * Created by frank on 6/30/2015.
 */
public class LocalStorageKvmBackend extends LocalStorageHypervisorBackend {
    private final static CLogger logger = Utils.getLogger(LocalStorageKvmBackend.class);

    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private LocalStorageFactory localStorageFactory;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    private GCFacade gcf;

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
        private String backingFile;

        public String getBackingFile() {
            return backingFile;
        }

        public void setBackingFile(String backingFile) {
            this.backingFile = backingFile;
        }

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

    @ApiTimeout(apiClasses = {APICreateVmInstanceMsg.class})
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

    @ApiTimeout(apiClasses = {
            APICreateRootVolumeTemplateFromRootVolumeMsg.class,
            APICreateDataVolumeTemplateFromVolumeMsg.class,
            APICreateDataVolumeFromVolumeSnapshotMsg.class,
            APILocalStorageMigrateVolumeMsg.class,
            APIDeleteVolumeSnapshotMsg.class,
    })
    public static class DeleteBitsCmd extends AgentCommand {
        private String hostUuid;
        private String path;

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

    public static class DeleteBitsRsp extends AgentResponse {
    }

    @ApiTimeout(apiClasses = {
            APICreateRootVolumeTemplateFromRootVolumeMsg.class,
            APICreateDataVolumeTemplateFromVolumeMsg.class
    })
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
        private String volumeUuid;
        private String snapshotInstallPath;
        private String workspaceInstallPath;

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

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
        private long actualSize;

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    @ApiTimeout(apiClasses = {APICreateRootVolumeTemplateFromVolumeSnapshotMsg.class})
    public static class RebaseAndMergeSnapshotsCmd extends AgentCommand {
        private String volumeUuid;
        private List<String> snapshotInstallPaths;
        private String workspaceInstallPath;

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

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
        private long actualSize;

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    @ApiTimeout(apiClasses = {APIDeleteVolumeSnapshotMsg.class})
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

    public static class CheckBitsCmd extends AgentCommand {
        public String path;
    }

    public static class CheckBitsRsp extends AgentResponse {
        public boolean existing;
    }

    public static class RebaseRootVolumeToBackingFileCmd extends AgentCommand {
        public String backingFilePath;
        public String rootVolumePath;
    }

    public static class RebaseRootVolumeToBackingFileRsp extends AgentResponse {
    }

    public static class GetMd5TO {
        public String resourceUuid;
        public String path;
    }

    @ApiTimeout(apiClasses = {APILocalStorageMigrateVolumeMsg.class})
    public static class GetMd5Cmd extends AgentCommand {
        public List<GetMd5TO> md5s;
    }

    public static class Md5TO {
        public String resourceUuid;
        public String path;
        public String md5;
    }

    public static class GetMd5Rsp extends AgentResponse {
        public List<Md5TO> md5s;
    }


    @ApiTimeout(apiClasses = {APILocalStorageMigrateVolumeMsg.class})
    public static class CheckMd5sumCmd extends AgentCommand {
        public List<Md5TO> md5s;
    }

    @ApiTimeout(apiClasses = {APILocalStorageMigrateVolumeMsg.class})
    public static class GetBackingFileCmd extends AgentCommand {
        public String path;
        public String volumeUuid;
    }

    public static class GetBackingFileRsp extends AgentResponse {
        public String backingFilePath;
        public Long size;
    }

    public static class GetVolumeBaseImagePathCmd extends AgentCommand {
        public String volumeUuid;
        public String installPath;
    }

    public static class GetVolumeBaseImagePathRsp extends AgentResponse {
        public String path;
    }

    public static class GetVolumeSizeCmd extends AgentCommand {
        public String volumeUuid;
        public String installPath;
    }

    public static class GetVolumeSizeRsp extends AgentResponse {
        public long actualSize;
        public long size;
    }

    public static class GetQCOW2ReferenceCmd extends AgentCommand {
        public String path;
        public String searchingDir;
    }

    public static class GetQCOW2ReferenceRsp extends AgentResponse {
        List<String> referencePaths;
    }

    public static final String INIT_PATH = "/localstorage/init";
    public static final String GET_PHYSICAL_CAPACITY_PATH = "/localstorage/getphysicalcapacity";
    public static final String CREATE_EMPTY_VOLUME_PATH = "/localstorage/volume/createempty";
    public static final String CREATE_VOLUME_FROM_CACHE_PATH = "/localstorage/volume/createvolumefromcache";
    public static final String DELETE_BITS_PATH = "/localstorage/delete";
    public static final String CHECK_BITS_PATH = "/localstorage/checkbits";
    public static final String CREATE_TEMPLATE_FROM_VOLUME = "/localstorage/volume/createtemplate";
    public static final String REVERT_SNAPSHOT_PATH = "/localstorage/snapshot/revert";
    public static final String MERGE_SNAPSHOT_PATH = "/localstorage/snapshot/merge";
    public static final String MERGE_AND_REBASE_SNAPSHOT_PATH = "/localstorage/snapshot/mergeandrebase";
    public static final String OFFLINE_MERGE_PATH = "/localstorage/snapshot/offlinemerge";
    public static final String GET_MD5_PATH = "/localstorage/getmd5";
    public static final String CHECK_MD5_PATH = "/localstorage/checkmd5";
    public static final String GET_BACKING_FILE_PATH = "/localstorage/volume/getbackingfile";
    public static final String GET_VOLUME_SIZE = "/localstorage/volume/getsize";
    public static final String GET_BASE_IMAGE_PATH = "/localstorage/volume/getbaseimagepath";
    public static final String GET_QCOW2_REFERENCE = "/localstorage/getqcow2reference";


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

    public String makeCachedImageInstallUrlFromImageUuidForTemplate(String imageUuid) {
        return PathUtil.join(self.getUrl(), PrimaryStoragePathMaker.makeCachedImageInstallPathFromImageUuidForTemplate(imageUuid));
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
        final List<String> hostInClusterUuids = q.listValue();

        if (hostInClusterUuids.isEmpty()) {
            completion.success(ret);
            return;
        }

        SimpleQuery<LocalStorageHostRefVO> sq = dbf.createQuery(LocalStorageHostRefVO.class);
        q.select(LocalStorageHostRefVO_.hostUuid);
        q.add(LocalStorageHostRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
        q.add(LocalStorageHostRefVO_.hostUuid, Op.IN, hostInClusterUuids);
        final List<String> hostUuids = sq.listValue();

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
                msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
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
                        //TODO
                        logger.warn(String.format("cannot get the physical capacity of local storage on the host[uuid:%s], %s", hostUuid, reply.getError()));
                        continue;
                    }

                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    AgentResponse rsp = r.toResponse(AgentResponse.class);

                    if (!rsp.isSuccess()) {
                        //TODO
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

    protected <T extends AgentResponse> void httpCall(String path, final String hostUuid, AgentCommand cmd, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        httpCall(path, hostUuid, cmd, false, rspType, completion);
    }

    protected <T extends AgentResponse> void httpCall(String path, final String hostUuid, AgentCommand cmd, boolean noCheckStatus, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setPath(path);
        msg.setNoStatusCheck(noCheckStatus);
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
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
                    new LocalStorageCapacityUpdater().updatePhysicalCapacityByKvmAgentResponse(self.getUuid(), hostUuid, rsp);
                }

                completion.success(rsp);
            }
        });
    }

    @Override
    protected void handle(final InstantiateVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion) {
        if (msg instanceof InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) {
            createRootVolume((InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg, completion);
        } else {
            createEmptyVolume(msg.getVolume(), msg.getDestHost().getUuid(), new ReturnValueCompletion<String>(completion) {
                @Override
                public void success(String returnValue) {
                    InstantiateVolumeOnPrimaryStorageReply r = new InstantiateVolumeOnPrimaryStorageReply();
                    VolumeInventory vol = msg.getVolume();
                    vol.setInstallPath(returnValue);
                    vol.setFormat(VolumeConstant.VOLUME_FORMAT_QCOW2);
                    r.setVolume(vol);
                    completion.success(r);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
        }
    }

    private void createEmptyVolume(final VolumeInventory volume, final String hostUuid, final ReturnValueCompletion<String> completion) {
        createEmptyVolume(volume, hostUuid, null, completion);
    }

    private void createEmptyVolume(final VolumeInventory volume, final String hostUuid, final String backingFile, final ReturnValueCompletion<String> completion) {
        final CreateEmptyVolumeCmd cmd = new CreateEmptyVolumeCmd();
        cmd.setAccountUuid(acntMgr.getOwnerAccountUuidOfResource(volume.getUuid()));
        if (VolumeType.Root.toString().equals(volume.getType())) {
            cmd.setInstallUrl(makeRootVolumeInstallUrl(volume));
        } else {
            cmd.setInstallUrl(makeDataVolumeInstallUrl(volume.getUuid()));
        }
        cmd.setName(volume.getName());
        cmd.setSize(volume.getSize());
        cmd.setVolumeUuid(volume.getUuid());
        cmd.setBackingFile(backingFile);

        httpCall(CREATE_EMPTY_VOLUME_PATH, hostUuid, cmd, CreateEmptyVolumeRsp.class, new ReturnValueCompletion<CreateEmptyVolumeRsp>(completion) {
            @Override
            public void success(CreateEmptyVolumeRsp returnValue) {
                completion.success(cmd.getInstallUrl());
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                        String.format("unable to create an empty volume[uuid:%s, name:%s] on the kvm host[uuid:%s]",
                                volume.getUuid(), volume.getName(), hostUuid), errorCode));
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

    public static class CacheInstallPath {
        public String fullPath;
        public String hostUuid;
        public String installPath;

        public CacheInstallPath disassemble() {
            DebugUtils.Assert(fullPath != null, "fullPath cannot be null");
            String[] pair = fullPath.split(";");
            DebugUtils.Assert(pair.length == 2, String.format("invalid cache path %s", fullPath));
            installPath = pair[0].replaceFirst("file://", "");
            hostUuid = pair[1].replaceFirst("hostUuid://", "");
            return this;
        }

        public String makeFullPath() {
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
                    return String.format("download-image-%s-to-localstorage-%s-cache-host-%s", image.getUuid(), self.getUuid(), hostUuid);
                }

                private void doDownload(final SyncTaskChain chain) {
                    FlowChain fchain = FlowChainBuilder.newShareFlowChain();
                    fchain.setName(String.format("download-image-%s-to-local-storage-%s-cache-host-%s",
                            image.getUuid(), self.getUuid(), hostUuid));
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
                                    amsg.setRequiredHostUuid(hostUuid);
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

                            flow(new Flow() {
                                String __name__ = "allocate-capacity-on-host";

                                @Override
                                public void run(FlowTrigger trigger, Map data) {
                                    reserveCapacityOnHost(hostUuid, image.getActualSize());
                                    trigger.next();
                                }

                                @Override
                                public void rollback(FlowRollback trigger, Map data) {
                                    returnCapacityToHost(hostUuid, image.getActualSize());
                                    trigger.rollback();
                                }
                            });


                            flow(new NoRollbackFlow() {
                                String __name__ = "download";

                                @Override
                                public void run(final FlowTrigger trigger, Map data) {
                                    LocalStorageBackupStorageMediator m = localStorageFactory.getBackupStorageMediator(KVMConstant.KVM_HYPERVISOR_TYPE, backupStorage.getType());
                                    m.downloadBits(getSelfInventory(), backupStorage,
                                            backupStorageInstallPath, primaryStorageInstallPath,
                                            hostUuid, new Completion(trigger) {
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
                    q.add(ImageCacheVO_.installUrl, Op.LIKE, String.format("%%hostUuid://%s%%", hostUuid));
                    String fullPath = q.findValue();
                    if (fullPath == null) {
                        doDownload(chain);
                        return;
                    }

                    CacheInstallPath path = new CacheInstallPath();
                    path.fullPath = fullPath;
                    final String installPath = path.disassemble().installPath;
                    CheckBitsCmd cmd = new CheckBitsCmd();
                    cmd.path = installPath;

                    httpCall(CHECK_BITS_PATH, hostUuid, cmd, CheckBitsRsp.class, new ReturnValueCompletion<CheckBitsRsp>(completion, chain) {
                        @Override
                        public void success(CheckBitsRsp rsp) {
                            if (rsp.existing) {
                                logger.debug(String.format("found image[uuid: %s, name: %s] in the image cache of local primary storage[uuid:%s, installPath: %s]",
                                        image.getUuid(), image.getName(), self.getUuid(), installPath));
                                completion.success(installPath);
                                chain.next();
                                return;
                            }

                            // the image is removed on the host
                            // delete the cache object and re-download it
                            SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
                            q.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, self.getUuid());
                            q.add(ImageCacheVO_.imageUuid, Op.EQ, image.getUuid());
                            q.add(ImageCacheVO_.installUrl, Op.LIKE, String.format("%%hostUuid://%s%%", hostUuid));
                            ImageCacheVO cvo = q.find();

                            ReturnPrimaryStorageCapacityMsg rmsg = new ReturnPrimaryStorageCapacityMsg();
                            rmsg.setDiskSize(cvo.getSize());
                            rmsg.setPrimaryStorageUuid(cvo.getPrimaryStorageUuid());
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, cvo.getPrimaryStorageUuid());
                            bus.send(rmsg);

                            returnCapacityToHost(hostUuid, cvo.getSize());
                            dbf.remove(cvo);

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

    private void createRootVolume(final InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg msg, final ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion) {
        final ImageSpec ispec = msg.getTemplateSpec();
        final ImageInventory image = ispec.getInventory();

        if (!ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType())) {
            createEmptyVolume(msg.getVolume(), msg.getDestHost().getUuid(), new ReturnValueCompletion<String>(completion) {
                @Override
                public void success(String returnValue) {
                    InstantiateVolumeOnPrimaryStorageReply r = new InstantiateVolumeOnPrimaryStorageReply();
                    VolumeInventory vol = msg.getVolume();
                    vol.setInstallPath(returnValue);
                    vol.setFormat(VolumeConstant.VOLUME_FORMAT_QCOW2);
                    r.setVolume(vol);
                    completion.success(r);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });

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
                    String __name__ = "create-template-from-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        installPath = makeRootVolumeInstallUrl(volume);

                        CreateVolumeFromCacheCmd cmd = new CreateVolumeFromCacheCmd();
                        cmd.setInstallUrl(installPath);
                        cmd.setTemplatePathInCache(pathInCache);
                        cmd.setVolumeUuid(volume.getUuid());

                        httpCall(CREATE_VOLUME_FROM_CACHE_PATH, hostUuid, cmd, CreateVolumeFromCacheRsp.class, new ReturnValueCompletion<CreateVolumeFromCacheRsp>(trigger) {
                            @Override
                            public void success(CreateVolumeFromCacheRsp returnValue) {
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

    @Override
    public void deleteBits(final String path, final String hostUuid, final Completion completion) {
        DeleteBitsCmd cmd = new DeleteBitsCmd();
        cmd.setPath(path);
        cmd.setHostUuid(hostUuid);

        httpCall(DELETE_BITS_PATH, hostUuid, cmd, DeleteBitsRsp.class, new ReturnValueCompletion<DeleteBitsRsp>(completion) {
            @Override
            public void success(DeleteBitsRsp returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (!errorCode.isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE)) {
                    completion.fail(errorCode);
                    return;
                }

                GCDeleteBitsContext c = new GCDeleteBitsContext();
                c.setPrimaryStorageUuid(self.getUuid());
                c.setHostUuid(hostUuid);
                c.setInstallPath(path);

                submitDeleteBitsGCJob(c);
                completion.success();
            }
        });
    }

    private void submitDeleteBitsGCJob(GCDeleteBitsContext c) {
        EventBasedGCPersistentContext<GCDeleteBitsContext> ctx = new EventBasedGCPersistentContext<GCDeleteBitsContext>();
        ctx.setContextClass(GCDeleteBitsContext.class);
        ctx.setRunnerClass(GCDeleteBitsRunner.class);
        ctx.setName(String.format("local-storage-delete-%s-%s", self.getUuid(), c.getInstallPath()));
        ctx.setContext(c);

        GCEventTrigger trigger = new GCEventTrigger();
        trigger.setCodeName("local-storage-delete-bits");
        trigger.setEventPath(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH);
        String code = ln(
                "import org.zstack.header.host.HostCanonicalEvents.HostStatusChangedData",
                "import org.zstack.storage.primary.local.GCDeleteBitsContext",
                "HostStatusChangedData d = (HostStatusChangedData) data",
                "GCDeleteBitsContext ctx = (GCDeleteBitsContext) context",
                "return d.hostUuid == ctx.hostUuid && d.newStatus == \"Connected\""
        ).toString();
        trigger.setCode(code);
        ctx.addTrigger(trigger);

        trigger = new GCEventTrigger();
        trigger.setEventPath(HostCanonicalEvents.HOST_DELETED_PATH);
        trigger.setCodeName("local-storage-delete-bits-on-host-deleted");
        code = ln(
                "import org.zstack.header.host.HostCanonicalEvents.HostDeletedData",
                "import org.zstack.storage.primary.local.GCDeleteBitsContext",
                "HostDeletedData d = (HostDeletedData) data",
                "GCDeleteBitsContext ctx = (GCDeleteBitsContext) context",
                "return d.hostUuid == ctx.hostUuid"
        ).toString();
        trigger.setCode(code);
        ctx.addTrigger(trigger);

        trigger = new GCEventTrigger();
        trigger.setEventPath(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_DELETED_PATH);
        trigger.setCodeName("local-storage-delete-bit-on-primary-storage-deleted");
        code = ln(
                "import org.zstack.header.storage.primary.PrimaryStorageCanonicalEvent.PrimaryStorageDeletedData",
                "import org.zstack.storage.primary.local.GCDeleteBitsContext",
                "PrimaryStorageDeletedData d = (PrimaryStorageDeletedData) data",
                "GCDeleteBitsContext ctx = (GCDeleteBitsContext) context",
                "return d.primaryStorageUuid == ctx.primaryStorageUuid"
        ).toString();
        trigger.setCode(code);
        ctx.addTrigger(trigger);

        gcf.schedule(ctx);
    }

    @Override
    void handle(final DeleteVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply> completion) {
        final DeleteVolumeOnPrimaryStorageReply dreply = new DeleteVolumeOnPrimaryStorageReply();
        final String hostUuid = getHostUuidByResourceUuid(msg.getVolume().getUuid(), VolumeVO.class.getSimpleName());
        deleteBits(msg.getVolume().getInstallPath(), hostUuid, new Completion(completion) {
            @Override
            public void success() {
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
    protected void handle(InitPrimaryStorageOnHostConnectedMsg msg, final ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        InitCmd cmd = new InitCmd();
        cmd.setHostUuid(msg.getHostUuid());
        cmd.setPath(self.getUrl());

        httpCall(INIT_PATH, msg.getHostUuid(), cmd, true, AgentResponse.class,
                new ReturnValueCompletion<AgentResponse>(completion) {
                    @Override
                    public void success(AgentResponse rsp) {
                        PhysicalCapacityUsage usage = new PhysicalCapacityUsage();
                        usage.totalPhysicalSize = rsp.getTotalCapacity();
                        usage.availablePhysicalSize = rsp.getAvailableCapacity();
                        completion.success(usage);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    @Override
    void handle(final TakeSnapshotMsg msg, final String hostUuid, final ReturnValueCompletion<TakeSnapshotReply> completion) {
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

                TakeSnapshotOnHypervisorReply treply = (TakeSnapshotOnHypervisorReply) reply;
                sp.setSize(treply.getSize());
                sp.setPrimaryStorageUuid(self.getUuid());
                sp.setPrimaryStorageInstallPath(treply.getSnapshotInstallPath());
                sp.setType(VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());

                TakeSnapshotReply ret = new TakeSnapshotReply();
                ret.setNewVolumeInstallPath(treply.getNewVolumeInstallPath());
                ret.setInventory(sp);

                reserveCapacityOnHost(hostUuid, sp.getSize());
                completion.success(ret);
            }
        });
    }

    @Override
    void handle(final DeleteSnapshotOnPrimaryStorageMsg msg, final String hostUuid, final ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply> completion) {
        final DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
        deleteBits(msg.getSnapshot().getPrimaryStorageInstallPath(), hostUuid, new Completion(completion) {
            @Override
            public void success() {
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

        httpCall(REVERT_SNAPSHOT_PATH, hostUuid, cmd, RevertVolumeFromSnapshotRsp.class, new ReturnValueCompletion<RevertVolumeFromSnapshotRsp>(completion) {
            @Override
            public void success(RevertVolumeFromSnapshotRsp rsp) {
                RevertVolumeFromSnapshotOnPrimaryStorageReply ret = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
                ret.setNewVolumeInstallPath(rsp.getNewVolumeInstallPath());
                completion.success(ret);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg, String hostUuid, final ReturnValueCompletion<ReInitRootVolumeFromTemplateOnPrimaryStorageReply> completion) {
        RevertVolumeFromSnapshotCmd cmd = new RevertVolumeFromSnapshotCmd();
        cmd.setSnapshotInstallPath(makeCachedImageInstallUrlFromImageUuidForTemplate(msg.getVolume().getRootImageUuid()));

        httpCall(REVERT_SNAPSHOT_PATH, hostUuid, cmd, RevertVolumeFromSnapshotRsp.class, new ReturnValueCompletion<RevertVolumeFromSnapshotRsp>(completion) {
            @Override
            public void success(RevertVolumeFromSnapshotRsp rsp) {
                ReInitRootVolumeFromTemplateOnPrimaryStorageReply ret = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();
                ret.setNewVolumeInstallPath(rsp.getNewVolumeInstallPath());
                completion.success(ret);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
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

        final String installPath = ((BackupStorageAskInstallPathReply) br).getInstallPath();

        m.uploadBits(getSelfInventory(), msg.getBackupStorage(), installPath, sp.getPrimaryStorageInstallPath(), hostUuid, new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String installPath) {
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

    @Override
    void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, String hostUuid, final ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        final CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();

        final String installPath = makeDataVolumeInstallUrl(msg.getVolumeUuid());
        VolumeSnapshotInventory sp = msg.getSnapshot();
        MergeSnapshotCmd cmd = new MergeSnapshotCmd();
        cmd.setVolumeUuid(sp.getVolumeUuid());
        cmd.setSnapshotInstallPath(sp.getPrimaryStorageInstallPath());
        cmd.setWorkspaceInstallPath(installPath);

        httpCall(MERGE_SNAPSHOT_PATH, hostUuid, cmd, MergeSnapshotRsp.class, new ReturnValueCompletion<MergeSnapshotRsp>(completion) {
            @Override
            public void success(MergeSnapshotRsp rsp) {
                reply.setActualSize(rsp.actualSize);
                reply.setSize(rsp.size);
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
    void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg, String hostUuid, final ReturnValueCompletion<MergeVolumeSnapshotOnPrimaryStorageReply> completion) {
        boolean offline = true;
        VolumeInventory volume = msg.getTo();
        VolumeSnapshotInventory sp = msg.getFrom();
        if (volume.getVmInstanceUuid() != null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
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

            httpCall(OFFLINE_MERGE_PATH, hostUuid, cmd, OfflineMergeSnapshotRsp.class, new ReturnValueCompletion<OfflineMergeSnapshotRsp>(completion) {
                @Override
                public void success(OfflineMergeSnapshotRsp returnValue) {
                    completion.success(ret);
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
    void handle(LocalStorageCreateEmptyVolumeMsg msg, final ReturnValueCompletion<LocalStorageCreateEmptyVolumeReply> completion) {
        createEmptyVolume(msg.getVolume(), msg.getHostUuid(), msg.getBackingFile(), new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String returnValue) {
                LocalStorageCreateEmptyVolumeReply reply = new LocalStorageCreateEmptyVolumeReply();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(LocalStorageDirectlyDeleteBitsMsg msg, String hostUuid, final ReturnValueCompletion<LocalStorageDirectlyDeleteBitsReply> completion) {
        deleteBits(msg.getPath(), hostUuid, new Completion(completion) {
            @Override
            public void success() {
                completion.success(new LocalStorageDirectlyDeleteBitsReply());
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(CreateTemporaryVolumeFromSnapshotMsg msg, final String hostUuid, final ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply> completion) {
        final String workSpaceInstallPath = makeSnapshotWorkspacePath(msg.getImageUuid());

        VolumeSnapshotInventory sp = msg.getSnapshot();
        MergeSnapshotCmd cmd = new MergeSnapshotCmd();
        cmd.setVolumeUuid(sp.getVolumeUuid());
        cmd.setSnapshotInstallPath(sp.getPrimaryStorageInstallPath());
        cmd.setWorkspaceInstallPath(workSpaceInstallPath);

        httpCall(MERGE_SNAPSHOT_PATH, hostUuid, cmd, MergeSnapshotRsp.class, new ReturnValueCompletion<MergeSnapshotRsp>(completion) {
            @Override
            public void success(MergeSnapshotRsp rsp) {
                CreateTemporaryVolumeFromSnapshotReply reply = new CreateTemporaryVolumeFromSnapshotReply();
                reply.setInstallPath(workSpaceInstallPath);
                reply.setSize(rsp.size);
                reply.setActualSize(rsp.actualSize);
                reply.setHostUuid(hostUuid);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(SyncVolumeSizeOnPrimaryStorageMsg msg, String hostUuid, final ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply> completion) {
        final SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
        GetVolumeSizeCmd cmd = new GetVolumeSizeCmd();
        cmd.installPath = msg.getInstallPath();
        cmd.volumeUuid = msg.getVolumeUuid();
        KvmCommandSender sender = new KvmCommandSender(hostUuid);
        sender.send(cmd, GET_VOLUME_SIZE, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                GetVolumeSizeRsp rsp = wrapper.getResponse(GetVolumeSizeRsp.class);
                return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>() {
            @Override
            public void success(KvmResponseWrapper returnValue) {
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
    void handle(final UploadBitsFromLocalStorageToBackupStorageMsg msg, String hostUuid, final ReturnValueCompletion<UploadBitsFromLocalStorageToBackupStorageReply> completion) {
        final BackupStorageVO bs = dbf.findByUuid(msg.getBackupStorageUuid(), BackupStorageVO.class);
        BackupStorageInventory bsinv = BackupStorageInventory.valueOf(bs);

        LocalStorageBackupStorageMediator m = localStorageFactory.getBackupStorageMediator(KVMConstant.KVM_HYPERVISOR_TYPE, bs.getType());
        m.uploadBits(getSelfInventory(), bsinv, msg.getBackupStorageInstallPath(), msg.getPrimaryStorageInstallPath(),
                hostUuid, new ReturnValueCompletion<String>(completion) {
                    @Override
                    public void success(String installPath) {
                        UploadBitsFromLocalStorageToBackupStorageReply reply = new UploadBitsFromLocalStorageToBackupStorageReply();
                        reply.setBackupStorageInstallPath(installPath);
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to upload template[%s] from local primary storage[uuid: %s] to the backup storage[uuid: %s, path: %s]",
                                msg.getPrimaryStorageInstallPath(), self.getUuid(), bs.getUuid(), msg.getBackupStorageInstallPath()));
                        completion.fail(errorCode);
                    }
                });
    }

    @Override
    void handle(final GetVolumeRootImageUuidFromPrimaryStorageMsg msg, String hostUuid, final ReturnValueCompletion<GetVolumeRootImageUuidFromPrimaryStorageReply> completion) {
        GetVolumeBaseImagePathCmd cmd = new GetVolumeBaseImagePathCmd();
        cmd.installPath = msg.getVolume().getInstallPath();
        cmd.volumeUuid = msg.getVolume().getUuid();
        new KvmCommandSender(hostUuid).send(cmd, GET_BASE_IMAGE_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper w) {
                GetVolumeBaseImagePathRsp rsp = w.getResponse(GetVolumeBaseImagePathRsp.class);
                return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper w) {
                GetVolumeBaseImagePathRsp rsp = w.getResponse(GetVolumeBaseImagePathRsp.class);
                String rootImageUuid = new File(rsp.path).getName().split("\\.")[0];
                GetVolumeRootImageUuidFromPrimaryStorageReply reply = new GetVolumeRootImageUuidFromPrimaryStorageReply();
                reply.setImageUuid(rootImageUuid);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    @MessageSafe
    void handleHypervisorSpecificMessage(LocalStorageHypervisorSpecificMessage msg) {
        bus.dealWithUnknownMessage((Message) msg);
    }

    @Override
    void downloadImageToCache(ImageInventory img, String hostUuid, final ReturnValueCompletion<String> completion) {
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

        BackupStorageInventory bs = BackupStorageInventory.valueOf(dbf.findByUuid(bsUuid, BackupStorageVO.class));
        ImageBackupStorageRefInventory ref = CollectionUtils.find(img.getBackupStorageRefs(), new Function<ImageBackupStorageRefInventory, ImageBackupStorageRefInventory>() {
            @Override
            public ImageBackupStorageRefInventory call(ImageBackupStorageRefInventory arg) {
                return arg.getBackupStorageUuid().equals(bsUuid) ? arg : null;
            }
        });

        final ImageCache cache = new ImageCache();
        cache.image = img;
        cache.hostUuid = hostUuid;
        cache.primaryStorageInstallPath = makeCachedImageInstallUrl(img);
        cache.backupStorage = bs;
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
    void handle(final LocalStorageDeleteImageCacheOnPrimaryStorageMsg msg, String hostUuid, final ReturnValueCompletion<DeleteImageCacheOnPrimaryStorageReply> completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("clean-up-image-cache-on-local-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "ensure-image-is-not-referenced";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        GetQCOW2ReferenceCmd cmd = new GetQCOW2ReferenceCmd();
                        cmd.searchingDir = self.getUrl();
                        cmd.path = msg.getInstallPath();

                        new KvmCommandSender(msg.getHostUuid()).send(cmd, GET_QCOW2_REFERENCE, new KvmCommandFailureChecker() {
                            @Override
                            public ErrorCode getError(KvmResponseWrapper wrapper) {
                                GetQCOW2ReferenceRsp rsp = wrapper.getResponse(GetQCOW2ReferenceRsp.class);
                                return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
                            }
                        }, new ReturnValueCompletion<KvmResponseWrapper>(trigger) {
                            @Override
                            public void success(KvmResponseWrapper w) {
                                GetQCOW2ReferenceRsp rsp = w.getResponse(GetQCOW2ReferenceRsp.class);
                                if (rsp.referencePaths == null || rsp.referencePaths.isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errf.stringToInternalError(String.format("[THIS IS A BUG NEEDED TO BE FIXED RIGHT NOW, PLEASE REPORT TO US ASAP] the image cache file[%s] is still referenced by" +
                                            " below QCOW2 files:\n%s", msg.getInstallPath(), StringUtils.join(rsp.referencePaths, "\n"))));
                                }
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
                        deleteBits(msg.getInstallPath(), msg.getHostUuid(), new Completion(trigger) {
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

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        DeleteImageCacheOnPrimaryStorageReply reply = new DeleteImageCacheOnPrimaryStorageReply();
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
    public List<Flow> createMigrateBitsFlow(final MigrateBitsStruct struct) {
        List<Flow> flows = new ArrayList<Flow>();

        SimpleQuery<KVMHostVO> q = dbf.createQuery(KVMHostVO.class);
        q.select(KVMHostVO_.managementIp, KVMHostVO_.username, KVMHostVO_.password);
        q.add(KVMHostVO_.uuid, Op.EQ, struct.getDestHostUuid());
        Tuple t = q.findTuple();

        final String mgmtIp = t.get(0, String.class);
        final String username = t.get(1, String.class);
        final String password = t.get(2, String.class);

        class Context {
            GetMd5Rsp getMd5Rsp;
            String backingFilePath;
            Long backingFileSize;
            String backingFileMd5;
            ImageVO image;
        }
        final Context context = new Context();

        if (VolumeType.Root.toString().equals(struct.getVolume().getType())) {
            final boolean downloadImage;
            String imageUuid = struct.getVolume().getRootImageUuid();
            if (imageUuid != null) {
                context.image = dbf.findByUuid(imageUuid, ImageVO.class);
                downloadImage = !(context.image == null || context.image.getMediaType() == ImageMediaType.ISO || context.image.getStatus() == ImageStatus.Deleted);
            } else {
                downloadImage = false;
            }

            if (downloadImage) {
                flows.add(new NoRollbackFlow() {
                    String __name__ = "download-base-image-to-dst-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        downloadImageToCache(ImageInventory.valueOf(context.image), struct.getDestHostUuid(), new ReturnValueCompletion<String>(trigger) {
                            @Override
                            public void success(String returnValue) {
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
                flows.add(new NoRollbackFlow() {
                    String __name__ = "get-backing-file-of-root-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        GetBackingFileCmd cmd = new GetBackingFileCmd();
                        cmd.path = struct.getVolume().getInstallPath();
                        cmd.volumeUuid = struct.getVolume().getUuid();
                        httpCall(GET_BACKING_FILE_PATH, struct.getSrcHostUuid(), cmd, GetBackingFileRsp.class, new ReturnValueCompletion<GetBackingFileRsp>() {
                            @Override
                            public void success(GetBackingFileRsp rsp) {
                                context.backingFilePath = rsp.backingFilePath;
                                context.backingFileSize = rsp.size;
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
                    String __name__ = "reserve-capacity-for-backing-file-on-dst-host";

                    boolean s = false;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (context.backingFilePath == null) {
                            logger.debug("no backing file, skip this flow");
                            trigger.next();
                            return;
                        }

                        reserveCapacityOnHost(struct.getDestHostUuid(), context.backingFileSize);
                        s = true;
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (s) {
                            returnCapacityToHost(struct.getDestHostUuid(), context.backingFileSize);
                        }
                        trigger.rollback();
                    }
                });

                flows.add(new NoRollbackFlow() {
                    String __name__ = "get-md5-of-backing-file";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (context.backingFilePath == null) {
                            logger.debug("no backing file, skip this flow");
                            trigger.next();
                            return;
                        }

                        GetMd5Cmd cmd = new GetMd5Cmd();
                        GetMd5TO to = new GetMd5TO();
                        to.resourceUuid = "backing-file";
                        to.path = context.backingFilePath;
                        cmd.md5s = list(to);

                        httpCall(GET_MD5_PATH, struct.getSrcHostUuid(), cmd, false, GetMd5Rsp.class, new ReturnValueCompletion<GetMd5Rsp>(trigger) {
                            @Override
                            public void success(GetMd5Rsp rsp) {
                                context.backingFileMd5 = rsp.md5s.get(0).md5;
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
                    String __name__ = "migrate-backing-file";

                    boolean s = false;

                    private void migrate(final FlowTrigger trigger) {
                        // sync here for migrating multiple volumes having the same backing file
                        thdf.chainSubmit(new ChainTask(trigger) {
                            @Override
                            public String getSyncSignature() {
                                return String.format("migrate-backing-file-%s-to-host-%s", context.backingFilePath, struct.getDestHostUuid());
                            }

                            @Override
                            public void run(final SyncTaskChain chain) {
                                final CopyBitsFromRemoteCmd cmd = new CopyBitsFromRemoteCmd();
                                cmd.dstIp = mgmtIp;
                                cmd.dstUsername = username;
                                cmd.dstPassword = password;
                                cmd.paths = list(context.backingFilePath);

                                httpCall(LocalStorageKvmMigrateVmFlow.COPY_TO_REMOTE_BITS_PATH, struct.getSrcHostUuid(), cmd, false,
                                        AgentResponse.class, new ReturnValueCompletion<AgentResponse>(trigger, chain) {
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
                        if (context.backingFilePath == null) {
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
                                            context.backingFilePath, struct.getDestHostUuid()));
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
                        cmd.path = context.backingFilePath;

                        httpCall(CHECK_BITS_PATH, struct.getDestHostUuid(), cmd, CheckBitsRsp.class, new ReturnValueCompletion<CheckBitsRsp>(completion) {
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
                            deleteBits(context.backingFilePath, struct.getDestHostUuid(), new Completion() {
                                @Override
                                public void success() {
                                    // ignore
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    //TODO
                                    logger.warn(String.format("failed to delete %s on the host[uuid:%s], %s",
                                            struct.getDestHostUuid(), context.backingFilePath, errorCode));
                                }
                            });
                        }

                        trigger.rollback();
                    }
                });

                flows.add(new NoRollbackFlow() {
                    String __name__ = "check-md5-of-backing-file-on-dst-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (context.backingFilePath == null) {
                            logger.debug("no backing file, skip this flow");
                            trigger.next();
                            return;
                        }

                        Md5TO to = new Md5TO();
                        to.resourceUuid = "backing-file";
                        to.path = context.backingFilePath;
                        to.md5 = context.backingFileMd5;

                        CheckMd5sumCmd cmd = new CheckMd5sumCmd();
                        cmd.md5s = list(to);

                        httpCall(CHECK_MD5_PATH, struct.getDestHostUuid(), cmd, false, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(trigger) {
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
        }

        flows.add(new NoRollbackFlow() {
            String __name__ = "get-md5-on-src-host";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                GetMd5Cmd cmd = new GetMd5Cmd();
                cmd.md5s = CollectionUtils.transformToList(struct.getInfos(), new Function<GetMd5TO, ResourceInfo>() {
                    @Override
                    public GetMd5TO call(ResourceInfo arg) {
                        GetMd5TO to = new GetMd5TO();
                        to.path = arg.getPath();
                        to.resourceUuid = arg.getResourceRef().getResourceUuid();
                        return to;
                    }
                });

                httpCall(GET_MD5_PATH, struct.getSrcHostUuid(), cmd, false, GetMd5Rsp.class, new ReturnValueCompletion<GetMd5Rsp>(trigger) {
                    @Override
                    public void success(GetMd5Rsp rsp) {
                        context.getMd5Rsp = rsp;
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
            String __name__ = "migrate-bits-to-dst-host";

            List<String> migrated;

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                final CopyBitsFromRemoteCmd cmd = new CopyBitsFromRemoteCmd();
                cmd.dstIp = mgmtIp;
                cmd.dstUsername = username;
                cmd.dstPassword = password;
                cmd.paths = CollectionUtils.transformToList(struct.getInfos(), new Function<String, ResourceInfo>() {
                    @Override
                    public String call(ResourceInfo arg) {
                        return arg.getPath();
                    }
                });

                httpCall(LocalStorageKvmMigrateVmFlow.COPY_TO_REMOTE_BITS_PATH, struct.getSrcHostUuid(), cmd, false,
                        AgentResponse.class, new ReturnValueCompletion<AgentResponse>(trigger) {
                            @Override
                            public void success(AgentResponse rsp) {
                                migrated = cmd.paths;
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
                if (migrated != null) {
                    new Runnable() {
                        @Override
                        @AsyncThread
                        public void run() {
                            doDelete(migrated.iterator());
                        }

                        private void doDelete(final Iterator<String> it) {
                            if (!it.hasNext()) {
                                return;
                            }

                            final String path = it.next();
                            deleteBits(path, struct.getDestHostUuid(), new Completion() {
                                @Override
                                public void success() {
                                    doDelete(it);
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    //TODO
                                    logger.warn(String.format("failed to delete %s on the host[uuid:%s], %s",
                                            path, struct.getDestHostUuid(), errorCode));
                                    doDelete(it);
                                }
                            });
                        }

                    }.run();
                }

                trigger.rollback();
            }
        });

        flows.add(new NoRollbackFlow() {
            String __name__ = "check-md5-on-dst";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                CheckMd5sumCmd cmd = new CheckMd5sumCmd();
                cmd.md5s = context.getMd5Rsp.md5s;
                httpCall(CHECK_MD5_PATH, struct.getDestHostUuid(), cmd, false, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(trigger) {
                    @Override
                    public void success(AgentResponse rsp) {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        });

        return flows;
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
            long avail = 0;
            long pt = 0;
            long pa = 0;
            long su = 0;
            for (LocalStorageHostRefVO ref : refs) {
                total += ref.getTotalCapacity();
                avail += ref.getAvailableCapacity();
                pt += ref.getTotalPhysicalCapacity();
                pa += ref.getAvailablePhysicalCapacity();
                su += ref.getSystemUsedCapacity();
            }

            // after detaching, total capacity on those hosts should be deducted
            // from both total and available capacity of the primary storage
            decreaseCapacity(total, avail, pt, pa, su);
        }
        completion.success();
    }

    @Override
    public void attachHook(String clusterUuid, final Completion completion) {
        // get all hosts of cluster
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        final List<String> hostUuids = q.listValue();

        if (hostUuids.isEmpty()) {
            completion.success();
            return;
        }

        // make init msg for each host
        List<KVMHostAsyncHttpCallMsg> msgs = CollectionUtils.transformToList(hostUuids,
                new Function<KVMHostAsyncHttpCallMsg, String>() {
                    @Override
                    public KVMHostAsyncHttpCallMsg call(String arg) {
                        InitCmd cmd = new InitCmd();
                        cmd.path = self.getUrl();
                        cmd.hostUuid = arg;

                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setCommand(cmd);
                        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
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
                long systemUsed = 0;
                List<LocalStorageHostRefVO> refs = new ArrayList<>();

                for (MessageReply reply : replies) {
                    String hostUuid = hostUuids.get(replies.indexOf(reply));
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("cannot get the physical capacity of local storage on the host[uuid:%s], %s",
                                hostUuid, reply.getError()));
                        continue;
                    }

                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    AgentResponse rsp = r.toResponse(AgentResponse.class);
                    if (!rsp.isSuccess()) {
                        logger.warn(String.format("cannot get the physical capacity of local storage on the host[uuid:%s], %s",
                                hostUuid, rsp.getError()));
                        continue;
                    }
                    {
                        SimpleQuery<LocalStorageHostRefVO> sq = dbf.createQuery(LocalStorageHostRefVO.class);
                        sq.add(LocalStorageHostRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
                        sq.add(LocalStorageHostRefVO_.hostUuid, Op.EQ, hostUuid);
                        if (sq.isExists()) {
                            logger.debug(String.format("host[uuid :%s] is already in the local primary storage[uuid: %s]",
                                    hostUuid, self.getUuid()));
                            continue;
                        }
                    }

                    total += rsp.getTotalCapacity();
                    avail += rsp.getAvailableCapacity();
                    systemUsed += (rsp.getTotalCapacity() - rsp.getAvailableCapacity());

                    LocalStorageHostRefVO ref = new LocalStorageHostRefVO();
                    ref.setPrimaryStorageUuid(self.getUuid());
                    ref.setHostUuid(hostUuid);
                    ref.setAvailablePhysicalCapacity(rsp.getAvailableCapacity());
                    ref.setAvailableCapacity(rsp.getAvailableCapacity());
                    ref.setTotalCapacity(rsp.getTotalCapacity());
                    ref.setTotalPhysicalCapacity(rsp.getTotalCapacity());
                    ref.setSystemUsedCapacity(rsp.getTotalCapacity() - rsp.getAvailableCapacity());
                    refs.add(ref);

                }

                dbf.persistCollection(refs);

                increaseCapacity(total, avail, total, avail, systemUsed);

                completion.success();
            }
        });
    }

    @Override
    protected void handle(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
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

                    long requiredSize = ratioMgr.calculateByRatio(self.getUuid(), msg.getVolumeInventory().getSize());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        reserveCapacityOnHost(ref.getHostUuid(), requiredSize);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        returnCapacityToHost(ref.getHostUuid(), requiredSize);
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

                        httpCall(CREATE_TEMPLATE_FROM_VOLUME, ref.getHostUuid(), cmd, false,
                                CreateTemplateFromVolumeRsp.class,
                                new ReturnValueCompletion<CreateTemplateFromVolumeRsp>(trigger) {
                                    @Override
                                    public void success(CreateTemplateFromVolumeRsp rsp) {
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
                        m.uploadBits(getSelfInventory(), BackupStorageInventory.valueOf(bsvo), backupStorageInstallPath, temporaryTemplatePath, ref.getHostUuid(), new ReturnValueCompletion<String>(trigger) {
                            @Override
                            public void success(String installPath) {
                                backupStorageInstallPath = installPath;
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
