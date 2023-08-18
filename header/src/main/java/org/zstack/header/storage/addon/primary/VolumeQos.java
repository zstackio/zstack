package org.zstack.header.storage.addon.primary;

public class VolumeQos {
    private long iops;
    private long bps;

    public static VolumeQos valueOf(String volumeQos) {
        // TODO
        return null;
    }

    public long getIops() {
        return iops;
    }

    public void setIops(long iops) {
        this.iops = iops;
    }

    public long getBps() {
        return bps;
    }

    public void setBps(long bps) {
        this.bps = bps;
    }
}
