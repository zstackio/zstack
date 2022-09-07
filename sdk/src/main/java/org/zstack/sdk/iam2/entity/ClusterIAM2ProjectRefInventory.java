package org.zstack.sdk.iam2.entity;



public class ClusterIAM2ProjectRefInventory  {

    public java.lang.String projectUuid;
    public void setProjectUuid(java.lang.String projectUuid) {
        this.projectUuid = projectUuid;
    }
    public java.lang.String getProjectUuid() {
        return this.projectUuid;
    }

    public java.lang.String clusterUuid;
    public void setClusterUuid(java.lang.String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }
    public java.lang.String getClusterUuid() {
        return this.clusterUuid;
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
