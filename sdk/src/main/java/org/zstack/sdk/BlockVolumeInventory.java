package org.zstack.sdk;

import org.zstack.sdk.BlockVolumeXskyRefInventory;

public class BlockVolumeInventory extends org.zstack.sdk.VolumeInventory {

    public java.lang.String iscsiPath;
    public void setIscsiPath(java.lang.String iscsiPath) {
        this.iscsiPath = iscsiPath;
    }
    public java.lang.String getIscsiPath() {
        return this.iscsiPath;
    }

    public BlockVolumeXskyRefInventory refInventory;
    public void setRefInventory(BlockVolumeXskyRefInventory refInventory) {
        this.refInventory = refInventory;
    }
    public BlockVolumeXskyRefInventory getRefInventory() {
        return this.refInventory;
    }

}
