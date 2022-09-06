package org.zstack.header.volume;

public class VolumeInfo {
    private String installPath;
    private Long actualSize;

    public VolumeInfo(String installPath, Long actualSize) {
        this.installPath = installPath;
        this.actualSize = actualSize;
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
}
