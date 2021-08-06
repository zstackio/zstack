package org.zstack.header.storage.primary;

public class AllocatePrimaryStorageSpaceMsg extends AllocatePrimaryStorageMsg {
    private String InstallDir;

    public String getInstallDir() {
        return InstallDir;
    }

    public void setInstallDir(String installDir) {
        InstallDir = installDir;
    }
}
