package org.zstack.header.vm;

import org.zstack.header.message.APIMessage;

/**
 * Created by frank on 11/12/2015.
 */
public class APIExpungeVmInstanceMsg extends APIMessage implements VmInstanceMessage {
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
