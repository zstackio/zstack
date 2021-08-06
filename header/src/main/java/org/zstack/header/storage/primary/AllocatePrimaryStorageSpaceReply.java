package org.zstack.header.storage.primary;

public class AllocatePrimaryStorageSpaceReply extends AllocatePrimaryStorageReply{
    private String installDir;

    public AllocatePrimaryStorageSpaceReply(PrimaryStorageInventory primaryStorageInventory, String installDir) {
        super(primaryStorageInventory);
        this.installDir = installDir;
    }

    public String getInstallDir() {
        return installDir;
    }

    public void setInstallDir(String installDir) {
        this.installDir = installDir;
    }
}
