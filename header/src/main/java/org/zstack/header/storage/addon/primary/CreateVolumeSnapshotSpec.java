package org.zstack.header.storage.addon.primary;

public class CreateVolumeSnapshotSpec {
    private String name;
    private String uuid;
    private String volumeInstallPath;
    private String allocatedUrl;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAllocatedUrl() {
        return allocatedUrl;
    }

    public void setAllocatedUrl(String allocatedUrl) {
        this.allocatedUrl = allocatedUrl;
    }

    public String getVolumeInstallPath() {
        return volumeInstallPath;
    }

    public void setVolumeInstallPath(String volumeInstallPath) {
        this.volumeInstallPath = volumeInstallPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
