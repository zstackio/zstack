package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by shixin.ruan on 10/17/2018.
 */
public class DetachIpAddressFromVmNicMsg extends NeedReplyMessage {
    private String vmNicUuid;
    private String usedIpUuid;

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }
}
