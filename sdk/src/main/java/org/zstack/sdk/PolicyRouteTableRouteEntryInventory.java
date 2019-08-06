package org.zstack.sdk;



public class PolicyRouteTableRouteEntryInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String tableUuid;
    public void setTableUuid(java.lang.String tableUuid) {
        this.tableUuid = tableUuid;
    }
    public java.lang.String getTableUuid() {
        return this.tableUuid;
    }

    public java.lang.String destinationCidr;
    public void setDestinationCidr(java.lang.String destinationCidr) {
        this.destinationCidr = destinationCidr;
    }
    public java.lang.String getDestinationCidr() {
        return this.destinationCidr;
    }

    public java.lang.String nextHopIp;
    public void setNextHopIp(java.lang.String nextHopIp) {
        this.nextHopIp = nextHopIp;
    }
    public java.lang.String getNextHopIp() {
        return this.nextHopIp;
    }

    public int distance;
    public void setDistance(int distance) {
        this.distance = distance;
    }
    public int getDistance() {
        return this.distance;
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
