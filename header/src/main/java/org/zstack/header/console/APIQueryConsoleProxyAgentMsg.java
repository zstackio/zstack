package org.zstack.header.console;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 * Created by xing5 on 2016/3/15.
 */
@AutoQuery(replyClass = APIQueryConsoleProxyAgentReply.class, inventoryClass = ConsoleProxyAgentInventory.class)
public class APIQueryConsoleProxyAgentMsg extends APIQueryMessage {
}
