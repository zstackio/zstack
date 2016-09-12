package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class StopVmOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private VmInstanceInventory vmInventory;
    private boolean force;

    public VmInstanceInventory getVmInventory() {
        return vmInventory;
    }

    public void setVmInventory(VmInstanceInventory vmInventory) {
        this.vmInventory = vmInventory;
    }

    public boolean getForce(){
        return force;
    }

    public void setForce(boolean force){
        this.force = force;
    }
    @Override
    public String getHostUuid() {
        return vmInventory.getHostUuid();
    }
}
