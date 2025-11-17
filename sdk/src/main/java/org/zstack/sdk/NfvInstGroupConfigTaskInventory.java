package org.zstack.sdk;



public class NfvInstGroupConfigTaskInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String nfvInstGroupUuid;
    public void setNfvInstGroupUuid(java.lang.String nfvInstGroupUuid) {
        this.nfvInstGroupUuid = nfvInstGroupUuid;
    }
    public java.lang.String getNfvInstGroupUuid() {
        return this.nfvInstGroupUuid;
    }

    public int configVersion;
    public void setConfigVersion(int configVersion) {
        this.configVersion = configVersion;
    }
    public int getConfigVersion() {
        return this.configVersion;
    }

    public java.lang.String serviceUuid;
    public void setServiceUuid(java.lang.String serviceUuid) {
        this.serviceUuid = serviceUuid;
    }
    public java.lang.String getServiceUuid() {
        return this.serviceUuid;
    }

    public java.lang.String taskName;
    public void setTaskName(java.lang.String taskName) {
        this.taskName = taskName;
    }
    public java.lang.String getTaskName() {
        return this.taskName;
    }

    public java.lang.String taskData;
    public void setTaskData(java.lang.String taskData) {
        this.taskData = taskData;
    }
    public java.lang.String getTaskData() {
        return this.taskData;
    }

    public java.lang.String path;
    public void setPath(java.lang.String path) {
        this.path = path;
    }
    public java.lang.String getPath() {
        return this.path;
    }

    public boolean checkStatus;
    public void setCheckStatus(boolean checkStatus) {
        this.checkStatus = checkStatus;
    }
    public boolean getCheckStatus() {
        return this.checkStatus;
    }

}
