package org.zstack.storage.primary.iscsi;

import org.zstack.header.message.APIParam;
import org.zstack.header.storage.primary.APIUpdatePrimaryStorageMsg;

/**
 * Created by frank on 6/16/2015.
 */
public class APIUpdateIscsiFileSystemBackendPrimaryStorageMsg extends APIUpdatePrimaryStorageMsg {
    @APIParam(maxLength = 255, required = false)
    private String chapUsername;
    @APIParam(maxLength = 255, required = false)
    private String chapPassword;
    @APIParam(maxLength = 255, required = false)
    private String sshUsername;
    @APIParam(maxLength = 255, required = false)
    private String sshPassword;

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
}
