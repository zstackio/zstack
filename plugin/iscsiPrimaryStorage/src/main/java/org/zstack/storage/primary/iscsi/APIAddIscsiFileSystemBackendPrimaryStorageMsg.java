package org.zstack.storage.primary.iscsi;

import org.zstack.header.message.APIParam;

/**
 * Created by frank on 4/19/2015.
 */
public class APIAddIscsiFileSystemBackendPrimaryStorageMsg extends APIAddIScsiPrimaryStorageMsg {
    @APIParam
    private String hostname;
    @APIParam
    private String sshUsername;
    @APIParam
    private String sshPassword;
    @APIParam(maxLength = 2048)
    private String url;
    private String filesystemType;

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

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    public String getFilesystemType() {
        return filesystemType;
    }

    public void setFilesystemType(String filesystemType) {
        this.filesystemType = filesystemType;
    }
}
