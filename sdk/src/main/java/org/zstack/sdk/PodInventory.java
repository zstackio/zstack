package org.zstack.sdk;



public class PodInventory extends org.zstack.sdk.VmInstanceInventory {

    public java.lang.String status;
    public void setStatus(java.lang.String status) {
        this.status = status;
    }
    public java.lang.String getStatus() {
        return this.status;
    }

    public java.lang.String namespace;
    public void setNamespace(java.lang.String namespace) {
        this.namespace = namespace;
    }
    public java.lang.String getNamespace() {
        return this.namespace;
    }

    public java.lang.Long clusterId;
    public void setClusterId(java.lang.Long clusterId) {
        this.clusterId = clusterId;
    }
    public java.lang.Long getClusterId() {
        return this.clusterId;
    }

    public java.lang.Integer requestCpu;
    public void setRequestCpu(java.lang.Integer requestCpu) {
        this.requestCpu = requestCpu;
    }
    public java.lang.Integer getRequestCpu() {
        return this.requestCpu;
    }

    public java.lang.Long requestMemory;
    public void setRequestMemory(java.lang.Long requestMemory) {
        this.requestMemory = requestMemory;
    }
    public java.lang.Long getRequestMemory() {
        return this.requestMemory;
    }

}
