package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetModelProfileAccessInventory;
import org.zstack.sdk.ZsDatasetModelProfileSourceInventory;

public class ZsDatasetModelProfileInventory  {

    public java.lang.String id;
    public void setId(java.lang.String id) {
        this.id = id;
    }
    public java.lang.String getId() {
        return this.id;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String provider;
    public void setProvider(java.lang.String provider) {
        this.provider = provider;
    }
    public java.lang.String getProvider() {
        return this.provider;
    }

    public java.lang.String modelId;
    public void setModelId(java.lang.String modelId) {
        this.modelId = modelId;
    }
    public java.lang.String getModelId() {
        return this.modelId;
    }

    public java.lang.String capability;
    public void setCapability(java.lang.String capability) {
        this.capability = capability;
    }
    public java.lang.String getCapability() {
        return this.capability;
    }

    public java.lang.String endpoint;
    public void setEndpoint(java.lang.String endpoint) {
        this.endpoint = endpoint;
    }
    public java.lang.String getEndpoint() {
        return this.endpoint;
    }

    public java.lang.String status;
    public void setStatus(java.lang.String status) {
        this.status = status;
    }
    public java.lang.String getStatus() {
        return this.status;
    }

    public boolean hasApiKey;
    public void setHasApiKey(boolean hasApiKey) {
        this.hasApiKey = hasApiKey;
    }
    public boolean getHasApiKey() {
        return this.hasApiKey;
    }

    public java.lang.String createAt;
    public void setCreateAt(java.lang.String createAt) {
        this.createAt = createAt;
    }
    public java.lang.String getCreateAt() {
        return this.createAt;
    }

    public java.lang.String updateAt;
    public void setUpdateAt(java.lang.String updateAt) {
        this.updateAt = updateAt;
    }
    public java.lang.String getUpdateAt() {
        return this.updateAt;
    }

    public ZsDatasetModelProfileAccessInventory access;
    public void setAccess(ZsDatasetModelProfileAccessInventory access) {
        this.access = access;
    }
    public ZsDatasetModelProfileAccessInventory getAccess() {
        return this.access;
    }

    public ZsDatasetModelProfileSourceInventory source;
    public void setSource(ZsDatasetModelProfileSourceInventory source) {
        this.source = source;
    }
    public ZsDatasetModelProfileSourceInventory getSource() {
        return this.source;
    }

    public long selectedBySpaces;
    public void setSelectedBySpaces(long selectedBySpaces) {
        this.selectedBySpaces = selectedBySpaces;
    }
    public long getSelectedBySpaces() {
        return this.selectedBySpaces;
    }

}
