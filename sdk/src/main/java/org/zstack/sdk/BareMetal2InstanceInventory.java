package org.zstack.sdk;

import org.zstack.sdk.BareMetal2InstanceProvisionNicInventory;

public class BareMetal2InstanceInventory extends org.zstack.sdk.VmInstanceInventory {

    public java.lang.String chassisUuid;
    public void setChassisUuid(java.lang.String chassisUuid) {
        this.chassisUuid = chassisUuid;
    }
    public java.lang.String getChassisUuid() {
        return this.chassisUuid;
    }

    public java.lang.String lastChassisUuid;
    public void setLastChassisUuid(java.lang.String lastChassisUuid) {
        this.lastChassisUuid = lastChassisUuid;
    }
    public java.lang.String getLastChassisUuid() {
        return this.lastChassisUuid;
    }

    public java.lang.String gatewayUuid;
    public void setGatewayUuid(java.lang.String gatewayUuid) {
        this.gatewayUuid = gatewayUuid;
    }
    public java.lang.String getGatewayUuid() {
        return this.gatewayUuid;
    }

    public java.lang.String lastGatewayUuid;
    public void setLastGatewayUuid(java.lang.String lastGatewayUuid) {
        this.lastGatewayUuid = lastGatewayUuid;
    }
    public java.lang.String getLastGatewayUuid() {
        return this.lastGatewayUuid;
    }

    public java.lang.String chassisOfferingUuid;
    public void setChassisOfferingUuid(java.lang.String chassisOfferingUuid) {
        this.chassisOfferingUuid = chassisOfferingUuid;
    }
    public java.lang.String getChassisOfferingUuid() {
        return this.chassisOfferingUuid;
    }

    public java.lang.String gatewayAllocatorStrategy;
    public void setGatewayAllocatorStrategy(java.lang.String gatewayAllocatorStrategy) {
        this.gatewayAllocatorStrategy = gatewayAllocatorStrategy;
    }
    public java.lang.String getGatewayAllocatorStrategy() {
        return this.gatewayAllocatorStrategy;
    }

    public java.lang.String status;
    public void setStatus(java.lang.String status) {
        this.status = status;
    }
    public java.lang.String getStatus() {
        return this.status;
    }

    public java.lang.String provisionType;
    public void setProvisionType(java.lang.String provisionType) {
        this.provisionType = provisionType;
    }
    public java.lang.String getProvisionType() {
        return this.provisionType;
    }

    public java.lang.String agentVersion;
    public void setAgentVersion(java.lang.String agentVersion) {
        this.agentVersion = agentVersion;
    }
    public java.lang.String getAgentVersion() {
        return this.agentVersion;
    }

    public boolean isLatestAgent;
    public void setIsLatestAgent(boolean isLatestAgent) {
        this.isLatestAgent = isLatestAgent;
    }
    public boolean getIsLatestAgent() {
        return this.isLatestAgent;
    }

    public BareMetal2InstanceProvisionNicInventory provisionNic;
    public void setProvisionNic(BareMetal2InstanceProvisionNicInventory provisionNic) {
        this.provisionNic = provisionNic;
    }
    public BareMetal2InstanceProvisionNicInventory getProvisionNic() {
        return this.provisionNic;
    }

}
