package org.zstack.sdk;



public class ModelServiceInstanceGroupInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String modelServiceUuid;
    public void setModelServiceUuid(java.lang.String modelServiceUuid) {
        this.modelServiceUuid = modelServiceUuid;
    }
    public java.lang.String getModelServiceUuid() {
        return this.modelServiceUuid;
    }

    public java.util.List instances;
    public void setInstances(java.util.List instances) {
        this.instances = instances;
    }
    public java.util.List getInstances() {
        return this.instances;
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
