package org.zstack.core.scheduler;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 7/18/16.
 */
@Action(category = SchedulerConstant.ACTION_CATEGORY, names = {"read"})
@AutoQuery(replyClass = APIQuerySchedulerReply.class, inventoryClass = SchedulerInventory.class)
public class APIQuerySchedulerMsg extends APIQueryMessage {
 
    public static APIQuerySchedulerMsg __example__() {
        APIQuerySchedulerMsg msg = new APIQuerySchedulerMsg();


        return msg;
    }

}
