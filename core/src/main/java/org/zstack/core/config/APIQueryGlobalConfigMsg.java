package org.zstack.core.config;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQueryGlobalConfigReply.class, inventoryClass = GlobalConfigInventory.class)
public class APIQueryGlobalConfigMsg extends APIQueryMessage {
}
