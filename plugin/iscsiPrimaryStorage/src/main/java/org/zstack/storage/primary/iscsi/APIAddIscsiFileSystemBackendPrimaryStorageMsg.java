package org.zstack.storage.primary.iscsi;

import org.zstack.header.message.APIParam;

/**
 * Created by frank on 4/19/2015.
 */
public class APIAddIscsiFileSystemBackendPrimaryStorageMsg extends APIAddIScsiPrimaryStorageMsg {
    @APIParam(maxLength = 255)
    private String hostname;
    @APIParam(maxLength = 255)
    private String sshUsername;
    @APIParam(maxLength = 255)
    private String sshPassword;
    @APIParam(maxLength = 255, required = false)
    private String filesystemType;

    @Override
    public String getType() {
        return IscsiPrimaryStorageConstants.ISCSI_FILE_SYSTEM_BACKEND_PRIMARY_STORAGE_TYPE;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }

    public String getFilesystemType() {
        return filesystemType;
    }

    public void setFilesystemType(String filesystemType) {
        this.filesystemType = filesystemType;
    }
}
