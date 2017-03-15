package org.zstack.core.notification;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/3/18.
 */
@RestRequest(
        path = "/notifications",
        optionalPaths = {"/notifications/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryNotificationReply.class
)
@AutoQuery(replyClass = APIQueryNotificationReply.class, inventoryClass = NotificationInventory.class)
public class APIQueryNotificationMsg extends APIQueryMessage {
}
