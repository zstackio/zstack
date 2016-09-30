package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class RebootVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }
}
