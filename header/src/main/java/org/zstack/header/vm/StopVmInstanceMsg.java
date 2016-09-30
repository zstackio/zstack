package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class StopVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private boolean gcOnFailure;

    public boolean isGcOnFailure() {
        return gcOnFailure;
    }

    public void setGcOnFailure(boolean gcOnFailure) {
        this.gcOnFailure = gcOnFailure;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
