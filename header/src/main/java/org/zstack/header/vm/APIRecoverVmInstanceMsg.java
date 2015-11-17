package org.zstack.header.vm;

import org.zstack.header.message.APIMessage;

/**
 * Created by frank on 11/12/2015.
 */
public class APIRecoverVmInstanceMsg extends APIMessage implements VmInstanceMessage {
    private String uuid;
    private String vmInstanceUuid;

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

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
