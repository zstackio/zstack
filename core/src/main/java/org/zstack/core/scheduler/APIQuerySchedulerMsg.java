package org.zstack.core.scheduler;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 * Created by root on 7/18/16.
 */
@AutoQuery(replyClass = APIQuerySchedulerReply.class, inventoryClass = SchedulerInventory.class)
public class APIQuerySchedulerMsg extends APIQueryMessage {
}
