package org.zstack.sdk;



public class GetVolumeQosResult {
    public java.lang.String volumeUuid;
    public void setVolumeUuid(java.lang.String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
    public java.lang.String getVolumeUuid() {
        return this.volumeUuid;
    }

    public long volumeBandwidth;
    public void setVolumeBandwidth(long volumeBandwidth) {
        this.volumeBandwidth = volumeBandwidth;
    }
    public long getVolumeBandwidth() {
        return this.volumeBandwidth;
    }

    public long volumeBandwidthRead;
    public void setVolumeBandwidthRead(long volumeBandwidthRead) {
        this.volumeBandwidthRead = volumeBandwidthRead;
    }
    public long getVolumeBandwidthRead() {
        return this.volumeBandwidthRead;
    }

    public long volumeBandwidthWrite;
    public void setVolumeBandwidthWrite(long volumeBandwidthWrite) {
        this.volumeBandwidthWrite = volumeBandwidthWrite;
    }
    public long getVolumeBandwidthWrite() {
        return this.volumeBandwidthWrite;
    }

}
