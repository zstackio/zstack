package org.zstack.header.host;

import java.io.Serializable;

public class HostParam implements Serializable {
    private String hostUuid;

    private String physicalInterface;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getPhysicalInterface() {
        return physicalInterface;
    }

    public void setPhysicalInterface(String physicalInterface) {
        this.physicalInterface = physicalInterface;
    }
}
