package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQueryPrimaryStorageReply.class, inventoryClass = PrimaryStorageInventory.class)
@RestRequest(
        path = "/primary-storage",
        method = HttpMethod.GET,
        responseClass = APIQueryPrimaryStorageReply.class
)
public class APIQueryPrimaryStorageMsg extends APIQueryMessage {
}
