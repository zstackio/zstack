package org.zstack.sdk;

import org.zstack.sdk.SecretResourcePoolInventory;

public class UpdateSecretResourcePoolResult {
    public SecretResourcePoolInventory inventory;
    public void setInventory(SecretResourcePoolInventory inventory) {
        this.inventory = inventory;
    }
    public SecretResourcePoolInventory getInventory() {
        return this.inventory;
    }

}
