package org.zstack.header.volume;

public class VolumeStats {
    protected String installPath;
    protected Long actualSize;
    protected String format;
    protected long size;

    public VolumeStats(String installPath, Long actualSize) {
        this.installPath = installPath;
        this.actualSize = actualSize;
    }

    public VolumeStats() {
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public Long getActualSize() {
        return actualSize;
    }

    public void setActualSize(Long actualSize) {
        this.actualSize = actualSize;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }
}
