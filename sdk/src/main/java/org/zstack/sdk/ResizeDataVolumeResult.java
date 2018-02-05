package org.zstack.sdk;

<<<<<<< HEAD:sdk/src/main/java/org/zstack/sdk/PrimaryStorageMigrateRootVolumeResult.java
import org.zstack.sdk.VolumeInventory;

public class PrimaryStorageMigrateRootVolumeResult {
=======
public class ResizeDataVolumeResult {
>>>>>>> upstream/master:sdk/src/main/java/org/zstack/sdk/ResizeDataVolumeResult.java
    public VolumeInventory inventory;
    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
    public VolumeInventory getInventory() {
        return this.inventory;
    }

}
