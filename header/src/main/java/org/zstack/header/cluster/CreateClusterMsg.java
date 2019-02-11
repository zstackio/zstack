package org.zstack.header.cluster;

import org.zstack.header.message.NeedReplyMessage;

public class CreateClusterMsg extends NeedReplyMessage implements CreateClusterMessage {
    private String zoneUuid;
    private String clusterName;
    private String description;
    private String hypervisorType;
    private String type;
    private String resourceUuid;

    @Override
    public String getZoneUuid() {
        return zoneUuid;
    }

    @Override
    public String getClusterName() {
        return clusterName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getHypervisorType() {
        return hypervisorType;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
}
