package org.zstack.storage.primary.local;

import org.zstack.header.message.APIReply;

/**
 * Created by frank on 10/15/2015.
 */
public class APIGetLocalStorageHostDiskCapacityReply extends APIReply {
    private long totalCapacity;
    private long availableCapacity;
    private long totalPhysicalCapacity;
    private long availablePhysicalCapacity;

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public long getTotalPhysicalCapacity() {
        return totalPhysicalCapacity;
    }

    public void setTotalPhysicalCapacity(long totalPhysicalCapacity) {
        this.totalPhysicalCapacity = totalPhysicalCapacity;
    }

    public long getAvailablePhysicalCapacity() {
        return availablePhysicalCapacity;
    }

    public void setAvailablePhysicalCapacity(long availablePhysicalCapacity) {
        this.availablePhysicalCapacity = availablePhysicalCapacity;
    }
}
