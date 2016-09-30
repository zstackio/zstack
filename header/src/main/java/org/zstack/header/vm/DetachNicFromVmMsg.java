package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 7/18/2015.
 */
public class DetachNicFromVmMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String vmNicUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }
}
