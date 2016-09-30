package org.zstack.kvm;

/**
 */
public class KVMHostConnectedContext {
    private KVMHostInventory inventory;
    private boolean newAddedHost;

    public boolean isNewAddedHost() {
        return newAddedHost;
    }

    public void setNewAddedHost(boolean newAddedHost) {
        this.newAddedHost = newAddedHost;
    }

    public KVMHostInventory getInventory() {
        return inventory;
    }

    public void setInventory(KVMHostInventory inventory) {
        this.inventory = inventory;
    }
}
