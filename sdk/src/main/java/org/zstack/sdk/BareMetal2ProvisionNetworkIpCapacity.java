package org.zstack.sdk;



public class BareMetal2ProvisionNetworkIpCapacity  {

    public java.lang.String networkUuid;
    public void setNetworkUuid(java.lang.String networkUuid) {
        this.networkUuid = networkUuid;
    }
    public java.lang.String getNetworkUuid() {
        return this.networkUuid;
    }

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

    public long gatewayUsedIpNumber;
    public void setGatewayUsedIpNumber(long gatewayUsedIpNumber) {
        this.gatewayUsedIpNumber = gatewayUsedIpNumber;
    }
    public long getGatewayUsedIpNumber() {
        return this.gatewayUsedIpNumber;
    }

    public long instanceUsedIpNumber;
    public void setInstanceUsedIpNumber(long instanceUsedIpNumber) {
        this.instanceUsedIpNumber = instanceUsedIpNumber;
    }
    public long getInstanceUsedIpNumber() {
        return this.instanceUsedIpNumber;
    }

}
