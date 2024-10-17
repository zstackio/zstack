package org.zstack.sdk;

import org.zstack.sdk.ModelCenterCapacityInventory;

public class ModelCenterInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String url;
    public void setUrl(java.lang.String url) {
        this.url = url;
    }
    public java.lang.String getUrl() {
        return this.url;
    }

    public java.lang.String status;
    public void setStatus(java.lang.String status) {
        this.status = status;
    }
    public java.lang.String getStatus() {
        return this.status;
    }

    public java.lang.String parameters;
    public void setParameters(java.lang.String parameters) {
        this.parameters = parameters;
    }
    public java.lang.String getParameters() {
        return this.parameters;
    }

    public java.lang.String managementIp;
    public void setManagementIp(java.lang.String managementIp) {
        this.managementIp = managementIp;
    }
    public java.lang.String getManagementIp() {
        return this.managementIp;
    }

    public int managementPort;
    public void setManagementPort(int managementPort) {
        this.managementPort = managementPort;
    }
    public int getManagementPort() {
        return this.managementPort;
    }

    public java.lang.String storageNetworkUuid;
    public void setStorageNetworkUuid(java.lang.String storageNetworkUuid) {
        this.storageNetworkUuid = storageNetworkUuid;
    }
    public java.lang.String getStorageNetworkUuid() {
        return this.storageNetworkUuid;
    }

    public java.lang.String serviceNetworkUuid;
    public void setServiceNetworkUuid(java.lang.String serviceNetworkUuid) {
        this.serviceNetworkUuid = serviceNetworkUuid;
    }
    public java.lang.String getServiceNetworkUuid() {
        return this.serviceNetworkUuid;
    }

    public java.lang.String containerRegistry;
    public void setContainerRegistry(java.lang.String containerRegistry) {
        this.containerRegistry = containerRegistry;
    }
    public java.lang.String getContainerRegistry() {
        return this.containerRegistry;
    }

    public java.lang.String containerStorageNetwork;
    public void setContainerStorageNetwork(java.lang.String containerStorageNetwork) {
        this.containerStorageNetwork = containerStorageNetwork;
    }
    public java.lang.String getContainerStorageNetwork() {
        return this.containerStorageNetwork;
    }

    public java.lang.String containerNetwork;
    public void setContainerNetwork(java.lang.String containerNetwork) {
        this.containerNetwork = containerNetwork;
    }
    public java.lang.String getContainerNetwork() {
        return this.containerNetwork;
    }

    public ModelCenterCapacityInventory capacity;
    public void setCapacity(ModelCenterCapacityInventory capacity) {
        this.capacity = capacity;
    }
    public ModelCenterCapacityInventory getCapacity() {
        return this.capacity;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

}
