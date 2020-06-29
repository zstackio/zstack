package org.zstack.sdk;



public class GetIpAddressCapacityResult {
    public long totalCapacity;
    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
    public long getTotalCapacity() {
        return this.totalCapacity;
    }

    public long availableCapacity;
    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }
    public long getAvailableCapacity() {
        return this.availableCapacity;
    }

    public long usedIpAddressNumber;
    public void setUsedIpAddressNumber(long usedIpAddressNumber) {
        this.usedIpAddressNumber = usedIpAddressNumber;
    }
    public long getUsedIpAddressNumber() {
        return this.usedIpAddressNumber;
    }

    public long ipv6TotalCapacity;
    public void setIpv6TotalCapacity(long ipv6TotalCapacity) {
        this.ipv6TotalCapacity = ipv6TotalCapacity;
    }
    public long getIpv6TotalCapacity() {
        return this.ipv6TotalCapacity;
    }

    public long ipv6AvailableCapacity;
    public void setIpv6AvailableCapacity(long ipv6AvailableCapacity) {
        this.ipv6AvailableCapacity = ipv6AvailableCapacity;
    }
    public long getIpv6AvailableCapacity() {
        return this.ipv6AvailableCapacity;
    }

    public long ipv6UsedIpAddressNumber;
    public void setIpv6UsedIpAddressNumber(long ipv6UsedIpAddressNumber) {
        this.ipv6UsedIpAddressNumber = ipv6UsedIpAddressNumber;
    }
    public long getIpv6UsedIpAddressNumber() {
        return this.ipv6UsedIpAddressNumber;
    }

    public java.util.List capacityData;
    public void setCapacityData(java.util.List capacityData) {
        this.capacityData = capacityData;
    }
    public java.util.List getCapacityData() {
        return this.capacityData;
    }

    public java.lang.String resourceType;
    public void setResourceType(java.lang.String resourceType) {
        this.resourceType = resourceType;
    }
    public java.lang.String getResourceType() {
        return this.resourceType;
    }

}
