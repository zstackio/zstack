package org.zstack.sdk;



public class ElaborationInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String errorInfo;
    public void setErrorInfo(java.lang.String errorInfo) {
        this.errorInfo = errorInfo;
    }
    public java.lang.String getErrorInfo() {
        return this.errorInfo;
    }

    public java.lang.String md5sum;
    public void setMd5sum(java.lang.String md5sum) {
        this.md5sum = md5sum;
    }
    public java.lang.String getMd5sum() {
        return this.md5sum;
    }

    public double distance;
    public void setDistance(double distance) {
        this.distance = distance;
    }
    public double getDistance() {
        return this.distance;
    }

    public boolean matched;
    public void setMatched(boolean matched) {
        this.matched = matched;
    }
    public boolean getMatched() {
        return this.matched;
    }

    public long repeats;
    public void setRepeats(long repeats) {
        this.repeats = repeats;
    }
    public long getRepeats() {
        return this.repeats;
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
