package org.zstack.storage.primary.nfs;

import org.zstack.core.upgrade.GrayVersion;
import org.zstack.header.HasThreadContext;
import org.zstack.header.core.validation.Validation;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMAgentCommands.AgentCommand;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;

import java.util.List;
import java.util.Map;


public class NfsPrimaryStorageKVMBackendCommands {
    public static class NfsPrimaryStorageAgentResponse extends AgentResponse {
        @GrayVersion(value = "5.0.0")
        private Long totalCapacity;
        @GrayVersion(value = "5.0.0")
        private Long availableCapacity;

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

    public static class NfsPrimaryStorageAgentCommand extends KVMAgentCommands.PrimaryStorageCommand {
        @GrayVersion(value = "5.0.0")
        private String uuid;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
            this.primaryStorageUuid = uuid;
        }
    }

    public static class DownloadBitsFromKVMHostRsp extends AgentResponse {
        @GrayVersion(value = "5.0.0")
        public String format;
    }

    public static class DownloadBitsFromKVMHostCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String hostname;
        @GrayVersion(value = "5.0.0")
        public String username;
        @GrayVersion(value = "5.0.0")
        public String sshKey;
        @GrayVersion(value = "5.0.0")
        public int sshPort;
        // it's file path on kvm host actually
        @GrayVersion(value = "5.0.0")
        public String backupStorageInstallPath;
        @GrayVersion(value = "5.0.0")
        public String primaryStorageInstallPath;
        @GrayVersion(value = "5.0.0")
        public Long bandWidth;
        @GrayVersion(value = "5.0.0")
        public String identificationCode;
    }

    public static class CancelDownloadBitsFromKVMHostCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String primaryStorageInstallPath;
    }

    public static class GetDownloadBitsFromKVMHostProgressCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public List<String> volumePaths;
    }

    public static class GetDownloadBitsFromKVMHostProgressRsp extends AgentResponse {
        @GrayVersion(value = "5.0.0")
        public long totalSize;
    }

    public static class MountCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        private String url;
        @GrayVersion(value = "5.0.0")
        private String mountPath;
        @GrayVersion(value = "5.0.0")
        private String options;

        public String getOptions() {
            return options;
        }

        public void setOptions(String options) {
            this.options = options;
        }

        public String getUrl() {
            return url;
        }
        public void setUrl(String url) {
            this.url = url;
        }
        public String getMountPath() {
            return mountPath;
        }
        public void setMountPath(String mountPath) {
            this.mountPath = mountPath;
        }
    }

    
    public static class MountAgentResponse extends NfsPrimaryStorageAgentResponse {
    }
    
    public static class GetCapacityCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        private String mountPath;

        public String getMountPath() {
            return mountPath;
        }

        public void setMountPath(String mountPath) {
            this.mountPath = mountPath;
        }
    }
    
    public static class GetCapacityResponse extends NfsPrimaryStorageAgentResponse {
    }
    
    public static class UnmountCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        private String mountPath;
        @GrayVersion(value = "5.0.0")
        private String url;
        
        public String getUrl() {
            return url;
        }
        public void setUrl(String url) {
            this.url = url;
        }
        public String getMountPath() {
            return mountPath;
        }
        public void setMountPath(String mountPath) {
            this.mountPath = mountPath;
        }
    }
    public static class UnmountResponse extends NfsPrimaryStorageAgentResponse {
    }

    public static class CreateTemplateFromVolumeCmd extends NfsPrimaryStorageAgentCommand implements HasThreadContext{
        @GrayVersion(value = "5.0.0")
        private String installPath;
        @GrayVersion(value = "5.0.0")
        private String rootVolumePath;
        @GrayVersion(value = "5.0.0")
        private boolean incremental;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
        public String getRootVolumePath() {
            return rootVolumePath;
        }
        public void setVolumePath(String rootVolumePath) {
            this.rootVolumePath = rootVolumePath;
        }

        public void setIncremental(boolean incremental) {
            this.incremental = incremental;
        }

        public boolean isIncremental() {
            return incremental;
        }
    }
    public static class CreateTemplateFromVolumeRsp extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        private long size;
        @GrayVersion(value = "5.0.0")
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

    public static class EstimateTemplateSizeCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        private String volumePath;

        public String getVolumePath() {
            return volumePath;
        }

        public void setVolumePath(String volumePath) {
            this.volumePath = volumePath;
        }
    }

    public static class EstimateTemplateSizeRsp extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        private long size;
        @GrayVersion(value = "5.0.0")
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

    public static class DownloadBitsFromSftpBackupStorageCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        private String sshKey;
        @GrayVersion(value = "5.0.0")
        private String hostname;
        @GrayVersion(value = "5.0.0")
        private String username;
        @GrayVersion(value = "5.0.0")
        private int sshPort;
        @GrayVersion(value = "5.0.0")
        private String backupStorageInstallPath;
        @GrayVersion(value = "5.0.0")
        private String primaryStorageInstallPath;

        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }
        public int getSshPort() {
            return sshPort;
        }
        public void setSshPort(int sshPort) {
            this.sshPort = sshPort;
        }
        public String getSshKey() {
            return sshKey;
        }

        public void setSshKey(String sshKey) {
            this.sshKey = sshKey;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }
    }

    public static class DownloadBitsFromSftpBackupStorageResponse extends NfsPrimaryStorageAgentResponse {
    }

    public static class CheckIsBitsExistingCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        private String installPath;
        @GrayVersion(value = "5.0.0")
        private String hostUuid;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class CheckIsBitsExistingRsp extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        private boolean existing;

        public boolean isExisting() {
            return existing;
        }

        public void setExisting(boolean existing) {
            this.existing = existing;
        }
    }

    public static class CreateFolderCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        private String installUrl;

        public String getInstallUrl() {
            return installUrl;
        }

        public void setInstallUrl(String installUrl) {
            this.installUrl = installUrl;
        }
    }

    public abstract static class CreateVolumeCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        private String installUrl;
        @GrayVersion(value = "5.0.0")
        private String accountUuid;
        @GrayVersion(value = "5.0.0")
        private String hypervisorType;
        @GrayVersion(value = "5.0.0")
        private String name;
        @GrayVersion(value = "5.0.0")
        private String volumeUuid;

        public String getInstallUrl() {
            return installUrl;
        }
        public void setInstallUrl(String installUrl) {
            this.installUrl = installUrl;
        }
        public String getAccountUuid() {
            return accountUuid;
        }
        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }
        public String getHypervisorType() {
            return hypervisorType;
        }
        public void setHypervisorType(String hypervisorType) {
            this.hypervisorType = hypervisorType;
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
        public void setVolumeUuid(String uuid) {
            this.volumeUuid = uuid;
        }

    }

    public static class CreateRootVolumeFromTemplateCmd extends CreateVolumeCmd {
        @GrayVersion(value = "5.0.0")
        private String templatePathInCache;
        @GrayVersion(value = "5.0.0")
        private long timeout;
        @GrayVersion(value = "5.0.0")
        private long virtualSize;
        
        public long getTimeout() {
            return timeout;
        }
        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
        public String getTemplatePathInCache() {
            return templatePathInCache;
        }
        public void setTemplatePathInCache(String templatePathInCache) {
            this.templatePathInCache = templatePathInCache;
        }
        public long getVirtualSize() {
            return virtualSize;
        }
        public void setVirtualSize(long virtualSize) {
            this.virtualSize = virtualSize;
        }
    }
    
    public static class CreateRootVolumeFromTemplateResponse extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        public Long actualSize;
        @GrayVersion(value = "5.0.0")
        public Long size;
    }

    public static class CreateVolumeWithBackingCmd extends CreateVolumeCmd {
        @GrayVersion(value = "5.0.0")
        private String templatePathInCache;

        public String getTemplatePathInCache() {
            return templatePathInCache;
        }
        public void setTemplatePathInCache(String templatePathInCache) {
            this.templatePathInCache = templatePathInCache;
        }
    }

    public static class CreateVolumeWithBackingRsp extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        private long size;
        @GrayVersion(value = "5.0.0")
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

    }

    public static class CreateEmptyVolumeCmd extends CreateVolumeCmd {
        @GrayVersion(value = "5.0.0")
        private long size;
        @GrayVersion(value = "5.0.0")
        private boolean withoutVolume;

        public long getSize() {
            return size;
        }
        public void setSize(long size) {
            this.size = size;
        }

        public boolean isWithoutVolume() {
            return withoutVolume;
        }

        public void setWithoutVolume(boolean withoutVolume) {
            this.withoutVolume = withoutVolume;
        }
    }
    public static class CreateEmptyVolumeResponse extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        public Long actualSize;
        @GrayVersion(value = "5.0.0")
        public Long size;
    }

    public static class DeleteCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        private boolean folder;
        @GrayVersion(value = "5.0.0")
        private String installPath;

        public boolean isFolder() {
            return folder;
        }

        public void setFolder(boolean isFolder) {
            this.folder = isFolder;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class DeleteResponse extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        public boolean inUse;
    }

    public static class UnlinkBitsCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        public String installPath;
        @GrayVersion(value = "5.0.0")
        public boolean onlyLinkedFile = true;
    }

    public static class UnlinkBitsRsp extends NfsPrimaryStorageAgentResponse {
    }

    public static class ListDirectionCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class ListDirectionResponse extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        private List<String> paths;

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths;
        }
    }
    
    public static class RevertVolumeFromSnapshotCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        private String snapshotInstallPath;

        public String getSnapshotInstallPath() {
            return snapshotInstallPath;
        }

        public void setSnapshotInstallPath(String snapshotInstallPath) {
            this.snapshotInstallPath = snapshotInstallPath;
        }
    }

    public static class RevertVolumeFromSnapshotResponse extends NfsPrimaryStorageAgentResponse {
        @Validation
        @GrayVersion(value = "5.0.0")
        private String newVolumeInstallPath;

        @Validation
        @GrayVersion(value = "5.0.0")
        private long size;

        public String getNewVolumeInstallPath() {
            return newVolumeInstallPath;
        }

        public void setNewVolumeInstallPath(String newVolumeInstallPath) {
            this.newVolumeInstallPath = newVolumeInstallPath;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class ReInitImageCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        private String imagePath;
        @GrayVersion(value = "5.0.0")
        private String volumePath;

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getVolumePath() {
            return volumePath;
        }

        public void setVolumePath(String volumePath) {
            this.volumePath = volumePath;
        }
    }

    public static class ReInitImageRsp extends NfsPrimaryStorageAgentResponse {
        @Validation
        @GrayVersion(value = "5.0.0")
        private String newVolumeInstallPath;

        public String getNewVolumeInstallPath() {
            return newVolumeInstallPath;
        }

        public void setNewVolumeInstallPath(String newVolumeInstallPath) {
            this.newVolumeInstallPath = newVolumeInstallPath;
        }
    }

    public static class UploadToSftpCmd extends NfsPrimaryStorageAgentCommand implements HasThreadContext{
        @GrayVersion(value = "5.0.0")
        private String primaryStorageInstallPath;
        @GrayVersion(value = "5.0.0")
        private String backupStorageInstallPath;
        @GrayVersion(value = "5.0.0")
        private String backupStorageHostName;
        @GrayVersion(value = "5.0.0")
        private String backupStorageUserName;
        @GrayVersion(value = "5.0.0")
        private String backupStorageSshKey;
        @GrayVersion(value = "5.0.0")
        private int backupStorageSshPort;
        public String getBackupStorageUserName() {
            return backupStorageUserName;
        }
        public void setBackupStorageUserName(String backupStorageUserName) {
            this.backupStorageUserName = backupStorageUserName;
        }
        public void setBackupStorageSshPort(int backupStorageSshPort) {
            this.backupStorageSshPort = backupStorageSshPort;
        }
        public int getBackupStorageSshPort() {
            return backupStorageSshPort;
        }
        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }

        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }

        public String getBackupStorageHostName() {
            return backupStorageHostName;
        }

        public void setBackupStorageHostName(String backupStorageHostName) {
            this.backupStorageHostName = backupStorageHostName;
        }

        public String getBackupStorageSshKey() {
            return backupStorageSshKey;
        }

        public void setBackupStorageSshKey(String backupStorageSshKey) {
            this.backupStorageSshKey = backupStorageSshKey;
        }
    }

    public static class UploadToSftpResponse extends NfsPrimaryStorageAgentResponse {
    }

    public static class MergeSnapshotCmd extends NfsPrimaryStorageAgentCommand implements HasThreadContext {
        @GrayVersion(value = "5.0.0")
        private String volumeUuid;
        @GrayVersion(value = "5.0.0")
        private String snapshotInstallPath;
        @GrayVersion(value = "5.0.0")
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

    public static class MergeSnapshotResponse extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        private long size;
        @GrayVersion(value = "5.0.0")
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

    public static class RebaseAndMergeSnapshotsCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        private String volumeUuid;
        @GrayVersion(value = "5.0.0")
        private List<String> snapshotInstallPaths;
        @GrayVersion(value = "5.0.0")
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

    public static class RebaseAndMergeSnapshotsResponse extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        private long size;
        @GrayVersion(value = "5.0.0")
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

    public static class MoveBitsCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        private String srcPath;
        @GrayVersion(value = "5.0.0")
        private String destPath;

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

    public static class MoveBitsRsp extends NfsPrimaryStorageAgentResponse {
    }

    public static class OfflineMergeSnapshotCmd extends NfsPrimaryStorageAgentCommand implements HasThreadContext {
        @GrayVersion(value = "5.0.0")
        private String srcPath;
        @GrayVersion(value = "5.0.0")
        private String destPath;
        @GrayVersion(value = "5.0.0")
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

    public static class OfflineMergeSnapshotRsp extends NfsPrimaryStorageAgentResponse {
    }

    public static class RemountCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        public String url;
        @GrayVersion(value = "5.0.0")
        public String mountPath;
        @GrayVersion(value = "5.0.0")
        public String options;
    }

    public static class GetVolumeActualSizeCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        public String volumeUuid;
        @GrayVersion(value = "5.0.0")
        public String installPath;
    }

    public static class GetBatchVolumeActualSizeCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        public Map<String, String> volumeUuidInstallPaths;
    }

    public static class GetVolumeActualSizeRsp extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        public long actualSize;
        @GrayVersion(value = "5.0.0")
        public long size;
    }

    public static class GetBatchVolumeActualSizeRsp extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        public Map<String, Long> actualSizes;
    }

    public static class PingCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        public String mountPath;
        @GrayVersion(value = "5.0.0")
        public String url;
    }

    /**
     * volumeInstallDir contains all snapshots and base volume file,
     * their backing file in imageCacheDir will be identified as the base image.
     * This takes into consideration multiple snapshot chains and chain-based image.
     */
    public static class GetVolumeBaseImagePathCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        public String volumeUuid;
        @GrayVersion(value = "5.0.0")
        public String volumeInstallDir;
        @GrayVersion(value = "5.0.0")
        public String imageCacheDir;
    }

    public static class GetVolumeBaseImagePathRsp extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        public String path;
    }

    public static class GetBackingChainCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        public String volumeUuid;
        @GrayVersion(value = "5.0.0")
        public String installPath;
    }

    public static class GetBackingChainRsp extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        public List<String> backingChain;
        @GrayVersion(value = "5.0.0")
        public long totalSize;
    }

    public static class UpdateMountPointCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        public String oldMountPoint;
        @GrayVersion(value = "5.0.0")
        public String newMountPoint;
        @GrayVersion(value = "5.0.0")
        public String mountPath;
        @GrayVersion(value = "5.0.0")
        public String options;
    }

    public static class UpdateMountPointRsp extends NfsPrimaryStorageAgentResponse {
    }

    public static class NfsToNfsMigrateBitsCmd extends NfsPrimaryStorageAgentCommand implements HasThreadContext {
        @GrayVersion(value = "5.0.0")
        public String srcFolderPath;
        @GrayVersion(value = "5.0.0")
        public String dstFolderPath;
        @GrayVersion(value = "5.0.0")
        public List<String> filtPaths;
        @GrayVersion(value = "5.0.0")
        public String independentPath;
        @GrayVersion(value = "5.0.0")
        public boolean isMounted = false;
        @GrayVersion(value = "5.0.0")
        public String url;
        @GrayVersion(value = "5.0.0")
        public String options;
        @GrayVersion(value = "5.0.0")
        public String mountPath;
        @GrayVersion(value = "5.0.0")
        public String volumeInstallPath;
        @GrayVersion(value = "5.0.0")
        public String srcPrimaryStorageUuid;
    }

    public static class NfsToNfsMigrateBitsRsp extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        public Map<String, Long> dstFilesActualSize;
    }

    public static class NfsRebaseVolumeBackingFileCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        public String srcPsMountPath;
        @GrayVersion(value = "5.0.0")
        public String dstPsMountPath;
        @GrayVersion(value = "5.0.0")
        public String dstVolumeFolderPath;
        @GrayVersion(value = "5.0.0")
        public String dstImageCacheTemplateFolderPath;
    }

    public static class NfsRebaseVolumeBackingFileRsp extends NfsPrimaryStorageAgentResponse {

    }

    public static class LinkVolumeNewDirCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        public String volumeUuid;
        @GrayVersion(value = "5.0.0")
        public String srcDir;
        @GrayVersion(value = "5.0.0")
        public String dstDir;
    }

    public static class LinkVolumeNewDirRsp extends NfsPrimaryStorageAgentResponse {
    }

    public static class GetQcow2HashValueCmd extends NfsPrimaryStorageAgentCommand {
        @GrayVersion(value = "5.0.0")
        private String hostUuid;
        @GrayVersion(value = "5.0.0")
        private String installPath;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class GetQcow2HashValueRsp extends NfsPrimaryStorageAgentResponse {
        @GrayVersion(value = "5.0.0")
        private String hashValue;

        public String getHashValue() {
            return hashValue;
        }

        public void setHashValue(String hashValue) {
            this.hashValue = hashValue;
        }
    }
}
