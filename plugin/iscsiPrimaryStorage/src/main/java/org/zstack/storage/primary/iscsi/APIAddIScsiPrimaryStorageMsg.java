package org.zstack.storage.primary.iscsi;

import org.zstack.header.message.APIParam;
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;

/**
 * Created by frank on 4/19/2015.
 */
public abstract class APIAddIScsiPrimaryStorageMsg extends APIAddPrimaryStorageMsg {
    @APIParam(maxLength = 255, required = false)
    private String chapUsername;
    @APIParam(maxLength = 255, required = false)
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
