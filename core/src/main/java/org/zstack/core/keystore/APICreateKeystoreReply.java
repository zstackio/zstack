package org.zstack.core.keystore;

import org.zstack.header.core.keystore.KeystoreInventory;
import org.zstack.header.message.APIReply;

/**
 * Created by miao on 16-8-15.
 */
public class APICreateKeystoreReply extends APIReply {
    private KeystoreInventory inventory;

    public KeystoreInventory getInventory() {
        return inventory;
    }

    public void setInventory(KeystoreInventory inventory) {
        this.inventory = inventory;
    }
}
