package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class RebootVmOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private VmInstanceInventory vmInventory;
    private String bootDevice;

    public String getBootDevice() {
        return bootDevice;
    }

    public void setBootDevice(String bootDevice) {
        this.bootDevice = bootDevice;
    }

    public VmInstanceInventory getVmInventory() {
        return vmInventory;
    }

    public void setVmInventory(VmInstanceInventory vmInventory) {
        this.vmInventory = vmInventory;
    }

    @Override
    public String getHostUuid() {
        return vmInventory.getHostUuid();
    }
}
