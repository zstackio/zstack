package org.zstack.sdk;

import org.zstack.sdk.SlbVmInstanceConfigTaskInventory;

public class SlbVmInstanceInventory extends org.zstack.sdk.VirtualRouterVmInventory {

    public java.lang.String slbGroupUuid;
    public void setSlbGroupUuid(java.lang.String slbGroupUuid) {
        this.slbGroupUuid = slbGroupUuid;
    }
    public java.lang.String getSlbGroupUuid() {
        return this.slbGroupUuid;
    }

    public SlbVmInstanceConfigTaskInventory configTask;
    public void setConfigTask(SlbVmInstanceConfigTaskInventory configTask) {
        this.configTask = configTask;
    }
    public SlbVmInstanceConfigTaskInventory getConfigTask() {
        return this.configTask;
    }

}
