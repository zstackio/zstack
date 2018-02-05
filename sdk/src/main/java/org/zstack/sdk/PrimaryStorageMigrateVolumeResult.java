package org.zstack.sdk;

<<<<<<< HEAD:sdk/src/main/java/org/zstack/sdk/PrimaryStorageMigrateDataVolumeResult.java
import org.zstack.sdk.VolumeInventory;

public class PrimaryStorageMigrateDataVolumeResult {
=======
public class PrimaryStorageMigrateVolumeResult {
>>>>>>> upstream/master:sdk/src/main/java/org/zstack/sdk/PrimaryStorageMigrateVolumeResult.java
    public VolumeInventory inventory;
    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
    public VolumeInventory getInventory() {
        return this.inventory;
    }

}
