package org.zstack.sdk;



public class SlbGroupInventory  {

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

    public java.lang.String backendType;
    public void setBackendType(java.lang.String backendType) {
        this.backendType = backendType;
    }
    public java.lang.String getBackendType() {
        return this.backendType;
    }

    public java.lang.String deployType;
    public void setDeployType(java.lang.String deployType) {
        this.deployType = deployType;
    }
    public java.lang.String getDeployType() {
        return this.deployType;
    }

    public java.lang.String slbOfferingUuid;
    public void setSlbOfferingUuid(java.lang.String slbOfferingUuid) {
        this.slbOfferingUuid = slbOfferingUuid;
    }
    public java.lang.String getSlbOfferingUuid() {
        return this.slbOfferingUuid;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
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

    public java.util.List slbVms;
    public void setSlbVms(java.util.List slbVms) {
        this.slbVms = slbVms;
    }
    public java.util.List getSlbVms() {
        return this.slbVms;
    }

    public java.util.List lbs;
    public void setLbs(java.util.List lbs) {
        this.lbs = lbs;
    }
    public java.util.List getLbs() {
        return this.lbs;
    }

    public java.util.List networks;
    public void setNetworks(java.util.List networks) {
        this.networks = networks;
    }
    public java.util.List getNetworks() {
        return this.networks;
    }

    public java.util.List configTasks;
    public void setConfigTasks(java.util.List configTasks) {
        this.configTasks = configTasks;
    }
    public java.util.List getConfigTasks() {
        return this.configTasks;
    }

}
