package org.zstack.core.gc;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/3/5.
 */
@AutoQuery(replyClass = APIQueryGCJobReply.class, inventoryClass = GarbageCollectorInventory.class)
@RestRequest(
        path = "/gc-jobs",
        optionalPaths = {"/gc-jobs/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryGCJobReply.class
)
public class APIQueryGCJobMsg extends APIQueryMessage {
}
