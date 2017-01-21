package org.zstack.storage.primary.nfs;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.core.validation.Validation;
import org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeMsg;
import org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeMsg;
import org.zstack.header.image.APICreateRootVolumeTemplateFromVolumeSnapshotMsg;
import org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotMsg;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.APIExpungeVmInstanceMsg;
import org.zstack.header.volume.APICreateDataVolumeFromVolumeSnapshotMsg;
import org.zstack.kvm.KVMAgentCommands.AgentCommand;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;

import java.util.List;


public class NfsPrimaryStorageKVMBackendCommands {
    public static class NfsPrimaryStorageAgentResponse extends AgentResponse {
        private Long totalCapacity;
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

    public static class NfsPrimaryStorageAgentCommand extends AgentCommand {
        private String uuid;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class MountCmd extends NfsPrimaryStorageAgentCommand {
        private String url;
        private String mountPath;
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
        private String mountPath;
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

    @ApiTimeout(apiClasses = {
            APICreateRootVolumeTemplateFromRootVolumeMsg.class,
            APICreateDataVolumeTemplateFromVolumeMsg.class
    })
    public static class CreateTemplateFromVolumeCmd extends NfsPrimaryStorageAgentCommand {
        private String installPath;
        private String rootVolumePath;

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
    }
    public static class CreateTemplateFromVolumeRsp extends NfsPrimaryStorageAgentResponse {
    }

    @ApiTimeout(apiClasses = {APICreateVmInstanceMsg.class})
    public static class DownloadBitsFromSftpBackupStorageCmd extends NfsPrimaryStorageAgentCommand {
        private String sshKey;
        private String hostname;
        private String username;
        private int sshPort;
        private String backupStorageInstallPath;
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
        private String installPath;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class CheckIsBitsExistingRsp extends NfsPrimaryStorageAgentResponse {
        private boolean existing;

        public boolean isExisting() {
            return existing;
        }

        public void setExisting(boolean existing) {
            this.existing = existing;
        }
    }

    @ApiTimeout(apiClasses = {APICreateVmInstanceMsg.class})
    public static class CreateRootVolumeFromTemplateCmd extends NfsPrimaryStorageAgentCommand {
        private String templatePathInCache;
        private long timeout;
        private String installUrl;
        private String accountUuid;
        private String name;
        private String volumeUuid;
        
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
    
    public static class CreateRootVolumeFromTemplateResponse extends NfsPrimaryStorageAgentResponse {
    }
    
    public static class CreateEmptyVolumeCmd extends NfsPrimaryStorageAgentCommand {
        private String installUrl;
        private long size;
        private String accountUuid;
        private String hypervisorType;
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
    public static class CreateEmptyVolumeResponse extends NfsPrimaryStorageAgentResponse {
    }

    @ApiTimeout(apiClasses = {APICreateDataVolumeFromVolumeSnapshotMsg.class, APIExpungeVmInstanceMsg.class})
    public static class DeleteCmd extends NfsPrimaryStorageAgentCommand {
        private boolean isFolder;
        private String installPath;

        public boolean isFolder() {
            return isFolder;
        }

        public void setFolder(boolean isFolder) {
            this.isFolder = isFolder;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class DeleteResponse extends NfsPrimaryStorageAgentResponse {
    }
    
    public static class RevertVolumeFromSnapshotCmd extends NfsPrimaryStorageAgentCommand {
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
        private String newVolumeInstallPath;

        public String getNewVolumeInstallPath() {
            return newVolumeInstallPath;
        }

        public void setNewVolumeInstallPath(String newVolumeInstallPath) {
            this.newVolumeInstallPath = newVolumeInstallPath;
        }
    }

    public static class UploadToSftpCmd extends NfsPrimaryStorageAgentCommand {
        private String primaryStorageInstallPath;
        private String backupStorageInstallPath;
        private String backupStorageHostName;
        private String backupStorageUserName;
        private String backupStorageSshKey;
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

    @ApiTimeout(apiClasses = {
            APICreateDataVolumeFromVolumeSnapshotMsg.class,
            APICreateRootVolumeTemplateFromVolumeSnapshotMsg.class
    })
    public static class MergeSnapshotCmd extends NfsPrimaryStorageAgentCommand {
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

    public static class MergeSnapshotResponse extends NfsPrimaryStorageAgentResponse {
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

    @ApiTimeout(apiClasses = {
            APICreateDataVolumeFromVolumeSnapshotMsg.class,
            APICreateRootVolumeTemplateFromVolumeSnapshotMsg.class
    })
    public static class RebaseAndMergeSnapshotsCmd extends NfsPrimaryStorageAgentCommand {
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

    public static class RebaseAndMergeSnapshotsResponse extends NfsPrimaryStorageAgentResponse {
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

    public static class MoveBitsCmd extends NfsPrimaryStorageAgentCommand {
        private String srcPath;
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

    @ApiTimeout(apiClasses = {APIDeleteVolumeSnapshotMsg.class})
    public static class OfflineMergeSnapshotCmd extends NfsPrimaryStorageAgentCommand {
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

    public static class OfflineMergeSnapshotRsp extends NfsPrimaryStorageAgentResponse {
    }

    public static class RemountCmd extends NfsPrimaryStorageAgentCommand {
        public String url;
        public String mountPath;
        public String options;
    }

    public static class GetVolumeActualSizeCmd extends NfsPrimaryStorageAgentCommand {
        public String volumeUuid;
        public String installPath;
    }

    public static class GetVolumeActualSizeRsp extends NfsPrimaryStorageAgentResponse {
        public long actualSize;
        public long size;
    }

    public static class PingCmd extends NfsPrimaryStorageAgentCommand {
    }

    public static class GetVolumeBaseImagePathCmd extends NfsPrimaryStorageAgentCommand {
        public String volumeUUid;
        public String installPath;
    }

    public static class GetVolumeBaseImagePathRsp extends NfsPrimaryStorageAgentResponse {
        public String path;
    }

    public static class UpdateMountPointCmd extends NfsPrimaryStorageAgentCommand {
        public String oldMountPoint;
        public String newMountPoint;
        public String mountPath;
        public String options;
    }

    public static class UpdateMountPointRsp extends NfsPrimaryStorageAgentResponse {
    }
}
