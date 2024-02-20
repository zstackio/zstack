package org.zstack.sdk;



public class SlbVmInstanceInventory extends org.zstack.sdk.VirtualRouterVmInventory {

    public java.lang.String slbGroupUuid;
    public void setSlbGroupUuid(java.lang.String slbGroupUuid) {
        this.slbGroupUuid = slbGroupUuid;
    }
    public java.lang.String getSlbGroupUuid() {
        return this.slbGroupUuid;
    }

    public java.util.List configTasks;
    public void setConfigTasks(java.util.List configTasks) {
        this.configTasks = configTasks;
    }
    public java.util.List getConfigTasks() {
        return this.configTasks;
    }

    public long configVersion;
    public void setConfigVersion(long configVersion) {
        this.configVersion = configVersion;
    }
    public long getConfigVersion() {
        return this.configVersion;
    }

}
