package org.zstack.storage.primary.local;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by frank on 10/15/2015.
 */
@RestResponse(allTo = "inventories")
public class APIGetLocalStorageHostDiskCapacityReply extends APIReply {
    public static class HostDiskCapacity {
        private String hostUuid;
        private long totalCapacity;
        private long availableCapacity;
        private long totalPhysicalCapacity;
        private long availablePhysicalCapacity;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

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

    private List<HostDiskCapacity> inventories;

    public List<HostDiskCapacity> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostDiskCapacity> inventories) {
        this.inventories = inventories;
    }
}
