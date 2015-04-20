package org.zstack.storage.primary.iscsi;

import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;

/**
 * Created by frank on 4/19/2015.
 */
public abstract class APIAddIScsiPrimaryStorageMsg extends APIAddPrimaryStorageMsg {
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
}
