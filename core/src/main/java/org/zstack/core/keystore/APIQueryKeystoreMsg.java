package org.zstack.core.keystore;

import org.zstack.header.core.keystore.KeystoreInventory;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 * Created by miao on 16-8-15.
 */
@AutoQuery(replyClass = APIQueryKeystoreReply.class, inventoryClass = KeystoreInventory.class)
public class APIQueryKeystoreMsg extends APIQueryMessage {
}
