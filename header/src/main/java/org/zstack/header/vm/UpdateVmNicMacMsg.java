package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

public class UpdateVmNicMacMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String vmNicUuid;
    private String mac;
    private boolean reconcile;

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

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public boolean isReconcile() {
        return reconcile;
    }

    public void setReconcile(boolean reconcile) {
        this.reconcile = reconcile;
    }
}
