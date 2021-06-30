package org.zstack.sdk;



public class RecoveryPointInfo  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public long size;
    public void setSize(long size) {
        this.size = size;
    }
    public long getSize() {
        return this.size;
    }

    public java.lang.String volumeUuid;
    public void setVolumeUuid(java.lang.String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
    public java.lang.String getVolumeUuid() {
        return this.volumeUuid;
    }

    public java.lang.String timestamp;
    public void setTimestamp(java.lang.String timestamp) {
        this.timestamp = timestamp;
    }
    public java.lang.String getTimestamp() {
        return this.timestamp;
    }

}
