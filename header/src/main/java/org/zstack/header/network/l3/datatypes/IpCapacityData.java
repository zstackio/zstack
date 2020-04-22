package org.zstack.header.network.l3.datatypes;

/**
 * Created by Qi Le on 2020/4/22
 */
public class IpCapacityData {
    private String resourceUuid;

    private long totalCapacity;

    private long availableCapacity;

    private long usedIpAddressNumber;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
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

    public long getUsedIpAddressNumber() {
        return usedIpAddressNumber;
    }

    public void setUsedIpAddressNumber(long usedIpAddressNumber) {
        this.usedIpAddressNumber = usedIpAddressNumber;
    }
}
