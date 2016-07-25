package org.zstack.storage.backup.sftp;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.image.APIAddImageMsg;

public class SftpBackupStorageCommands {
    public static class AgentCommand {
    }
    public static class AgentResponse {
        private boolean success = true;
        private String error;
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
    }
    
    public static class PingCmd extends AgentCommand {
    }
    public static class PingResponse extends AgentResponse {
        private String uuid;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }
    
    public static class ConnectCmd extends AgentCommand {
        private String uuid;
        private String storagePath;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getStoragePath() {
            return storagePath;
        }
        public void setStoragePath(String storagePath) {
            this.storagePath = storagePath;
        }
    }
    public static class ConnectResponse extends AgentResponse {
    }

    @ApiTimeout(apiClasses = {APIAddImageMsg.class})
    public static class DownloadCmd extends AgentCommand {
        private String uuid;
        private String installPath;
        private String url;
        private long timeout;
        private String urlScheme;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public long getTimeout() {
            return timeout;
        }
        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
        public String getUrlScheme() {
            return urlScheme;
        }
        public void setUrlScheme(String urlScheme) {
            this.urlScheme = urlScheme;
        }
    }
    public static class DownloadResponse extends AgentResponse {
        public String md5Sum;
        public String format;
        public long size;
        public long actualSize;

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }

        public String getMd5Sum() {
            return md5Sum;
        }
        public void setMd5Sum(String md5Sum) {
            this.md5Sum = md5Sum;
        }
        public long getSize() {
            return size;
        }
        public void setSize(long size) {
            this.size = size;
        }
    }
    
    public static class DeleteCmd extends AgentCommand {
        private String installUrl;
        public String getInstallUrl() {
            return installUrl;
        }

        public void setInstallUrl(String installUrl) {
            this.installUrl = installUrl;
        }
    }
    public static class DeleteResponse extends AgentResponse {
    }
    
    public static class GetSshKeyCommand extends AgentCommand {
    }
    public static class GetSshKeyResponse extends AgentResponse {
        private String sshKey;

        public String getSshKey() {
            return sshKey;
        }

        public void setSshKey(String sshKey) {
            this.sshKey = sshKey;
        }
    }

    public static class GetImageSizeCmd extends AgentCommand {
        public String imageUuid;
        public String installPath;
    }

    public static class GetImageSizeRsp extends AgentResponse {
        public long size;
        public long actualSize;
    }
}
