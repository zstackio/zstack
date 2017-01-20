package org.zstack.core.scheduler;

import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

import java.util.List;
import static java.util.Arrays.asList;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 7/18/16.
 */
@Action(category = SchedulerConstant.ACTION_CATEGORY, names = {"read"})
@AutoQuery(replyClass = APIQuerySchedulerReply.class, inventoryClass = SchedulerInventory.class)
public class APIQuerySchedulerMsg extends APIQueryMessage {
 
    public static List<String> __example__() {
        return asList("schedulerJob=StopVmInstanceJob");
    }

}
