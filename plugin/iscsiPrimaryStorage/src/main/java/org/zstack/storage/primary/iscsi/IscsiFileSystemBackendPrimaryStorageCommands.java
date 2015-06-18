package org.zstack.storage.primary.iscsi;

import org.zstack.core.validation.ConditionalValidation;

/**
 * Created by frank on 4/19/2015.
 */
public interface IscsiFileSystemBackendPrimaryStorageCommands {
    public static class AgentResponse implements ConditionalValidation {
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

        @Override
        public boolean needValidation() {
            return success;
        }
    }

    public static class AgentCapacityResponse extends  AgentResponse {
        private Long availableCapacity;
        private Long totalCapacity;

        public Long getAvailableCapacity() {
            return availableCapacity;
        }

        public void setAvailableCapacity(Long availableCapacity) {
            this.availableCapacity = availableCapacity;
        }

        public Long getTotalCapacity() {
            return totalCapacity;
        }

        public void setTotalCapacity(Long totalCapacity) {
            this.totalCapacity = totalCapacity;
        }
    }

    public static class AgentCommand {
    }

    public static class DownloadBitsFromSftpBackupStorageCmd extends AgentCommand {
        private String sshKey;
        private String hostname;
        private String backupStorageInstallPath;
        private String primaryStorageInstallPath;

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

    public static class DownloadBitsFromSftpBackupStorageRsp extends AgentCapacityResponse {
    }

    public static class CheckBitsExistenceCmd extends AgentCommand {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class CheckBitsExistenceRsp extends AgentResponse  {
        private boolean isExisting;

        public boolean isExisting() {
            return isExisting;
        }

        public void setExisting(boolean isExisting) {
            this.isExisting = isExisting;
        }
    }

    public static class CreateRootVolumeFromTemplateCmd extends AgentCommand {
        private String templatePathInCache;
        private String installPath;
        private String volumeUuid;
        private String chapUsername;
        private String chapPassword;

        public String getChapUsername() {
            return chapUsername;
        }

        public void setChapUsername(String chapUsername) {
            this.chapUsername = chapUsername;
        }

        public String getChapPassword() {
            return chapPassword;
        }

        public void setChapPassword(String chapPassword) {
            this.chapPassword = chapPassword;
        }

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public String getTemplatePathInCache() {
            return templatePathInCache;
        }
        public void setTemplatePathInCache(String templatePathInCache) {
            this.templatePathInCache = templatePathInCache;
        }
        public String getInstallPath() {
            return installPath;
        }
        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class CreateRootVolumeFromTemplateRsp extends AgentCapacityResponse {
        private String iscsiPath;

        public String getIscsiPath() {
            return iscsiPath;
        }

        public void setIscsiPath(String iscsiPath) {
            this.iscsiPath = iscsiPath;
        }
    }

    public static class DeleteBitsCmd extends AgentCommand {
        private String installPath;
        private String volumeUuid;
        private String iscsiPath;

        public String getIscsiPath() {
            return iscsiPath;
        }

        public void setIscsiPath(String iscsiPath) {
            this.iscsiPath = iscsiPath;
        }

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class DeleteBitsRsp extends AgentCapacityResponse {
    }

    public static class CreateEmptyVolumeCmd extends AgentCommand {
        private String installPath;
        private long size;
        private String volumeUuid;
        private String chapUsername;
        private String chapPassword;

        public String getChapUsername() {
            return chapUsername;
        }

        public void setChapUsername(String chapUsername) {
            this.chapUsername = chapUsername;
        }

        public String getChapPassword() {
            return chapPassword;
        }

        public void setChapPassword(String chapPassword) {
            this.chapPassword = chapPassword;
        }

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public String getInstallPath() {
            return installPath;
        }
        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
        public long getSize() {
            return size;
        }
        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class CreateEmptyVolumeRsp extends AgentCapacityResponse {
        private String iscsiPath;

        public String getIscsiPath() {
            return iscsiPath;
        }

        public void setIscsiPath(String iscsiPath) {
            this.iscsiPath = iscsiPath;
        }
    }

    public static class InitCmd extends AgentCommand {
        private String rootFolderPath;

        public String getRootFolderPath() {
            return rootFolderPath;
        }

        public void setRootFolderPath(String rootFolderPath) {
            this.rootFolderPath = rootFolderPath;
        }
    }

    public static class InitRsp extends AgentCapacityResponse {

    }

    public static class UploadToSftpCmd extends AgentCommand {
        private String primaryStorageInstallPath;
        private String backupStorageInstallPath;
        private String backupStorageHostName;
        private String backupStorageSshKey;

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

    public static class UploadToSftpRsp extends AgentCapacityResponse {
    }

    public static class CreateIscsiTargetCmd extends AgentCommand {
        private String volumeUuid;
        private String installPath;
        private String chapUsername;
        private String chapPassword;

        public String getChapUsername() {
            return chapUsername;
        }

        public void setChapUsername(String chapUsername) {
            this.chapUsername = chapUsername;
        }

        public String getChapPassword() {
            return chapPassword;
        }

        public void setChapPassword(String chapPassword) {
            this.chapPassword = chapPassword;
        }

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class DeleteIscsiTargetCmd extends AgentCommand {
        private String target;
        private String uuid;

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class DeleteIscsiTargetRsp extends AgentResponse {
    }

    public static class CreateIscsiTargetRsp extends AgentResponse {
        private String target;
        private int lun;

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public int getLun() {
            return lun;
        }

        public void setLun(int lun) {
            this.lun = lun;
        }
    }

    public static class CreateSubVolumeCmd extends AgentCommand {
        private String src;
        private String dst;

        public String getSrc() {
            return src;
        }

        public void setSrc(String src) {
            this.src = src;
        }

        public String getDst() {
            return dst;
        }

        public void setDst(String dst) {
            this.dst = dst;
        }
    }

    public static class CreateSubVolumeRsp extends AgentCapacityResponse {
        private String path;
        private long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class GetCapacityCmd extends AgentCommand {
    }
}
