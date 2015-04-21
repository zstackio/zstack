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
        private String installUrl;

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
    }

    public static class CreateRootVolumeFromTemplateRsp extends AgentCapacityResponse {
    }

    public static class DeleteBitsCmd extends AgentCommand {
        private String installPath;

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
        private String installUrl;
        private long size;

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
    }

    public static class CreateEmptyVolumeRsp extends AgentCapacityResponse {
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
}
