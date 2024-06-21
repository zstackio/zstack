package org.zstack.sdk;



public class ModelServiceInventory  {

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

    public java.lang.String yaml;
    public void setYaml(java.lang.String yaml) {
        this.yaml = yaml;
    }
    public java.lang.String getYaml() {
        return this.yaml;
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

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public java.util.List modelServiceRefs;
    public void setModelServiceRefs(java.util.List modelServiceRefs) {
        this.modelServiceRefs = modelServiceRefs;
    }
    public java.util.List getModelServiceRefs() {
        return this.modelServiceRefs;
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
