package org.zstack.sdk;



public class ContainerImageInventory extends org.zstack.sdk.ImageInventory {

    public java.lang.String endpointUuid;
    public void setEndpointUuid(java.lang.String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }
    public java.lang.String getEndpointUuid() {
        return this.endpointUuid;
    }

    public java.lang.String imageTag;
    public void setImageTag(java.lang.String imageTag) {
        this.imageTag = imageTag;
    }
    public java.lang.String getImageTag() {
        return this.imageTag;
    }

    public java.lang.String registryUrl;
    public void setRegistryUrl(java.lang.String registryUrl) {
        this.registryUrl = registryUrl;
    }
    public java.lang.String getRegistryUrl() {
        return this.registryUrl;
    }

}
