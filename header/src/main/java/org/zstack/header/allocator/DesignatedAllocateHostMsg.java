package org.zstack.header.allocator;

import org.zstack.header.vm.VmInstanceMessage;

public class DesignatedAllocateHostMsg extends AllocateHostMsg implements VmInstanceMessage {
    private String zoneUuid;
    private String clusterUuid;
    private String hostUuid;

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return getVmInstance().getUuid();
    }
}
