package org.zstack.sdk.disasterrecovery;

import org.zstack.sdk.VmInstanceInventory;

public class FailbackVmFromMirrorCdpBackupResult {
    public VmInstanceInventory inventory;
    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
    public VmInstanceInventory getInventory() {
        return this.inventory;
    }

}
