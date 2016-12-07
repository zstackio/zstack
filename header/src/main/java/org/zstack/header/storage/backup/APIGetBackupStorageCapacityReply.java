package org.zstack.header.storage.backup;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetBackupStorageCapacityReply extends APIReply {
    private long totalCapacity;
    private long availableCapacity;

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
}
