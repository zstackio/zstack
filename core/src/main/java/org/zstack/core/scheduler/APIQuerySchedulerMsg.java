package org.zstack.core.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;
import static java.util.Arrays.asList;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 7/18/16.
 */
@Action(category = SchedulerConstant.ACTION_CATEGORY, names = {"read"})
@AutoQuery(replyClass = APIQuerySchedulerReply.class, inventoryClass = SchedulerInventory.class)
@RestRequest(
        path = "/schedulers",
        optionalPaths = {"/schedulers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySchedulerReply.class
)
public class APIQuerySchedulerMsg extends APIQueryMessage {
 
    public static List<String> __example__() {
        return asList("schedulerJob=StopVmInstanceJob");
    }

}
