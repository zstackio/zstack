package org.zstack.expon.sdk.volume;

import org.zstack.expon.sdk.Param;
import org.zstack.header.volume.VolumeQos;

public class ExponVolumeQos {
    @Param(numberRange = {10485760, 107374182400L})
    private Long bpsLimit;
    @Param(numberRange = {10485760, 107374182400L})
    private Long readBpsLimit;
    @Param(numberRange = {10485760, 107374182400L})
    private Long writeBpsLimit;
    @Param(numberRange = {1000, 10000000})
    private Long iopsLimit;
    @Param(numberRange = {1000, 10000000})
    private Long readIopsLimit;
    @Param(numberRange = {1000, 10000000})
    private Long writeIopsLimit;

    public static ExponVolumeQos valueOf(VolumeQos qos) {
        if (qos == null) {
            return null;
        }

        ExponVolumeQos ret = new ExponVolumeQos();
        ret.bpsLimit = qos.getTotalBandwidth();
        ret.iopsLimit = qos.getTotalIOPS();
        ret.readBpsLimit = qos.getReadBandwidth();
        ret.readIopsLimit = qos.getReadIOPS();
        ret.writeBpsLimit = qos.getWriteBandwidth();
        ret.writeIopsLimit = qos.getWriteIOPS();
        return ret;
    }

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

    public void setBpsLimit(Long bpsLimit) {
        this.bpsLimit = bpsLimit;
    }

    public Long getReadBpsLimit() {
        return readBpsLimit;
    }

    public void setReadBpsLimit(Long readBpsLimit) {
        this.readBpsLimit = readBpsLimit;
    }

    public Long getWriteBpsLimit() {
        return writeBpsLimit;
    }

    public void setWriteBpsLimit(Long writeBpsLimit) {
        this.writeBpsLimit = writeBpsLimit;
    }

    public void setIopsLimit(Long iopsLimit) {
        this.iopsLimit = iopsLimit;
    }

    public Long getReadIopsLimit() {
        return readIopsLimit;
    }

    public void setReadIopsLimit(Long readIopsLimit) {
        this.readIopsLimit = readIopsLimit;
    }

    public Long getWriteIopsLimit() {
        return writeIopsLimit;
    }

    public void setWriteIopsLimit(Long writeIopsLimit) {
        this.writeIopsLimit = writeIopsLimit;
    }
}
