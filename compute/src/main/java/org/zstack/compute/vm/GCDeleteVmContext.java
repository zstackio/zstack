package org.zstack.compute.vm;

import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by xing5 on 2016/3/24.
 */
public class GCDeleteVmContext {
    private String vmUuid;
    private String hostUuid;
    private VmInstanceInventory inventory;
    private String triggerHostStatus;

    public String getTriggerHostStatus() {
        return triggerHostStatus;
    }

    public void setTriggerHostStatus(String triggerHostStatus) {
        this.triggerHostStatus = triggerHostStatus;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
