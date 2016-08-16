package org.zstack.core.keystore;

import org.zstack.header.core.keystore.KeystoreInventory;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

/**
 * Created by miao on 16-8-15.
 */
public class APIQueryKeystoreReply extends APIQueryReply {
    private List<KeystoreInventory> inventories;

    public List<KeystoreInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<KeystoreInventory> inventories) {
        this.inventories = inventories;
    }
}
