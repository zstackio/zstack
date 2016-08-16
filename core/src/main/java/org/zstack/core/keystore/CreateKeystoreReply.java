package org.zstack.core.keystore;

import org.zstack.header.core.keystore.KeystoreInventory;
import org.zstack.header.message.MessageReply;

/**
 * Created by miao on 16-8-15.
 */
public class CreateKeystoreReply extends MessageReply {

    private KeystoreInventory inventory;

    public KeystoreInventory getInventory() {
        return inventory;
    }

    public void setInventory(KeystoreInventory inventory) {
        this.inventory = inventory;
    }
}
