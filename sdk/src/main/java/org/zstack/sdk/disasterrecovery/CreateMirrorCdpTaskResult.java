package org.zstack.sdk.disasterrecovery;

import org.zstack.sdk.disasterrecovery.MirrorCdpTaskInventory;

public class CreateMirrorCdpTaskResult {
    public MirrorCdpTaskInventory inventory;
    public void setInventory(MirrorCdpTaskInventory inventory) {
        this.inventory = inventory;
    }
    public MirrorCdpTaskInventory getInventory() {
        return this.inventory;
    }

}
