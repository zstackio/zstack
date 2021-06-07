package org.zstack.header.vm;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

public class UpdateVmOSMsg extends NeedReplyMessage implements VmInstanceMessage{

    private String uuid;
    @NoLogging
    private String password;
    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public void setVmInstanceUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
