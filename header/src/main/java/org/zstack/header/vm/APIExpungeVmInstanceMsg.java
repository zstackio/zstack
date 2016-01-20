package org.zstack.header.vm;

import org.zstack.header.message.APIMessage;

/**
 * Created by frank on 11/12/2015.
 */
public class APIExpungeVmInstanceMsg extends APIMessage implements VmInstanceMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }
}
