package org.zstack.core.keystore;

import org.zstack.header.core.keystore.KeystoreInventory;
import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Created by miao on 16-8-15.
 */
public class QueryKeystoreReply extends MessageReply {
    private KeystoreInventory inventory;

    public KeystoreInventory getInventory() {
        return inventory;
    }

    public void setInventory(KeystoreInventory inventory) {
        this.inventory = inventory;
    }
}
