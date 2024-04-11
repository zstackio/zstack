package org.zstack.sdk;



public class VpcSharedQosInventory  {

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

    public java.lang.String l3NetworkUuid;
    public void setL3NetworkUuid(java.lang.String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
    public java.lang.String getL3NetworkUuid() {
        return this.l3NetworkUuid;
    }

    public java.lang.String vpcUuid;
    public void setVpcUuid(java.lang.String vpcUuid) {
        this.vpcUuid = vpcUuid;
    }
    public java.lang.String getVpcUuid() {
        return this.vpcUuid;
    }

    public java.lang.Long bandwidth;
    public void setBandwidth(java.lang.Long bandwidth) {
        this.bandwidth = bandwidth;
    }
    public java.lang.Long getBandwidth() {
        return this.bandwidth;
    }

    public java.util.List vips;
    public void setVips(java.util.List vips) {
        this.vips = vips;
    }
    public java.util.List getVips() {
        return this.vips;
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
