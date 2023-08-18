package org.zstack.expon.sdk.volume;

import org.zstack.expon.sdk.Param;

public class VolumeQos {
    @Param
    private Long bpsLimit;
    @Param
    private Long iopsLimit;

    public Long getBpsLimit() {
        return bpsLimit;
    }

    public void setBpsLimit(long bpsLimit) {
        this.bpsLimit = bpsLimit;
    }

    public Long getIopsLimit() {
        return iopsLimit;
    }

    public void setIopsLimit(long iopsLimit) {
        this.iopsLimit = iopsLimit;
    }

    public static VolumeQos valueOf(org.zstack.header.storage.addon.primary.VolumeQos qos) {
        if (qos == null) {
            return null;
        }

        VolumeQos ret = new VolumeQos();
        ret.bpsLimit = qos.getBps();
        ret.iopsLimit = qos.getIops();
        return ret;
    }
}
